package com.osmium.mixin.client;

import com.osmium.OsmiumConfig;
import com.osmium.core.FrameBudgetController;
import com.osmium.core.PredictionEngine;
import com.osmium.render.DynamicResolutionController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private long osmium$frameStartTime = System.nanoTime();

    @Inject(method = "tick", at = @At("HEAD"))
    private void osmium$onTick(CallbackInfo ci) {
        PredictionEngine.onTick((MinecraftClient) (Object) this);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void osmium$onRenderHead(boolean tick, CallbackInfo ci) {
        osmium$frameStartTime = System.nanoTime();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void osmium$onRenderTail(boolean tick, CallbackInfo ci) {
        long frameDuration = System.nanoTime() - osmium$frameStartTime;
        FrameBudgetController.onFrameEnd(frameDuration);
        DynamicResolutionController.tick(OsmiumConfig.get());
    }
}
