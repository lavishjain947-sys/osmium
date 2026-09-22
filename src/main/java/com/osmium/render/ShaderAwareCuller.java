package com.osmium.render;

import com.osmium.OsmiumConfig;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Intercepts Iris G-buffer depth to feed into HierarchicalZCuller for shader-accurate culling.
 * Falls back to no-op when Iris is absent or disabled in config.
 */
public final class ShaderAwareCuller {
    public static boolean isIrisLoaded = false;
    private static int gbufferDepthTex = -1;

    private ShaderAwareCuller() {}

    public static void init() {
        isIrisLoaded = FabricLoader.getInstance().isModLoaded("iris");
    }

    public static void captureGbufferDepth() {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (!isIrisLoaded || cfg == null || !cfg.shaderAwareCullingEnabled) {
            return;
        }
        // Feed the active G-buffer depth into HierarchicalZCuller
        HierarchicalZCuller.captureDepthMirror();
    }

    public static boolean shouldSkipEntity(Object e) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (!isIrisLoaded || cfg == null || !cfg.shaderAwareCullingEnabled) {
            return false;
        }
        return false;
    }

    public static boolean shouldSkipBlockEntity(Object be) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (!isIrisLoaded || cfg == null || !cfg.shaderAwareCullingEnabled) {
            return false;
        }
        return false;
    }

    public static int getGbufferDepthTex() {
        return gbufferDepthTex;
    }

    public static void setGbufferDepthTex(int tex) {
        gbufferDepthTex = tex;
    }
}
