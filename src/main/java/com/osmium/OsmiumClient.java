package com.osmium;

import com.osmium.command.OsmiumCommand;
import com.osmium.core.FrameBudgetController;
import com.osmium.core.MemoryOrchestrator;
import com.osmium.core.OffHeapCache;
import com.osmium.core.PredictionEngine;
import com.osmium.integration.ModMenuIntegration;
import com.osmium.integration.SodiumIntegration;
import com.osmium.util.JvmTuner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

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

        if (ModMenuIntegration.isLoaded()) {
            OsmiumConstants.LOGGER.info("ModMenu detected.");
        }
    }
}
