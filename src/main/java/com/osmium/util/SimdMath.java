package com.osmium.util;

import org.joml.Matrix4f;

/**
 * Math utilities for batch vector operations.
 * Full SIMD Vector API acceleration is planned for v1.1 after cross-platform testing.
 * v1.0.1 provides an optimized scalar fallback.
 */
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
        for (int i = 0; i + 2 < xyz.length; i += 3) {
            float x = xyz[i], y = xyz[i + 1], z = xyz[i + 2];
            float nx = mat.m00() * x + mat.m10() * y + mat.m20() * z + mat.m30();
            float ny = mat.m01() * x + mat.m11() * y + mat.m21() * z + mat.m31();
            float nz = mat.m02() * x + mat.m12() * y + mat.m22() * z + mat.m32();
            xyz[i] = nx;
            xyz[i + 1] = ny;
            xyz[i + 2] = nz;
        }
    }

    public static void applyMovement(double[] positions, double[] deltas) {
        if (positions == null || deltas == null) return;
        int n = Math.min(positions.length, deltas.length);
        for (int i = 0; i < n; i++) {
            positions[i] += deltas[i];
        }
    }
}
