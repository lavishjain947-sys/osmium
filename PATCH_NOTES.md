# Osmium v1.0.1 — Patch Notes

This patch addresses 15 critical, high, and medium severity issues across rendering, mixins, memory pooling, and modularity.

## Files Modified & Summary of Changes

1. **`src/main/java/com/osmium/mixin/client/ClientChunkManagerMixin.java`**: Corrected packet chunk load method parameters and error handling for 1.21.11 Yarn mappings.
2. **`src/main/java/com/osmium/mixin/client/EntityRenderDispatcherMixin.java`**: Multiplied projection matrix by model-view matrix for accurate world-to-screen occlusion testing.
3. **`src/main/java/com/osmium/render/HierarchicalZCuller.java`**: Replaced blocking `glReadPixels` with asynchronous Pixel Buffer Objects (PBO) and sync fences to eliminate GPU stalls.
4. **`src/main/java/com/osmium/mixin/client/GameRendererMixin.java`**: Added dynamic viewport scaling in `renderWorld` HEAD and viewport restoration in RETURN.
5. **`src/main/java/com/osmium/OsmiumConfig.java`**: Implemented thread-safe double-checked locking `get()` singleton accessor.
6. **`src/main/java/com/osmium/pool/EnumValuesCache.java`**: Updated reflection to use Java 21 `MethodHandles.privateLookupIn` and `VarHandle`.
7. **`src/main/java/com/osmium/chunk/ChunkDataCompressor.java`**: Fixed `TAG_RAW` and stage payload offset handling in `decompress()`.
8. **`src/main/java/com/osmium/mixin/client/MinecraftClientMixin.java`**: Moved `PredictiveChunkLoader` invocation to `tick` at `TAIL` for per-tick motion calculation.
9. **`src/main/java/com/osmium/mixin/iris/IrisPipelineMixin.java`**: Stubbed out Iris mixin to prevent load warnings and crashes in v1.0.1.
10. **`src/main/resources/osmium.mixins.json`**: Removed `iris.IrisPipelineMixin` from client mixin registration for v1.0.1 stability.
11. **`src/main/java/com/osmium/render/ShaderAwareCuller.java`**: Neutralized Iris depth hooks and made skip queries return false safely.
12. **`src/main/java/com/osmium/core/ObjectPool.java`**: Made `shrinkAll` thread-safe using non-blocking `queue.poll()`.
13. **`src/main/java/com/osmium/util/FastMath.java`**: Fixed trigonometry table indexing to wrap negative input angles correctly.
14. **`src/main/java/com/osmium/util/SimdMath.java`**: Provided an optimized scalar fallback for vector transformations.
15. **`src/main/java/com/osmium/integration/ModMenuIntegration.java`**: Removed ModMenu interface inheritance to prevent classloading errors when ModMenu is absent.
16. **`src/main/java/com/osmium/integration/ModMenuApiImpl.java`**: Created dedicated entrypoint for ModMenu integration.
17. **`src/main/java/com/osmium/OsmiumClient.java`**: Removed direct references to ModMenu and Iris from client initializer.
18. **`src/main/resources/fabric.mod.json`**: Registered `"modmenu"` entrypoint pointing to `ModMenuApiImpl`.
19. **`README.md`**: Updated recommended JVM arguments with `--add-opens java.base/java.lang=ALL-UNNAMED` and v1.0.1 feature notes.
20. **`CHANGELOG.md`**: Documented all v1.0.1 fixes and enhancements.
