package com.osmium.core;

import com.osmium.OsmiumConstants;

import java.util.Arrays;

public final class FrameBudgetController {
    private static int targetFps = OsmiumConstants.DEFAULT_TARGET_FPS;
    private static final long[] frameTimesNanos = new long[OsmiumConstants.FRAME_HISTORY];
    private static int head = 0;
    private static int count = 0;

    private static volatile double p50Ms = 10.0;
    private static volatile double p95Ms = 10.0;
    private static volatile double p99Ms = 10.0;
    private static volatile boolean throttling = false;

    private FrameBudgetController() {}

    public static void init(int fps) {
        setTargetFps(fps);
        Arrays.fill(frameTimesNanos, (long) (1_000_000_000.0 / Math.max(10, fps)));
        count = OsmiumConstants.FRAME_HISTORY;
        recalculateQuantiles();
    }

    public static synchronized void onFrameEnd(long nanos) {
        frameTimesNanos[head] = nanos;
        head = (head + 1) % OsmiumConstants.FRAME_HISTORY;
        if (count < OsmiumConstants.FRAME_HISTORY) {
            count++;
        }
        recalculateQuantiles();
    }

    private static void recalculateQuantiles() {
        if (count == 0) return;

        long[] copy = new long[count];
        System.arraycopy(frameTimesNanos, 0, copy, 0, count);
        Arrays.sort(copy);

        int idx50 = (int) (count * 0.50);
        int idx95 = (int) Math.min(count - 1, count * 0.95);
        int idx99 = (int) Math.min(count - 1, count * 0.99);

        p50Ms = copy[idx50] / 1_000_000.0;
        p95Ms = copy[idx95] / 1_000_000.0;
        p99Ms = copy[idx99] / 1_000_000.0;

        double targetBudgetMs = 1000.0 / targetFps;
        throttling = (p95Ms > targetBudgetMs * 1.5);
    }

    public static double getP50Ms() {
        return p50Ms;
    }

    public static double getP95Ms() {
        return p95Ms;
    }

    public static double getP99Ms() {
        return p99Ms;
    }

    public static boolean shouldReduceResolution() {
        return throttling;
    }

    public static boolean shouldThrottleChunks() {
        return throttling;
    }

    public static void setTargetFps(int fps) {
        targetFps = Math.max(10, Math.min(1000, fps));
    }

    public static int getTargetFps() {
        return targetFps;
    }
}
