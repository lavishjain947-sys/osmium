package com.osmium.mixin.client;

import com.osmium.chunk.ChunkLRUCache;
import com.osmium.chunk.PredictiveChunkLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;

@Mixin(ClientChunkManager.class)
public class ClientChunkManagerMixin {

    @Inject(method = "loadChunkFromPacket", at = @At("RETURN"), require = 0)
    private void osmium$onLoadChunkFromPacket(int x, int z, PacketByteBuf buf, NbtCompound tag,
            java.util.function.Consumer<?> consumer,
            CallbackInfoReturnable<WorldChunk> cir) {
        WorldChunk chunk = cir.getReturnValue();
        if (chunk != null) {
            ChunkLRUCache.put(ChunkPos.toLong(x, z), chunk);
        }
    }

    @Inject(method = "unload", at = @At("HEAD"), require = 0)
    private void osmium$onUnload(int x, int z, CallbackInfo ci) {
        ChunkLRUCache.remove(ChunkPos.toLong(x, z));
    }

    @Inject(method = "updateLoadDistance", at = @At("TAIL"), require = 0)
    private void osmium$onUpdateLoadDistance(int loadDistance, CallbackInfo ci) {
        // Reserved: config change hook. Predictive loader is ticked from
        // MinecraftClientMixin now (see FIX 8).
    }
}
