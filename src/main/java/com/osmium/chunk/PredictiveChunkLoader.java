package com.osmium.chunk;

import com.osmium.core.FrameBudgetController;
import com.osmium.core.PredictionEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;

import java.util.Comparator;
import java.util.PriorityQueue;

public final class PredictiveChunkLoader {
    public record PrioritizedChunk(ChunkPos pos, int priority, double distanceSq) {}

    private static final PriorityQueue<PrioritizedChunk> chunkQueue = new PriorityQueue<>(
        Comparator.comparingInt(PrioritizedChunk::priority)
            .thenComparingDouble(PrioritizedChunk::distanceSq)
    );

    private static volatile ChunkPos currentTarget = null;

    private PredictiveChunkLoader() {}

    public static synchronized void onChunkTick(MinecraftClient client) {
        if (client == null || client.player == null) return;

        ChunkPos predicted = PredictionEngine.predictChunk(40);
        ChunkPos current = client.player.getChunkPos();

        chunkQueue.clear();

        // Priority 0: Predicted chunk and 3x3 neighbors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos p = new ChunkPos(predicted.x + dx, predicted.z + dz);
                chunkQueue.offer(new PrioritizedChunk(p, 0, distSq(p, predicted)));
            }
        }

        // Priority 1: Current chunk and 3x3 neighbors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos p = new ChunkPos(current.x + dx, current.z + dz);
                chunkQueue.offer(new PrioritizedChunk(p, 1, distSq(p, current)));
            }
        }

        // Priority 2: Interpolated path
        int steps = 5;
        for (int i = 1; i < steps; i++) {
            double alpha = (double) i / steps;
            int px = (int) Math.round(current.x + alpha * (predicted.x - current.x));
            int pz = (int) Math.round(current.z + alpha * (predicted.z - current.z));
            ChunkPos p = new ChunkPos(px, pz);
            chunkQueue.offer(new PrioritizedChunk(p, 2, distSq(p, current)));
        }

        PrioritizedChunk top = chunkQueue.peek();
        currentTarget = (top != null) ? top.pos() : predicted;
    }

    private static double distSq(ChunkPos a, ChunkPos b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    public static ChunkPos getPriorityTarget() {
        return currentTarget;
    }

    public static int getCurrentBudget() {
        return FrameBudgetController.shouldThrottleChunks() ? 1 : 3;
    }
}
