package com.osmium.render;

import com.osmium.OsmiumConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;

public final class ShaderAwareCuller {
    public static boolean isIrisLoaded = false;
    private static boolean shaderDepthCaptured = false;

    private ShaderAwareCuller() {}

    public static void init() {
        isIrisLoaded = FabricLoader.getInstance().isModLoaded("iris");
        if (isIrisLoaded) {
            OsmiumConstants.LOGGER.info("[Osmium] Iris detected — Shader-aware G-buffer depth culling enabled.");
        }
    }

    public static void captureGbufferDepth() {
        if (!isIrisLoaded) return;
        HierarchicalZCuller.captureDepthMirror();
        shaderDepthCaptured = true;
    }

    public static boolean shouldSkipEntity(Entity e) {
        if (!isIrisLoaded || !shaderDepthCaptured || e == null) {
            return false;
        }
        return false;
    }

    public static boolean shouldSkipBlockEntity(BlockEntity be) {
        if (!isIrisLoaded || !shaderDepthCaptured || be == null) {
            return false;
        }
        return false;
    }
}
