package com.osmium.render;

public final class ShaderAwareCuller {
    public static boolean isIrisLoaded = false;

    private ShaderAwareCuller() {}

    public static void init() {
        isIrisLoaded = false;
    }

    public static void captureGbufferDepth() {
        // no-op in v1.0.1
    }

    public static boolean shouldSkipEntity(Object e) {
        return false;
    }

    public static boolean shouldSkipBlockEntity(Object be) {
        return false;
    }
}
