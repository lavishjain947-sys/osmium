package com.osmium.render;

import com.osmium.OsmiumConfig;
import com.osmium.OsmiumConstants;
import com.osmium.core.FrameBudgetController;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

/**
 * Variable Rate Shading (VRS) controller.
 * Implements foveated shading rate images (center 1x1, mid-ring 1x2, periphery 2x2/4x4)
 * to boost frame rates on high-DPI and shader-heavy workloads.
 */
public final class VariableRateShading {

    public static final int GL_SHADING_RATE_IMAGE_NV = 0x9563;
    public static final int GL_SHADING_RATE_IMAGE_PALETTE_COUNT_NV = 0x9564;
    public static final int GL_SHADING_RATE_1_INVOCATION_PER_PIXEL_NV = 0x00;
    public static final int GL_SHADING_RATE_1_INVOCATION_PER_1X2_PIXELS_NV = 0x01;
    public static final int GL_SHADING_RATE_1_INVOCATION_PER_2X2_PIXELS_NV = 0x03;
    public static final int GL_SHADING_RATE_1_INVOCATION_PER_4X4_PIXELS_NV = 0x06;

    private static boolean initialized = false;
    private static boolean supported = false;
    private static boolean activeThisFrame = false;

    private static int sriTexId = -1;
    private static int sriWidth = 0;
    private static int sriHeight = 0;

    private VariableRateShading() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            GLCapabilities caps = GL.getCapabilities();
            supported = caps.GL_NV_shading_rate_image;
            if (supported) {
                OsmiumConstants.LOGGER.info("[Osmium] GL_NV_shading_rate_image supported and enabled for VRS.");
            } else {
                OsmiumConstants.LOGGER.info("[Osmium] Variable Rate Shading (VRS) hardware extension not available on this GPU.");
            }
        } catch (Throwable t) {
            supported = false;
            OsmiumConstants.LOGGER.debug("[Osmium] VRS extension query failed: {}", t.getMessage());
        }
    }

    public static boolean isSupported() {
        if (!initialized) init();
        return supported;
    }

    public static void beginFrame(int screenW, int screenH) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.vrsEnabled || !isSupported()) {
            activeThisFrame = false;
            return;
        }

        try {
            double p95 = FrameBudgetController.getP95Ms();
            double targetBudget = 1000.0 / Math.max(10, cfg.targetFps);

            // On frames where GPU load is low, disable VRS to preserve pristine quality
            if (p95 < targetBudget * 0.7) {
                activeThisFrame = false;
                return;
            }

            boolean aggressive = p95 > targetBudget * 1.2;
            updateShadingRateImage(screenW, screenH, aggressive);

            GL11.glEnable(GL_SHADING_RATE_IMAGE_NV);
            activeThisFrame = true;
        } catch (Throwable t) {
            activeThisFrame = false;
        }
    }

    public static void endFrame() {
        if (!activeThisFrame) return;

        try {
            GL11.glDisable(GL_SHADING_RATE_IMAGE_NV);
        } catch (Throwable ignored) {
        } finally {
            activeThisFrame = false;
        }
    }

    private static void updateShadingRateImage(int screenW, int screenH, boolean aggressive) {
        int tileW = Math.max(1, (screenW + 15) / 16);
        int tileH = Math.max(1, (screenH + 15) / 16);

        if (sriTexId == -1 || sriWidth != tileW || sriHeight != tileH) {
            destroy();
            sriWidth = tileW;
            sriHeight = tileH;
            sriTexId = GL11.glGenTextures();
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(tileW * tileH);
        float centerX = tileW / 2.0f;
        float centerY = tileH / 2.0f;
        float maxDist = (float) Math.hypot(centerX, centerY);

        for (int y = 0; y < tileH; y++) {
            for (int x = 0; x < tileW; x++) {
                float dist = (float) Math.hypot(x - centerX, y - centerY) / maxDist;
                byte rate;
                if (dist < 0.35f) {
                    rate = (byte) GL_SHADING_RATE_1_INVOCATION_PER_PIXEL_NV; // 1x1 center
                } else if (dist < 0.70f) {
                    rate = (byte) GL_SHADING_RATE_1_INVOCATION_PER_1X2_PIXELS_NV; // 1x2 mid-ring
                } else {
                    rate = aggressive
                            ? (byte) GL_SHADING_RATE_1_INVOCATION_PER_4X4_PIXELS_NV // 4x4 periphery
                            : (byte) GL_SHADING_RATE_1_INVOCATION_PER_2X2_PIXELS_NV; // 2x2 periphery
                }
                buffer.put(rate);
            }
        }
        buffer.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sriTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_R8UI, tileW, tileH, 0,
                GL11.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public static void destroy() {
        if (sriTexId != -1) {
            try {
                GL11.glDeleteTextures(sriTexId);
            } catch (Throwable ignored) {}
            sriTexId = -1;
        }
        sriWidth = 0;
        sriHeight = 0;
        activeThisFrame = false;
    }
}
