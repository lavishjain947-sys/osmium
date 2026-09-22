package com.osmium.integration;

import net.fabricmc.loader.api.FabricLoader;

public final class ModMenuIntegration {
    private ModMenuIntegration() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("modmenu");
    }
}
