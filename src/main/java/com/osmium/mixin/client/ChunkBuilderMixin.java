package com.osmium.mixin.client;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {
    @Inject(method = "rebuild", at = @At("HEAD"), require = 0)
    private void osmium$onRebuildHead(CallbackInfo ci) {
        // Placeholder hook for chunk builder object pool integration
        // Kept non-invasive to maintain 100% compatibility with Sodium
    }
}
