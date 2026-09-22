package com.osmium.integration;

import com.osmium.OsmiumConstants;
import net.fabricmc.loader.api.FabricLoader;

public final class SodiumIntegration {
    private static boolean loaded = false;

    private SodiumIntegration() {}

    public static boolean isLoaded() {
        return loaded;
    }

    public static void onInit() {
        loaded = FabricLoader.getInstance().isModLoaded("sodium");
        if (loaded) {
            OsmiumConstants.LOGGER.info("[Osmium] Sodium detected — Osmium complements Sodium's GPU work.");
        }
    }
}
