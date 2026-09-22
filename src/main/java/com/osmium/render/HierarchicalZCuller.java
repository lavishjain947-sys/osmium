package com.osmium.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class HierarchicalZCuller {
    public static final int MIRROR_W = 64;
    public static final int MIRROR_H = 64;
    private static final float[] depthMirror = new float[MIRROR_W * MIRROR_H];
    private static boolean mirrorValid = false;

    private static int mirrorFbo = -1;
    private static int mirrorDepthTex = -1;
    private static final float DEPTH_BIAS = 0.01f;

    private HierarchicalZCuller() {}

    public static void captureDepthMirror() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Framebuffer mainFbo = client.getFramebuffer();
        if (mainFbo == null) return;

        try {
            ensureMirrorFbo();

            // Blit depth from main FBO to 64x64 mirror FBO
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mirrorFbo);

            GL30.glBlitFramebuffer(
                0, 0, mainFbo.textureWidth, mainFbo.textureHeight,
                0, 0, MIRROR_W, MIRROR_H,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
            );

            // Read back 64x64 depth buffer
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mirrorFbo);
            GL11.glReadPixels(0, 0, MIRROR_W, MIRROR_H, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, depthMirror);

            // Restore main FBO binding
            mainFbo.beginWrite(false);
            mirrorValid = true;
        } catch (Throwable t) {
            mirrorValid = false;
        }
    }

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

    public static boolean isOccluded(Box box, Matrix4f viewProj, int screenW, int screenH) {
        if (!mirrorValid || box == null || viewProj == null || screenW <= 0 || screenH <= 0) {
            return false;
        }

        double[][] corners = {
            {box.minX, box.minY, box.minZ},
            {box.maxX, box.minY, box.minZ},
            {box.minX, box.maxY, box.minZ},
            {box.maxX, box.maxY, box.minZ},
            {box.minX, box.minY, box.maxZ},
            {box.maxX, box.minY, box.maxZ},
            {box.minX, box.maxY, box.maxZ},
            {box.maxX, box.maxY, box.maxZ}
        };

        float minNdcX = Float.MAX_VALUE, maxNdcX = -Float.MAX_VALUE;
        float minNdcY = Float.MAX_VALUE, maxNdcY = -Float.MAX_VALUE;
        float minDepth = Float.MAX_VALUE;

        Vector4f v = new Vector4f();
        for (double[] c : corners) {
            v.set((float) c[0], (float) c[1], (float) c[2], 1.0f);
            viewProj.transform(v);

            // If behind near plane, do not occlude
            if (v.w <= 0.001f) {
                return false;
            }

            float ndcX = v.x / v.w;
            float ndcY = v.y / v.w;
            float depth01 = (v.z / v.w) * 0.5f + 0.5f;

            minNdcX = Math.min(minNdcX, ndcX);
            maxNdcX = Math.max(maxNdcX, ndcX);
            minNdcY = Math.min(minNdcY, ndcY);
            maxNdcY = Math.max(maxNdcY, ndcY);
            minDepth = Math.min(minDepth, depth01);
        }

        // Frustum check
        if (maxNdcX < -1.0f || minNdcX > 1.0f || maxNdcY < -1.0f || minNdcY > 1.0f) {
            return true;
        }

        int mX0 = (int) Math.max(0, Math.min(MIRROR_W - 1, ((minNdcX * 0.5f + 0.5f) * MIRROR_W)));
        int mX1 = (int) Math.max(0, Math.min(MIRROR_W - 1, ((maxNdcX * 0.5f + 0.5f) * MIRROR_W)));
        int mY0 = (int) Math.max(0, Math.min(MIRROR_H - 1, ((minNdcY * 0.5f + 0.5f) * MIRROR_H)));
        int mY1 = (int) Math.max(0, Math.min(MIRROR_H - 1, ((maxNdcY * 0.5f + 0.5f) * MIRROR_H)));

        for (int y = mY0; y <= mY1; y++) {
            for (int x = mX0; x <= mX1; x++) {
                float mirrorDepthVal = depthMirror[y * MIRROR_W + x];
                if (minDepth <= mirrorDepthVal + DEPTH_BIAS) {
                    return false; // At least one pixel is visible
                }
            }
        }

        return true;
    }

    public static void setDepthMirrorData(float[] data) {
        if (data != null && data.length == depthMirror.length) {
            System.arraycopy(data, 0, depthMirror, 0, data.length);
            mirrorValid = true;
        }
    }

    public static void invalidate() {
        mirrorValid = false;
    }
}
