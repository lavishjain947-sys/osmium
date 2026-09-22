package com.osmium.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.osmium.OsmiumConfig;
import com.osmium.pool.AABBPool;
import com.osmium.render.HierarchicalZCuller;
import com.osmium.render.ShaderAwareCuller;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends BlockEntity> void osmium$onRenderBlockEntity(E blockEntity, float tickDelta, MatrixStack matrices,
                                                                   VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (blockEntity == null) return;

        if (ShaderAwareCuller.shouldSkipBlockEntity(blockEntity)) {
            ci.cancel();
            return;
        }

        OsmiumConfig config = OsmiumConfig.get();
        if (config.enabled && config.hierarchicalZCullingEnabled) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    BlockPos pos = blockEntity.getPos();
                    Box box = AABBPool.acquire(pos);
                    int w = client.getWindow().getFramebufferWidth();
                    int h = client.getWindow().getFramebufferHeight();
                    if (HierarchicalZCuller.isOccluded(box, RenderSystem.getProjectionMatrix(), w, h)) {
                        ci.cancel();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
