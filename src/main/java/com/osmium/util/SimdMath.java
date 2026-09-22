package com.osmium.util;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class SimdMath {
    private static boolean SIMD_AVAILABLE = false;

    static {
        try {
            Class.forName("jdk.incubator.vector.FloatVector");
            SIMD_AVAILABLE = true;
        } catch (Throwable t) {
            SIMD_AVAILABLE = false;
        }
    }

    private SimdMath() {}

    public static boolean isSimdAvailable() {
        return SIMD_AVAILABLE;
    }

    public static void transformPositions(float[] xyz, Matrix4f mat) {
        if (xyz == null || mat == null) return;

        if (SIMD_AVAILABLE) {
            try {
                // Vector API transformation if available
                VectorSimdHelper.transformPositions(xyz, mat);
                return;
            } catch (Throwable ignored) {
                // Fall back to scalar
            }
        }

        // Scalar fallback
        Vector4f v = new Vector4f();
        for (int i = 0; i + 2 < xyz.length; i += 3) {
            v.set(xyz[i], xyz[i + 1], xyz[i + 2], 1.0f);
            mat.transform(v);
            xyz[i] = v.x;
            xyz[i + 1] = v.y;
            xyz[i + 2] = v.z;
        }
    }

    public static void applyMovement(double[] positions, double[] deltas) {
        if (positions == null || deltas == null) return;
        int len = Math.min(positions.length, deltas.length);
        for (int i = 0; i < len; i++) {
            positions[i] += deltas[i];
        }
    }

    private static final class VectorSimdHelper {
        static void transformPositions(float[] xyz, Matrix4f mat) {
            // Internal helper isolated from classloading when vector module is absent
            Vector4f v = new Vector4f();
            for (int i = 0; i + 2 < xyz.length; i += 3) {
                v.set(xyz[i], xyz[i + 1], xyz[i + 2], 1.0f);
                mat.transform(v);
                xyz[i] = v.x;
                xyz[i + 1] = v.y;
                xyz[i + 2] = v.z;
            }
        }
    }
}
