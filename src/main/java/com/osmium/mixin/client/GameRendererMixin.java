package com.osmium.mixin.client;

import com.osmium.OsmiumConfig;
import com.osmium.render.DynamicResolutionController;
import com.osmium.render.HierarchicalZCuller;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderWorld", at = @At("HEAD"), require = 1)
    private void osmium$beforeWorldRender(RenderTickCounter tickCounter,
            CallbackInfo ci) {
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.enabled) return;

        if (cfg.dynamicResolutionEnabled) {
            DynamicResolutionController.tick(cfg);
        }

        float scale = DynamicResolutionController.getScale();
        if (scale < 0.999f) {
            Window window = MinecraftClient.getInstance().getWindow();
            if (window != null) {
                int w = Math.max(1, (int) (window.getFramebufferWidth() * scale));
                int h = Math.max(1, (int) (window.getFramebufferHeight() * scale));
                RenderSystem.viewport(0, 0, w, h);
            }
        }
    }

    @Inject(method = "renderWorld", at = @At("RETURN"), require = 1)
    private void osmium$afterWorldRender(RenderTickCounter tickCounter,
            CallbackInfo ci) {
        Window window = MinecraftClient.getInstance().getWindow();
        if (window != null) {
            RenderSystem.viewport(0, 0,
                    window.getFramebufferWidth(), window.getFramebufferHeight());
        }
    }
}
