package com.osmium.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public final class PredictionEngine {
    private static final int HISTORY_SIZE = 20;
    private static final Vec3d[] positionHistory = new Vec3d[HISTORY_SIZE];
    private static final long[] timeHistory = new long[HISTORY_SIZE];
    private static int head = 0;
    private static int count = 0;

    private static volatile Vec3d currentVelocity = Vec3d.ZERO;
    private static volatile Vec3d currentAcceleration = Vec3d.ZERO;
    private static volatile Vec3d latestPosition = Vec3d.ZERO;

    private PredictionEngine() {}

    public static void init() {
        head = 0;
        count = 0;
        currentVelocity = Vec3d.ZERO;
        currentAcceleration = Vec3d.ZERO;
        latestPosition = Vec3d.ZERO;
    }

    public static synchronized void onTick(MinecraftClient client) {
        if (client == null || client.player == null) return;

        Vec3d pos = client.player.getPos();
        long now = System.currentTimeMillis();

        positionHistory[head] = pos;
        timeHistory[head] = now;
        latestPosition = pos;

        if (count > 0) {
            int prevIdx = (head - 1 + HISTORY_SIZE) % HISTORY_SIZE;
            Vec3d prevPos = positionHistory[prevIdx];
            long prevTime = timeHistory[prevIdx];

            if (prevPos != null && now > prevTime) {
                // Compute tick-based velocity (units per tick)
                Vec3d newVel = pos.subtract(prevPos);
                currentAcceleration = newVel.subtract(currentVelocity);
                currentVelocity = newVel;
            }
        }

        head = (head + 1) % HISTORY_SIZE;
        if (count < HISTORY_SIZE) {
            count++;
        }
    }

    public static Vec3d predictPosition(int ticksAhead) {
        if (latestPosition == null) return Vec3d.ZERO;
        double t = ticksAhead;
        // pos + vel * t + 0.5 * accel * t^2
        double x = latestPosition.x + currentVelocity.x * t + 0.5 * currentAcceleration.x * t * t;
        double y = latestPosition.y + currentVelocity.y * t + 0.5 * currentAcceleration.y * t * t;
        double z = latestPosition.z + currentVelocity.z * t + 0.5 * currentAcceleration.z * t * t;
        return new Vec3d(x, y, z);
    }

    public static ChunkPos predictChunk(int ticksAhead) {
        Vec3d predicted = predictPosition(ticksAhead);
        int chunkX = (int) Math.floor(predicted.x) >> 4;
        int chunkZ = (int) Math.floor(predicted.z) >> 4;
        return new ChunkPos(chunkX, chunkZ);
    }

    public static Vec3d currentVelocity() {
        return currentVelocity;
    }
}
