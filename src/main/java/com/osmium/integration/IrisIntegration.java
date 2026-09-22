package com.osmium.integration;

import com.osmium.OsmiumConstants;
import com.osmium.render.ShaderAwareCuller;
import net.fabricmc.loader.api.FabricLoader;

public final class IrisIntegration {
    private static boolean loaded = false;

    private IrisIntegration() {}

    public static boolean isLoaded() {
        return loaded;
    }

    public static void onInit() {
        loaded = FabricLoader.getInstance().isModLoaded("iris");
        if (loaded) {
            OsmiumConstants.LOGGER.info("[Osmium] Iris detected — Enabling shader-aware depth culling.");
            ShaderAwareCuller.init();
        }
    }
}
