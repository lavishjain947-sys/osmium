# Changelog

All notable changes to this project will be documented in this file.

## 1.0.5 — Block Entity Culling Fix

### Fixed
- **BlockEntityRenderDispatcherMixin**: Rewrote mixin using full view-projection matrix (`RenderSystem.getProjectionMatrix() * RenderSystem.getModelViewMatrix()`), expanded bounding box (`new Box(pos).expand(1.0)` to properly encompass chests, signs, banners, beds, and shulker boxes), concrete `BlockEntity` parameter type, and promoted `require = 1`.
- **WorldRendererMixin**: Promoted `renderEntities` injection to `require = 1`. Kept `renderBlockEntities` as `require = 0` no-op reserved for v1.1.
- **GameRendererMixin**: Removed dead field `osmium$lastStateValid`.
- **Low-RAM Tuning**: Tuned `gradle.properties` JVM args to `-Xmx768M -XX:MaxMetaspaceSize=256M -XX:+UseG1GC -XX:G1NewSizePercent=40 -XX:MaxGCPauseMillis=50` with daemon/parallel disabled and configure-on-demand enabled for 4GB RAM systems.
- **OsmiumClient**: Removed unused `ShaderAwareCuller.init()` call and import.

## 1.0.4 — Mixin Hardening & GPU Cleanup

### Fixed
- **Mixin Hardening**: Promoted `require = 1` across `MinecraftClientMixin`, `GameRendererMixin`, and `EntityRenderDispatcherMixin` to prevent silent mixin failures on mapping changes.
- **GPU Resource Leak**: Added `HierarchicalZCuller.destroy()` to cleanly deallocate OpenGL FBO, depth texture, PBO, and sync fences.
- **Client Lifecycle Cleanup**: Registered Fabric `ClientLifecycleEvents.CLIENT_STOPPING` in `OsmiumClient` invoking `HierarchicalZCuller.destroy()`, `OffHeapCache.shutdown()`, and `MemoryOrchestrator.shutdown()`.
- **Core Shutdown Handlers**: Implemented `OffHeapCache.shutdown()` (closing FFM Arena, zeroing slab, clearing tracking structures) and `MemoryOrchestrator.shutdown()`.

## 1.0.3 — Real Chunk Eviction & Pipeline Hardening

### Added
- **Real Chunk Data Eviction**: `ChunkLRUCache.onEvict()` extracts real block state IDs from `WorldChunk` chunk sections into `short[4096]` volumes for compression and off-heap caching.
- **HierarchicalZCuller Lifecycle**: Added `destroy()` method to cleanly deallocate OpenGL FBO, depth texture, PBO, and sync fences.
- **ShaderAwareCuller Initialization**: Added explicit `init()` call in `OsmiumClient`.

### Hardened
- Promoted mixin injections in `MinecraftClientMixin`, `GameRendererMixin`, and `EntityRenderDispatcherMixin` to `require = 1` for strict compile/load validation.

## 1.0.2 — Patch Release

### Fixed
- ClientChunkManagerMixin: correct method signature for 1.21.11 Yarn
  mappings (BiomeArray + BitSet parameters, WorldChunk return type).
  Requires = 1 now enforces signature validation at build time.
- ChunkDataCompressor: verified raw tag handling in decompress().
- FastMath: verified negative angle wrap for sin/cos.
- SimdMath: verified scalar fallback implementations.
- ModMenuIntegration: separated ModMenuApi into its own class to prevent
  NoClassDefFoundError when ModMenu is absent.
- Removed AABBPool and Vec3Pool (immutable objects cannot be pooled
  effectively in 1.21.11). BlockPosPool retained (BlockPos.Mutable is safe).

### Changed
- Chunk LRU caching and off-heap compression now active (were silently
  disabled due to the mixin signature issue).

## 1.0.1 — Critical Bug Fix Patch
- **ClientChunkManagerMixin**: Fixed packet chunk load method signature and parameters for 1.21.11.
- **EntityRenderDispatcherMixin**: Combined projection matrix with model-view camera matrix for accurate occlusion culling.
- **HierarchicalZCuller**: Replaced synchronous `glReadPixels` with asynchronous Pixel Buffer Objects (PBO) and sync fences, preventing GPU pipeline stalls.
- **GameRendererMixin**: Added active viewport scaling and restoration hooks during 3D world rendering pass.
- **OsmiumConfig**: Added thread-safe double-checked locking static `get()` singleton accessor.
- **EnumValuesCache**: Migrated reflection to Java 21 `MethodHandles.Lookup` and `VarHandle` with fallback.
- **ChunkDataCompressor**: Fixed raw tag handling and buffer wrapping logic in `decompress()`.
- **MinecraftClientMixin**: Hooked `PredictiveChunkLoader` into `tick` at `TAIL` for per-tick motion calculation.
- **IrisPipelineMixin & ShaderAwareCuller**: Safely stubbed out Iris mixin and pipeline hooks for v1.0.1 stability.
- **ObjectPool**: Replaced `shrinkAll` with a thread-safe implementation using `ConcurrentLinkedQueue.poll()`.
- **FastMath**: Fixed trigonometry table indexing to wrap negative angles correctly.
- **SimdMath**: Added optimized scalar fallback routines for vector transformations.
- **ModMenuIntegration**: Decoupled ModMenu integration into a separate entrypoint (`ModMenuApiImpl`) to prevent classloading errors.
- **Documentation**: Updated recommended JVM arguments with `--add-opens java.base/java.lang=ALL-UNNAMED`.

## 1.0.0 — Initial Release
- Off-heap chunk caching (Java 21 FFM)
- Object pooling for BlockPos, AABB, Vec3d, Vec3f
- Enum $VALUES caching
- MemoryOrchestrator with 4 pressure levels
- Dynamic resolution scaling with temporal reprojection
- Hierarchical-Z occlusion culling
- Shader-aware culling (Iris integration)
- Predictive chunk loading
- Frame budget controller
- Palette + RLE + Zstd chunk compression
- Optional SIMD acceleration
- Fast math lookup tables
- /osmium command suite
