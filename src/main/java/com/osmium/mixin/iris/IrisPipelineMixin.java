package com.osmium.mixin.iris;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Placeholder for future Iris integration. Disabled in v1.0.1.
 * Shader-aware culling is planned for v1.1 after Iris API verification.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.WorldRenderingPipeline")
public class IrisPipelineMixin {
    // Intentionally empty.
}
