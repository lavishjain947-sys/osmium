package com.osmium.util;

import java.util.Locale;

public final class MemoryMath {
    private MemoryMath() {}

    public static long bytesToMb(long bytes) {
        return bytes / (1024L * 1024L);
    }

    public static String humanReadable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), unit);
    }

    public static double percentage(long used, long max) {
        if (max <= 0) return 0.0;
        return ((double) used / (double) max) * 100.0;
    }
}
