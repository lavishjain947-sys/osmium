package com.osmium.mixin.client;

import com.osmium.OsmiumConfig;
import com.osmium.chunk.PredictiveChunkLoader;
import com.osmium.core.FrameBudgetController;
import com.osmium.core.PredictionEngine;
import com.osmium.render.DynamicResolutionController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"), require = 1)
    private void osmium$onTickHead(CallbackInfo ci) {
        PredictionEngine.onTick((MinecraftClient) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 1)
    private void osmium$onTickTail(CallbackInfo ci) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.enabled) return;
        if (cfg.predictiveChunkLoadingEnabled) {
            PredictiveChunkLoader.onChunkTick((MinecraftClient) (Object) this);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 1)
    private void osmium$onRenderTail(boolean tick, CallbackInfo ci) {
        FrameBudgetController.onFrameEnd(System.nanoTime());
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg != null && cfg.enabled && cfg.dynamicResolutionEnabled) {
            DynamicResolutionController.tick(cfg);
        }
    }
}
