# Changelog

All notable changes to this project will be documented in this file.

## 1.0.1 — Critical Bug Fix Patch
- **ClientChunkManagerMixin**: Fixed packet chunk load method signature and parameters for 1.21.11.
- **EntityRenderDispatcherMixin**: Combined projection matrix with model-view camera matrix for accurate occlusion culling.
- **HierarchicalZCuller**: Replaced synchronous `glReadPixels` with asynchronous Pixel Buffer Objects (PBO) and sync fences, preventing GPU pipeline stalls.
- **GameRendererMixin**: Added active viewport scaling and restoration hooks during 3D world rendering pass.
- **OsmiumConfig**: Added thread-safe double-checked locking static `get()` singleton accessor.
- **EnumValuesCache**: Migrated reflection to Java 21 `MethodHandles.Lookup` and `VarHandle` with fallback.
- **ChunkDataCompressor**: Fixed raw tag handling and buffer wrapping logic in `decompress()`.
- **MinecraftClientMixin**: Hooked `PredictiveChunkLoader` into `tick` at `TAIL` for per-tick motion vector calculation.
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
