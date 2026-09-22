package com.osmium.pool;

import com.osmium.core.ObjectPool;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class Vec3Pool {
    private static final ObjectPool<Vector3f> POOL_F = new ObjectPool<>(
        Vector3f::new,
        v -> v.set(0.0f, 0.0f, 0.0f),
        512
    );

    private static final ObjectPool<Vec3d> POOL_D = new ObjectPool<>(
        () -> new Vec3d(0, 0, 0),
        v -> {},
        512
    );

    private Vec3Pool() {}

    public static Vec3d acquireD(double x, double y, double z) {
        return new Vec3d(x, y, z);
    }

    public static Vector3f acquireF(float x, float y, float z) {
        Vector3f v = POOL_F.acquire();
        v.set(x, y, z);
        return v;
    }

    public static void release(Vec3d vec) {
        if (vec != null) {
            POOL_D.release(vec);
        }
    }

    public static void release(Vector3f vec) {
        if (vec != null) {
            POOL_F.release(vec);
        }
    }

    public static int size() {
        return POOL_D.size() + POOL_F.size();
    }

    public static long hits() {
        return POOL_D.hits() + POOL_F.hits();
    }

    public static long misses() {
        return POOL_D.misses() + POOL_F.misses();
    }
}
