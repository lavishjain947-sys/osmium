package com.osmium.mixin.client;

import com.osmium.OsmiumConfig;
import com.osmium.render.HierarchicalZCuller;
import com.osmium.render.ShaderAwareCuller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends Entity> void osmium$beforeRender(E entity,
            double x, double y, double z, float yaw, float tickDelta,
            Matrix4f matrix, net.minecraft.client.render.VertexConsumerProvider vcp,
            int light, CallbackInfo ci) {

        if (entity == null) return;
        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.enabled) return;

        // Iris integration takes priority when active
        if (ShaderAwareCuller.isIrisLoaded
                && ShaderAwareCuller.shouldSkipEntity(entity)) {
            ci.cancel();
            return;
        }

        if (!cfg.hierarchicalZCullingEnabled) return;

        Window window = MinecraftClient.getInstance().getWindow();
        if (window == null) return;
        int w = window.getFramebufferWidth();
        int h = window.getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        // Combine view * projection for correct world->screen mapping
        Matrix4f viewProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        viewProj.mul(RenderSystem.getModelViewMatrix());

        if (HierarchicalZCuller.isOccluded(entity.getBoundingBox(),
                viewProj, w, h)) {
            ci.cancel();
        }
    }
}
