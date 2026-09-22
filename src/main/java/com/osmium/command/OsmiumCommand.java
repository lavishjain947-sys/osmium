package com.osmium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.osmium.OsmiumConfig;
import com.osmium.chunk.ChunkDataCompressor;
import com.osmium.core.FrameBudgetController;
import com.osmium.core.MemoryOrchestrator;
import com.osmium.core.OffHeapCache;
import com.osmium.pool.AABBPool;
import com.osmium.pool.BlockPosPool;
import com.osmium.pool.EnumValuesCache;
import com.osmium.pool.Vec3Pool;
import com.osmium.render.DynamicResolutionController;
import com.osmium.util.MemoryMath;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.Locale;

public final class OsmiumCommand {
    private OsmiumCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            ClientCommandManager.literal("osmium")
                .executes(ctx -> executeHelp(ctx.getSource()))
                .then(ClientCommandManager.literal("help")
                    .executes(ctx -> executeHelp(ctx.getSource())))
                .then(ClientCommandManager.literal("stats")
                    .executes(ctx -> executeStats(ctx.getSource())))
                .then(ClientCommandManager.literal("reload")
                    .executes(ctx -> executeReload(ctx.getSource())))
                .then(ClientCommandManager.literal("pools")
                    .executes(ctx -> executePools(ctx.getSource())))
                .then(ClientCommandManager.literal("offheap")
                    .executes(ctx -> executeOffHeap(ctx.getSource())))
                .then(ClientCommandManager.literal("target")
                    .then(ClientCommandManager.argument("fps", IntegerArgumentType.integer(10, 1000))
                        .executes(ctx -> executeTarget(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "fps")))))
                .then(ClientCommandManager.literal("scale")
                    .then(ClientCommandManager.argument("scale", FloatArgumentType.floatArg(0.5f, 1.0f))
                        .executes(ctx -> executeScale(ctx.getSource(), FloatArgumentType.getFloat(ctx, "scale")))))
        );
    }

    private static int executeHelp(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("osmium.command.help.header"));
        source.sendFeedback(Text.translatable("osmium.command.help.stats"));
        source.sendFeedback(Text.translatable("osmium.command.help.target"));
        source.sendFeedback(Text.translatable("osmium.command.help.scale"));
        source.sendFeedback(Text.translatable("osmium.command.help.reload"));
        source.sendFeedback(Text.translatable("osmium.command.help.pools"));
        source.sendFeedback(Text.translatable("osmium.command.help.offheap"));
        return 1;
    }

    private static int executeStats(FabricClientCommandSource source) {
        long usedMb = MemoryOrchestrator.getHeapUsedMb();
        long maxMb = MemoryOrchestrator.getHeapMaxMb();
        long allocRate = MemoryOrchestrator.getAllocationRateMbPerSec();
        double pct = maxMb > 0 ? ((double) usedMb / maxMb) * 100.0 : 0.0;
        String pressure = MemoryOrchestrator.getPressureLevel().name();

        source.sendFeedback(Text.translatable("osmium.command.stats.header"));
        source.sendFeedback(Text.translatable("osmium.command.stats.heap",
                String.valueOf(usedMb), String.valueOf(maxMb), String.format(Locale.US, "%.1f", pct),
                String.valueOf(allocRate), pressure));

        source.sendFeedback(Text.translatable("osmium.command.stats.gc",
                String.valueOf(MemoryOrchestrator.getGcPauseTotalMs()),
                String.valueOf(MemoryOrchestrator.getGcCollectionCount())));

        source.sendFeedback(Text.translatable("osmium.command.stats.frametimes",
                FrameBudgetController.getP50Ms(),
                FrameBudgetController.getP95Ms(),
                FrameBudgetController.getP99Ms()));

        long totalHits = BlockPosPool.hits() + AABBPool.hits() + Vec3Pool.hits();
        long totalMisses = BlockPosPool.misses() + AABBPool.misses() + Vec3Pool.misses();
        long total = totalHits + totalMisses;
        double hitRatio = total > 0 ? ((double) totalHits / total) * 100.0 : 0.0;

        source.sendFeedback(Text.translatable("osmium.command.stats.pools",
                String.valueOf(totalHits), String.valueOf(totalMisses), hitRatio));

        long offHeapUsedMb = MemoryMath.bytesToMb(OffHeapCache.usedBytes());
        long offHeapCapMb = MemoryMath.bytesToMb(OffHeapCache.capacityBytes());
        source.sendFeedback(Text.translatable("osmium.command.stats.offheap",
                String.valueOf(offHeapUsedMb), String.valueOf(offHeapCapMb),
                OffHeapCache.entryCount(), OffHeapCache.evictions()));

        source.sendFeedback(Text.translatable("osmium.command.stats.compression",
                ChunkDataCompressor.ratio()));

        return 1;
    }

    private static int executeTarget(FabricClientCommandSource source, int fps) {
        OsmiumConfig config = OsmiumConfig.get();
        config.targetFps = fps;
        config.onChanged();
        FrameBudgetController.setTargetFps(fps);
        source.sendFeedback(Text.translatable("osmium.command.target.set", fps));
        return 1;
    }

    private static int executeScale(FabricClientCommandSource source, float scale) {
        DynamicResolutionController.setManual(scale);
        source.sendFeedback(Text.translatable("osmium.command.scale.set", scale));
        return 1;
    }

    private static int executeReload(FabricClientCommandSource source) {
        OsmiumConfig config = OsmiumConfig.load();
        FrameBudgetController.setTargetFps(config.targetFps);
        OffHeapCache.init(config.offHeapCacheMb);
        source.sendFeedback(Text.translatable("osmium.command.reloaded"));
        return 1;
    }

    private static int executePools(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("osmium.command.pools.header"));
        source.sendFeedback(Text.translatable("osmium.command.pools.blockpos",
                BlockPosPool.size(), BlockPosPool.hits(), BlockPosPool.misses()));
        source.sendFeedback(Text.translatable("osmium.command.pools.aabb",
                AABBPool.size(), AABBPool.hits(), AABBPool.misses()));
        source.sendFeedback(Text.translatable("osmium.command.pools.vec3",
                Vec3Pool.size(), Vec3Pool.hits(), Vec3Pool.misses()));
        source.sendFeedback(Text.translatable("osmium.command.pools.enums",
                EnumValuesCache.cachedClassesCount()));
        return 1;
    }

    private static int executeOffHeap(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("osmium.command.offheap.header"));
        source.sendFeedback(Text.translatable("osmium.command.offheap.used",
                MemoryMath.humanReadable(OffHeapCache.usedBytes())));
        source.sendFeedback(Text.translatable("osmium.command.offheap.capacity",
                MemoryMath.humanReadable(OffHeapCache.capacityBytes())));
        long free = Math.max(0, OffHeapCache.capacityBytes() - OffHeapCache.usedBytes());
        source.sendFeedback(Text.translatable("osmium.command.offheap.free",
                MemoryMath.humanReadable(free)));
        source.sendFeedback(Text.translatable("osmium.command.offheap.entries",
                OffHeapCache.entryCount()));
        source.sendFeedback(Text.translatable("osmium.command.offheap.evictions",
                OffHeapCache.evictions()));
        source.sendFeedback(Text.translatable("osmium.command.offheap.mode",
                OffHeapCache.isFallbackMode() ? "On-Heap Fallback" : "Java 21 FFM Direct Slab"));
        return 1;
    }
}
