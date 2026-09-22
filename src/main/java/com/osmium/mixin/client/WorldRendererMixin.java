package com.osmium.mixin.client;

import com.osmium.OsmiumConfig;
import com.osmium.render.HierarchicalZCuller;
import com.osmium.render.ShaderAwareCuller;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"), require = 0)
    private void osmium$onRenderEntitiesHead(CallbackInfo ci) {
        if (OsmiumConfig.get().hierarchicalZCullingEnabled) {
            HierarchicalZCuller.captureDepthMirror();
        }
    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"), require = 0)
    private void osmium$onRenderBlockEntitiesHead(CallbackInfo ci) {
        if (ShaderAwareCuller.isIrisLoaded) {
            ShaderAwareCuller.captureGbufferDepth();
        }
    }
}
