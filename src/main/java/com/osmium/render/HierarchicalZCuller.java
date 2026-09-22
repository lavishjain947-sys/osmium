package com.osmium.render;

import com.osmium.OsmiumConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class HierarchicalZCuller {

    public static final int MIRROR_W = 64;
    public static final int MIRROR_H = 64;
    private static final float[] DEPTH_MIRROR = new float[MIRROR_W * MIRROR_H];
    private static boolean mirrorValid = false;

    private static int mirrorFbo = -1;
    private static int mirrorDepthTex = -1;
    private static int pbo = -1;
    private static long fence = 0L;

    private HierarchicalZCuller() {}

    private static void ensureMirrorFbo() {
        if (mirrorFbo != -1) return;

        mirrorFbo = GL30.glGenFramebuffers();
        mirrorDepthTex = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mirrorDepthTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, MIRROR_W, MIRROR_H, 0,
                GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mirrorFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, mirrorDepthTex, 0);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
    }

    /** Called from WorldRendererMixin after the opaque terrain pass. */
    public static void captureDepthMirror() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        Window window = mc.getWindow();
        Framebuffer mainFbo = mc.getFramebuffer();
        if (window == null || mainFbo == null) return;

        try {
            ensureMirrorFbo();

            // Blit the main depth buffer down to the 64x64 depth FBO
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mirrorFbo);

            GL30.glBlitFramebuffer(
                    0, 0, mainFbo.textureWidth, mainFbo.textureHeight,
                    0, 0, MIRROR_W, MIRROR_H,
                    GL11.GL_DEPTH_BUFFER_BIT,
                    GL11.GL_NEAREST
            );

            // Bind mirror FBO for readback
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mirrorFbo);

            if (pbo == -1) {
                pbo = GL15.glGenBuffers();
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
                GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER,
                        (long) MIRROR_W * MIRROR_H * Float.BYTES, GL15.GL_STREAM_READ);
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            }

            // Submit async readback into the PBO (non-blocking).
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
            GL11.glReadPixels(0, 0, MIRROR_W, MIRROR_H,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);

            // Check the previous frame's fence — non-blocking.
            if (fence != 0L) {
                int status = GL32.glClientWaitSync(fence,
                        GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 0L);
                if (status == GL32.GL_ALREADY_SIGNALED
                        || status == GL32.GL_CONDITION_SATISFIED) {
                    GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
                    ByteBuffer mapped = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER,
                            GL15.GL_READ_ONLY);
                    if (mapped != null) {
                        FloatBuffer fb = mapped.asFloatBuffer();
                        fb.get(DEPTH_MIRROR);
                        GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
                        mirrorValid = true;
                    }
                    GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
                    GL32.glDeleteSync(fence);
                    fence = 0L;
                }
            }
            fence = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0L);

            // Restore main FBO
            mainFbo.beginWrite(false);
        } catch (Throwable t) {
            mirrorValid = false;
        }
    }

    public static boolean isOccluded(Box box, Matrix4f viewProj,
            int screenW, int screenH) {
        if (!mirrorValid || box == null) return false;

        // Project all 8 corners to screen space
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minDepth = Float.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? (float) box.minX : (float) box.maxX;
            float y = (i & 2) == 0 ? (float) box.minY : (float) box.maxY;
            float z = (i & 4) == 0 ? (float) box.minZ : (float) box.maxZ;

            Vector4f v = new Vector4f(x, y, z, 1.0f);
            v.mul(viewProj);
            if (v.w == 0.0f) return false;
            float ndcX = v.x / v.w;
            float ndcY = v.y / v.w;
            float ndcZ = v.z / v.w;
            if (ndcX < -1f || ndcX > 1f || ndcY < -1f || ndcY > 1f) {
                return false; // partially outside frustum -> don't cull
            }
            float sx = (ndcX * 0.5f + 0.5f) * screenW;
            float sy = (ndcY * 0.5f + 0.5f) * screenH;
            minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
            minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
            minDepth = Math.min(minDepth, (ndcZ * 0.5f + 0.5f));
        }

        int x0 = (int) (minX / screenW * MIRROR_W);
        int x1 = (int) (maxX / screenW * MIRROR_W) + 1;
        int y0 = (int) (minY / screenH * MIRROR_H);
        int y1 = (int) (maxY / screenH * MIRROR_H) + 1;
        x0 = Math.max(0, Math.min(MIRROR_W - 1, x0));
        x1 = Math.max(0, Math.min(MIRROR_W, x1));
        y0 = Math.max(0, Math.min(MIRROR_H - 1, y0));
        y1 = Math.max(0, Math.min(MIRROR_H, y1));
        if (x0 >= x1 || y0 >= y1) return false;

        final float bias = 0.001f;
        for (int yy = y0; yy < y1; yy++) {
            for (int xx = x0; xx < x1; xx++) {
                float mirrorDepth = DEPTH_MIRROR[yy * MIRROR_W + xx];
                if (mirrorDepth <= 0.0f || mirrorDepth >= 1.0f) return false;
                if (minDepth < mirrorDepth + bias) {
                    return false; // not occluded at this pixel
                }
            }
        }
        return true; // occluded at every pixel
    }

    public static void invalidate() {
        mirrorValid = false;
    }
}
