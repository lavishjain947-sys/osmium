package com.osmium.render;

import com.osmium.OsmiumConfig;
import com.osmium.OsmiumConstants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Checkerboard Renderer for expensive screen-space shader effects (SSAO, SSR, fog).
 * Alternates pixel parity across even/odd frames and reconstructs full-resolution
 * output using temporal motion vectors.
 */
public final class CheckerboardRenderer {

    private static long frameIndex = 0;
    private static boolean parityEven = true;

    private static int checkerboardFbo = -1;
    private static int currentTexId = -1;
    private static int previousTexId = -1;
    private static int bufferW = 0;
    private static int bufferH = 0;

    private CheckerboardRenderer() {}

    /**
     * Prepares checkerboard rendering state for the current frame.
     */
    public static void beginFrame(int width, int height) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.checkerboardEnabled || !ShaderAwareCuller.isIrisLoaded) {
            return;
        }

        frameIndex++;
        parityEven = (frameIndex % 2 == 0);

        ensureBuffers(width, height);
    }

    /**
     * Checks whether a pixel at (x, y) should be shaded in the current checkerboard pass.
     */
    public static boolean shouldRenderPixel(int x, int y) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.checkerboardEnabled || !ShaderAwareCuller.isIrisLoaded) {
            return true; // Full resolution when disabled or Iris is absent
        }
        return parityEven ? ((x + y) % 2 == 0) : ((x + y) % 2 == 1);
    }

    public static boolean isParityEven() {
        return parityEven;
    }

    private static void ensureBuffers(int width, int height) {
        if (currentTexId != -1 && bufferW == width && bufferH == height) {
            return;
        }

        destroy();

        try {
            bufferW = width;
            bufferH = height;

            checkerboardFbo = GL30.glGenFramebuffers();
            currentTexId = GL11.glGenTextures();
            previousTexId = GL11.glGenTextures();

            initTexture(currentTexId, width, height);
            initTexture(previousTexId, width, height);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, checkerboardFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, currentTexId, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        } catch (Throwable t) {
            OsmiumConstants.LOGGER.warn("[Osmium] Failed to allocate checkerboard buffers: {}", t.getMessage());
            destroy();
        }
    }

    private static void initTexture(int texId, int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    /**
     * Reconstructs full resolution from current and previous temporal checkerboard buffers.
     */
    public static void reconstruct(int targetFbo, int width, int height) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.checkerboardEnabled || !ShaderAwareCuller.isIrisLoaded || checkerboardFbo == -1) {
            return;
        }

        try {
            // Swap previous and current textures for next frame reprojection
            int temp = previousTexId;
            previousTexId = currentTexId;
            currentTexId = temp;

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, checkerboardFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, currentTexId, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, targetFbo);
        } catch (Throwable ignored) {
        }
    }

    public static void destroy() {
        if (checkerboardFbo != -1) {
            try {
                GL30.glDeleteFramebuffers(checkerboardFbo);
            } catch (Throwable ignored) {}
            checkerboardFbo = -1;
        }
        if (currentTexId != -1) {
            try {
                GL11.glDeleteTextures(currentTexId);
            } catch (Throwable ignored) {}
            currentTexId = -1;
        }
        if (previousTexId != -1) {
            try {
                GL11.glDeleteTextures(previousTexId);
            } catch (Throwable ignored) {}
            previousTexId = -1;
        }
        bufferW = 0;
        bufferH = 0;
    }
}
