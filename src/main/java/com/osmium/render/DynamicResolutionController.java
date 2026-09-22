package com.osmium.render;

import com.osmium.OsmiumConfig;
import com.osmium.core.FrameBudgetController;

public final class DynamicResolutionController {
    private static float currentScale = 1.0f;
    private static boolean manual = false;
    private static float temporalWeight = 0.15f;
    private static int prevTextureId = -1;
    private static float lastDeltaPitch = 0.0f;
    private static float lastDeltaYaw = 0.0f;

    private DynamicResolutionController() {}

    public static void tick(OsmiumConfig cfg) {
        if (!cfg.enabled || !cfg.dynamicResolutionEnabled || manual) {
            return;
        }

        double p95 = FrameBudgetController.getP95Ms();
        double targetBudget = 1000.0 / Math.max(10, cfg.targetFps);
        float minScale = Math.max(0.5f, cfg.dynamicResolutionMinScale);

        if (p95 > targetBudget * 1.5) {
            currentScale = Math.max(minScale, currentScale - 0.05f);
        } else if (p95 < targetBudget * 0.8) {
            currentScale = Math.min(1.0f, currentScale + 0.02f);
        }

        temporalWeight = cfg.temporalBlendWeight;
        // Suppress temporal blend on fast camera turns to avoid ghosting
        if (Math.abs(lastDeltaPitch) > 2.0f || Math.abs(lastDeltaYaw) > 2.0f) {
            temporalWeight = 0.0f;
        }
    }

    public static int getScaledWidth(int nativeWidth) {
        return Math.max(1, (int) Math.round(nativeWidth * currentScale));
    }

    public static int getScaledHeight(int nativeHeight) {
        return Math.max(1, (int) Math.round(nativeHeight * currentScale));
    }

    public static float getScale() {
        return currentScale;
    }

    public static void setManual(float s) {
        manual = true;
        currentScale = Math.max(0.5f, Math.min(1.0f, s));
    }

    public static void resetAuto() {
        manual = false;
    }

    public static boolean isManual() {
        return manual;
    }

    public static float getTemporalBlendWeight() {
        return temporalWeight;
    }

    public static void updateCameraDelta(float deltaPitch, float deltaYaw) {
        lastDeltaPitch = deltaPitch;
        lastDeltaYaw = deltaYaw;
    }

    public static int getPrevTextureId() {
        return prevTextureId;
    }

    public static void setPrevTextureId(int id) {
        prevTextureId = id;
    }
}
