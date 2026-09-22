package com.osmium.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.osmium.OsmiumConfig;
import com.osmium.render.HierarchicalZCuller;
import com.osmium.render.ShaderAwareCuller;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void osmium$onRenderBlockEntity(BlockEntity blockEntity,
            float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, CallbackInfo ci) {

        if (blockEntity == null) return;

        if (ShaderAwareCuller.shouldSkipBlockEntity(blockEntity)) {
            ci.cancel();
            return;
        }

        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg == null || !cfg.enabled || !cfg.hierarchicalZCullingEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Window window = client.getWindow();
        if (window == null) return;
        int w = window.getFramebufferWidth();
        int h = window.getFramebufferHeight();
        if (w <= 0 || h <= 0) return;

        // Use the full block box plus a 1-block margin to cover the
        // render bounds of chests, signs, banners, beds, etc.
        BlockPos pos = blockEntity.getPos();
        Box box = new Box(pos).expand(1.0);

        // Combine view * projection for correct world->screen mapping.
        Matrix4f viewProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        viewProj.mul(RenderSystem.getModelViewMatrix());

        try {
            if (HierarchicalZCuller.isOccluded(box, viewProj, w, h)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Never let culling crash the render thread.
        }
    }
}

