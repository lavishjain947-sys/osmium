package com.osmium;

import com.osmium.command.OsmiumCommand;
import com.osmium.core.FrameBudgetController;
import com.osmium.core.MemoryOrchestrator;
import com.osmium.core.OffHeapCache;
import com.osmium.core.PredictionEngine;
import com.osmium.integration.ModMenuIntegration;
import com.osmium.integration.SodiumIntegration;
import com.osmium.render.ShaderAwareCuller;
import com.osmium.util.JvmTuner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class OsmiumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OsmiumConfig config = OsmiumConfig.get();

        OsmiumConstants.LOGGER.info("==================================================");
        OsmiumConstants.LOGGER.info("{} v{} — Densest optimization. Zero stutter. Maximum FPS.",
                OsmiumConstants.MOD_NAME, OsmiumConstants.VERSION);
        OsmiumConstants.LOGGER.info("==================================================");

        MemoryOrchestrator.start(config);
        OffHeapCache.init(config.offHeapCacheMb);
        FrameBudgetController.init(config.targetFps);
        PredictionEngine.init();

        ClientCommandRegistrationCallback.EVENT.register(OsmiumCommand::register);
        JvmTuner.logRecommendations();

        SodiumIntegration.onInit();
        ShaderAwareCuller.init();

        if (ModMenuIntegration.isLoaded()) {
            OsmiumConstants.LOGGER.info("ModMenu detected.");
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                com.osmium.render.HierarchicalZCuller.destroy();
                com.osmium.core.OffHeapCache.shutdown();
                com.osmium.core.MemoryOrchestrator.shutdown();
            } catch (Throwable t) {
                com.osmium.OsmiumConstants.LOGGER.warn(
                        "Osmium: error during shutdown cleanup", t);
            }
        });
    }
}
