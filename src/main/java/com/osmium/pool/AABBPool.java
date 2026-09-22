package com.osmium.pool;

import com.osmium.core.ObjectPool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public final class AABBPool {
    private static final ObjectPool<Box> POOL = new ObjectPool<>(
        () -> new Box(0, 0, 0, 1, 1, 1),
        b -> {},
        512
    );

    private AABBPool() {}

    public static Box acquire(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Box(x1, y1, z1, x2, y2, z2);
    }

    public static Box acquire(BlockPos pos) {
        return new Box(pos);
    }

    public static Box acquire(BlockPos pos, VoxelShape shape) {
        if (shape == null || shape.isEmpty()) {
            return new Box(pos);
        }
        return shape.getBoundingBox().offset(pos);
    }

    public static void release(Box box) {
        if (box != null) {
            POOL.release(box);
        }
    }

    public static int size() {
        return POOL.size();
    }

    public static long hits() {
        return POOL.hits();
    }

    public static long misses() {
        return POOL.misses();
    }
}
