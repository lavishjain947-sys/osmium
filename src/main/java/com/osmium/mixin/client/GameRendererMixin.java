package com.osmium.mixin.client;

import com.osmium.render.DynamicResolutionController;
import com.osmium.render.HierarchicalZCuller;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final MinecraftClient client;

    @Unique
    private int osmium$lastWidth = -1;
    @Unique
    private int osmium$lastHeight = -1;

    @Inject(method = "renderWorld", at = @At("HEAD"), require = 0)
    private void osmium$onRenderWorldHead(RenderTickCounter tickCounter, CallbackInfo ci) {
        float scale = DynamicResolutionController.getScale();
        if (scale < 1.0f && client != null && client.getFramebuffer() != null) {
            Framebuffer fb = client.getFramebuffer();
            int scaledW = DynamicResolutionController.getScaledWidth(fb.textureWidth);
            int scaledH = DynamicResolutionController.getScaledHeight(fb.textureHeight);
            RenderSystem.viewport(0, 0, scaledW, scaledH);
        }
    }

    @Inject(method = "renderWorld", at = @At("TAIL"), require = 0)
    private void osmium$onRenderWorldTail(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (client != null && client.getFramebuffer() != null) {
            Framebuffer fb = client.getFramebuffer();
            RenderSystem.viewport(0, 0, fb.textureWidth, fb.textureHeight);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void osmium$onRenderTail(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (client != null && client.getWindow() != null) {
            int w = client.getWindow().getFramebufferWidth();
            int h = client.getWindow().getFramebufferHeight();
            if (w != osmium$lastWidth || h != osmium$lastHeight) {
                osmium$lastWidth = w;
                osmium$lastHeight = h;
                HierarchicalZCuller.invalidate();
            }
        }
    }
}
