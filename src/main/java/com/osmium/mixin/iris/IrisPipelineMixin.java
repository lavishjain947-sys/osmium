package com.osmium.mixin.iris;

import com.osmium.render.ShaderAwareCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Iris shader pipeline to capture G-buffer depth for occlusion culling.
 * Uses @Pseudo and require = 0 for optional, fault-tolerant mod compatibility.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.WorldRenderingPipeline", remap = false)
public class IrisPipelineMixin {

    @Inject(method = "renderTerrainBegin", at = @At("HEAD"), require = 0, cancellable = true)
    private void osmium$onRenderTerrainBegin(CallbackInfo ci) {
        ShaderAwareCuller.captureGbufferDepth();
    }

    @Inject(method = "beginLevelRendering", at = @At("HEAD"), require = 0, cancellable = true)
    private void osmium$onBeginLevelRendering(CallbackInfo ci) {
        ShaderAwareCuller.captureGbufferDepth();
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            com.osmium.render.CheckerboardRenderer.beginFrame(
                    client.getWindow().getFramebufferWidth(),
                    client.getWindow().getFramebufferHeight()
            );
        }
    }
}
