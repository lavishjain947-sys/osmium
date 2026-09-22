package com.osmium.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.osmium.OsmiumConfig;
import com.osmium.render.HierarchicalZCuller;
import com.osmium.render.ShaderAwareCuller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void osmium$onRenderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                                      MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                      CallbackInfo ci) {
        if (entity == null) return;

        if (ShaderAwareCuller.shouldSkipEntity(entity)) {
            ci.cancel();
            return;
        }

        OsmiumConfig config = OsmiumConfig.get();
        if (config.enabled && config.hierarchicalZCullingEnabled) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    int w = client.getWindow().getFramebufferWidth();
                    int h = client.getWindow().getFramebufferHeight();
                    if (HierarchicalZCuller.isOccluded(entity.getBoundingBox(), RenderSystem.getProjectionMatrix(), w, h)) {
                        ci.cancel();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
