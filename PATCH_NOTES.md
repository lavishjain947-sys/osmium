# Osmium — Patch Notes

## v1.0.4 Patch Notes

### Mixin Hardening & Resource Cleanup
1. **`src/main/java/com/osmium/mixin/client/MinecraftClientMixin.java`**: Promoted all `@Inject` annotations to `require = 1`.
2. **`src/main/java/com/osmium/mixin/client/GameRendererMixin.java`**: Promoted `renderWorld` HEAD and RETURN injections to `require = 1` and removed per-frame invalidation.
3. **`src/main/java/com/osmium/mixin/client/EntityRenderDispatcherMixin.java`**: Promoted `render` injection to `require = 1`.
4. **`src/main/java/com/osmium/render/HierarchicalZCuller.java`**: Added `destroy()` method to free FBO, depth texture, PBO, and sync fence.
5. **`src/main/java/com/osmium/core/OffHeapCache.java`**: Added `shutdown()` to cleanly close the FFM Arena, zero the slab, and clear structures.
6. **`src/main/java/com/osmium/core/MemoryOrchestrator.java`**: Added `shutdown()` to interrupt daemon thread and stop memory monitoring.
7. **`src/main/java/com/osmium/OsmiumClient.java`**: Registered `ClientLifecycleEvents.CLIENT_STOPPING` to invoke GPU and memory cleanup handlers on client shutdown.

---

## v1.0.3 Patch Notes

### Enhancements & Hardening
- **ChunkLRUCache.onEvict**: Extracted real block data from the chunk's first section using `chunk.getSectionArray()`, `section.getBlockState(x, y, z)`, and `Block.getRawIdFromState(state)` into `short[4096]` before Zstd compression and off-heap storage.
- **MinecraftClientMixin**: Promoted `render` and `tick` injections to `require = 1` for strict mixin validation.
- **GameRendererMixin**: Promoted `renderWorld` injections to `require = 1`.
- **EntityRenderDispatcherMixin**: Promoted `render` injection to `require = 1`.
- **HierarchicalZCuller**: Added `destroy()` method to release FBO, depth texture, PBO, and GL sync fence cleanly.
- **OsmiumClient**: Added explicit `ShaderAwareCuller.init()` initialization call.

---

## v1.0.2 Patch Notes

### Critical
- Fixed ClientChunkManagerMixin signature. This unblocks the entire
  chunk caching + off-heap compression pipeline.

### Verified
- ChunkDataCompressor.decompress raw tag handling
- FastMath negative angle wrap
- SimdMath scalar fallback

### Changed
- ModMenu integration split into two classes (safe loading)
- Removed AABBPool and Vec3Pool (immutability issue in 1.21.11)

---

## v1.0.1 Patch Notes

### Files Modified & Summary of Changes

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
