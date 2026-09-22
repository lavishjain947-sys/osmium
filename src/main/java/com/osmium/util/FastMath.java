package com.osmium.util;

public final class FastMath {
    private static final int TABLE_SIZE = 65536;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    public static final float[] SIN_TABLE = new float[TABLE_SIZE];
    public static final float[] COS_TABLE = new float[TABLE_SIZE];

    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            float rad = (i * TWO_PI) / TABLE_SIZE;
            SIN_TABLE[i] = (float) Math.sin(rad);
            COS_TABLE[i] = (float) Math.cos(rad);
        }
    }

    private FastMath() {}

    public static float sin(float radians) {
        int index = (int) (radians * 10430.378f) & 0xFFFF;
        return SIN_TABLE[index];
    }

    public static float cos(float radians) {
        int index = (int) (radians * 10430.378f + 16384.0f) & 0xFFFF;
        if (index < 0) index += 65536;
        return SIN_TABLE[index];
    }

    public static double fastSqrt(double x) {
        if (x <= 0) return 0;
        return Double.longBitsToDouble((Double.doubleToLongBits(x) + 0x3FF0000000000000L) >> 1);
    }
}
