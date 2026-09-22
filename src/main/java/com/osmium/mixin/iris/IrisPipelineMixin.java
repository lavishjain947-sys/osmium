package com.osmium.mixin.iris;

import com.osmium.render.ShaderAwareCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.WorldRenderingPipeline", remap = false)
public class IrisPipelineMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 0, remap = false)
    private void osmium$onIrisRenderHead(CallbackInfo ci) {
        ShaderAwareCuller.captureGbufferDepth();
    }
}
