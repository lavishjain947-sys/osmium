# Changelog

All notable changes to this project will be documented in this file.

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
