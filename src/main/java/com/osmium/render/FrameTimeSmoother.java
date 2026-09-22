package com.osmium.render;

import java.util.Arrays;

public final class FrameTimeSmoother {
    private FrameTimeSmoother() {}

    public static double ema(double prev, double curr, double alpha) {
        return (alpha * curr) + ((1.0 - alpha) * prev);
    }

    public static double[] quantiles(long[] frameTimes, double... q) {
        if (frameTimes == null || frameTimes.length == 0) {
            return new double[q.length];
        }

        long[] copy = Arrays.copyOf(frameTimes, frameTimes.length);
        Arrays.sort(copy);

        double[] results = new double[q.length];
        for (int i = 0; i < q.length; i++) {
            double quantile = Math.max(0.0, Math.min(1.0, q[i]));
            int index = (int) Math.round(quantile * (copy.length - 1));
            results[i] = copy[index] / 1_000_000.0;
        }

        return results;
    }
}
