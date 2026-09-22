package com.osmium.pool;

import com.osmium.core.ObjectPool;
import net.minecraft.util.math.BlockPos;

public final class BlockPosPool {
    private static final ObjectPool<BlockPos.Mutable> POOL = new ObjectPool<>(
        BlockPos.Mutable::new,
        p -> p.set(0, 0, 0),
        512
    );

    private BlockPosPool() {}

    public static BlockPos acquire(int x, int y, int z) {
        BlockPos.Mutable pos = POOL.acquire();
        pos.set(x, y, z);
        return pos;
    }

    public static void release(BlockPos p) {
        if (p instanceof BlockPos.Mutable mutable) {
            POOL.release(mutable);
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
