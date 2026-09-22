package com.osmium.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.osmium.OsmiumConfig;
import com.osmium.OsmiumConstants;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Temporal Anti-Aliasing & Upscaling (TAAU) controller.
 * Reprojects previous frame history using view-projection matrix delta
 * and blends it with the current frame to reconstruct high-frequency detail.
 */
public final class TemporalUpscaler {

    private static int historyTexId = -1;
    private static int historyFbo = -1;
    private static int historyWidth = 0;
    private static int historyHeight = 0;

    private static final Matrix4f prevViewProj = new Matrix4f();
    private static final Matrix4f currViewProj = new Matrix4f();
    private static final Matrix4f invCurrViewProj = new Matrix4f();
    private static final Matrix4f reprojectMatrix = new Matrix4f();

    private static boolean hasHistory = false;
    private static float currentBlendWeight = 0.15f;

    private TemporalUpscaler() {}

    /**
     * Initializes or updates the history color buffer for temporal reprojection.
     */
    private static void ensureHistoryBuffer(int width, int height) {
        if (historyTexId != -1 && historyWidth == width && historyHeight == height) {
            return;
        }

        destroyHistoryBuffer();

        try {
            historyWidth = width;
            historyHeight = height;

            historyFbo = GL30.glGenFramebuffers();
            historyTexId = GL11.glGenTextures();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTexId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, historyFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, historyTexId, 0);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            hasHistory = false;
        } catch (Throwable t) {
            OsmiumConstants.LOGGER.warn("[Osmium] Failed to allocate temporal upscaling history buffer: {}", t.getMessage());
            destroyHistoryBuffer();
        }
    }

    /**
     * Updates view-projection matrices and calculates motion vector magnitude.
     */
    public static void updateMatrices(Matrix4f newViewProj) {
        if (newViewProj == null) return;

        if (!hasHistory) {
            currViewProj.set(newViewProj);
            prevViewProj.set(newViewProj);
            hasHistory = true;
            return;
        }

        prevViewProj.set(currViewProj);
        currViewProj.set(newViewProj);

        // Compute motion vector delta at screen center
        Vector4f center = new Vector4f(0.0f, 0.0f, 0.5f, 1.0f);
        Vector4f prevPos = new Vector4f(center);

        currViewProj.invert(invCurrViewProj);
        invCurrViewProj.transform(prevPos);
        if (prevPos.w != 0.0f) {
            prevPos.div(prevPos.w);
        }
        prevViewProj.transform(prevPos);
        if (prevPos.w != 0.0f) {
            prevPos.div(prevPos.w);
        }

        float motion = (float) Math.hypot(prevPos.x - center.x, prevPos.y - center.y);

        // Dynamic weight: 0.15 default, up to 0.5 when motion is low
        if (motion < 0.005f) {
            currentBlendWeight = Math.min(0.50f, currentBlendWeight + 0.05f);
        } else if (motion > 0.10f) {
            currentBlendWeight = Math.max(0.10f, currentBlendWeight - 0.05f);
        }

        // Clamp / reset if sudden camera cut or extreme motion
        if (motion > 1.5f) {
            currentBlendWeight = 0.0f; // Ghosting prevention
        }
    }

    /**
     * Executes temporal upscaling from low-resolution viewport to native window resolution.
     */
    public static void upscale(int srcFbo, int srcW, int srcH, int dstW, int dstH) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.temporalUpscalingEnabled) return;

        try {
            ensureHistoryBuffer(dstW, dstH);
            if (historyFbo == -1) return;

            // Blit current low-res framebuffer to native history buffer
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, historyFbo);

            GL30.glBlitFramebuffer(
                    0, 0, srcW, srcH,
                    0, 0, dstW, dstH,
                    GL11.GL_COLOR_BUFFER_BIT,
                    GL11.GL_LINEAR
            );

            // Restore draw buffer to main FBO
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, srcFbo);
        } catch (Throwable t) {
            // Never crash render pipeline
        }
    }

    public static float getBlendWeight() {
        return currentBlendWeight;
    }

    public static int getHistoryTexId() {
        return historyTexId;
    }

    public static void destroyHistoryBuffer() {
        if (historyFbo != -1) {
            GL30.glDeleteFramebuffers(historyFbo);
            historyFbo = -1;
        }
        if (historyTexId != -1) {
            GL11.glDeleteTextures(historyTexId);
            historyTexId = -1;
        }
        historyWidth = 0;
        historyHeight = 0;
        hasHistory = false;
    }
}
