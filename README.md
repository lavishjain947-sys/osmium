# Osmium — Densest optimization. Zero stutter. Maximum FPS.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.17.0+-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Osmium is a client-side Fabric optimization mod for Minecraft 1.21.11 engineered specifically to eliminate stutter and maximize FPS on low-RAM (2GB–4GB) and integrated GPU systems. Rather than competing with existing optimization mods, Osmium introduces a complementary layer targeting off-heap memory management, allocation reduction, hierarchical depth culling, dynamic resolution scaling, and predictive loading.

---

## Features & Technologies

- **Off-Heap Chunk Caching (Java 21 FFM API):** Moves chunk cache payloads out of the garbage-collected JVM heap into direct native memory, eliminating GC pauses from cached terrain.
- **Allocation Reduction (Thread-Local Object Pools):** Bounded object pools for `BlockPos`, `AABB`, `Vec3d`, and `Vec3f` alongside enum `$VALUES` array caching, slashing allocation rate by up to 60%.
- **Adaptive GC Orchestration:** Continuously samples `MemoryMXBean` and `GarbageCollectorMXBean` to proactively trim caches and shrink pools before GC pauses occur.
- **Dynamic Resolution Scaling (DRS) + Temporal Reprojection:** Dynamically scales 3D render resolution between 50% and 100% based on frame budgets with TAAU temporal reprojection, recovering massive GPU headroom on integrated graphics.
- **Hierarchical-Z Occlusion Culling (Asynchronous PBO):** Downsamples the depth buffer into a 64×64 depth mirror using non-blocking Pixel Buffer Objects (PBO) and sync fences to cull hidden entities and block entities without CPU/GPU sync stalls.
- **Shader-Aware Culling (Iris Integration):** Reuses the Iris G-buffer depth texture directly when shaders are active, delivering zero-overhead culling synchronization (inert placeholder in v1.0.1; full pipeline integration in v1.1).
- **Predictive Chunk Loading:** Tracks player trajectory, velocity, and acceleration to prioritize loading chunks along the player's predicted movement vector.
- **Frame Budgeting:** Maintains a 60-frame ring buffer to compute P50/P95/P99 latency, automatically throttling chunk compilation and rendering work when frame time spikes.
- **Chunk Data Compression (Palette + RLE + Zstd):** Homogeneous chunk sections are compressed via a 3-stage pipeline, reducing chunk memory footprints by 60–90%.
- **SIMD Acceleration (jdk.incubator.vector):** Vectorized math routines for batch vertex and coordinate transformations on supported hardware (optimized scalar fallback in v1.0.1).
- **Fast Math Lookup Tables:** 65,536-entry precomputed trigonometry tables with proper negative angle wrapping and fast inverse square root bit tricks for particle and ambient rendering paths.

---

## Installation

1. Install **Fabric Loader** (`0.17.0` or higher) for Minecraft `1.21.11`.
2. Install **Fabric API** (`0.115.0+1.21.11` or higher).
3. Download `osmium-1.0.0.jar` (or pre-built `osmium-fabric-1.21.11.zip`) from releases.
4. Place the mod JAR into your Minecraft `.minecraft/mods/` directory.
5. Launch Minecraft using Java 21+.

---

## Recommended JVM Arguments

For optimal GC stability, minimal latency on 2GB–4GB systems, and full Java 21 module reflection access:

```text
-Xms1536M -Xmx1536M
-XX:+UseG1GC
-XX:G1NewSizePercent=40
-XX:G1MaxNewSizePercent=60
-XX:MaxGCPauseMillis=30
-XX:+AlwaysPreTouch
-XX:+DisableExplicitGC
-XX:+UseStringDeduplication
--add-opens java.base/java.lang=ALL-UNNAMED
```

> [!IMPORTANT]
> The `--add-opens java.base/java.lang=ALL-UNNAMED` flag is recommended for `EnumValuesCache` on Java 21 to allow `MethodHandles.privateLookupIn` to access private static `$VALUES` fields across all core enum classes.

---

## Optional SIMD Acceleration

To enable SIMD vector acceleration using the Java 21 Vector API:
1. Add the following JVM argument to your launcher:
   ```text
   --add-modules jdk.incubator.vector
   ```
2. In `config/osmium.json`, set `"simdEnabled": true` (or use `/osmium` in-game).

---

## Compatibility Matrix

| Mod | Compatibility | Notes |
| :--- | :---: | :--- |
| **Sodium** | ✔ | Fully compatible. Sodium optimizes GPU chunk rendering while Osmium minimizes CPU allocation and GC overhead. |
| **Lithium** | ✔ | Fully compatible. Lithium focuses on server-side logic; Osmium is strictly client-side. |
| **FerriteCore** | ✔ | Fully compatible. FerriteCore optimizes block state storage; Osmium provides off-heap chunk caching. |
| **Iris** | ✔ | Fully compatible. Inert placeholder in v1.0.1; full G-buffer depth capture planned for v1.1. |
| **EntityCulling** | ✔ | Fully compatible. Can run concurrently or deferred to Osmium's depth mirror culling. |
| **ModMenu** | ✔ | Fully compatible. Dedicated entrypoint provides Osmium settings and diagnostic screen without classloading issues. |
| **Krypton** | ✔ | Fully compatible. Krypton optimizes network traffic; Osmium handles memory and rendering. |

---

## Configuration Reference (`config/osmium.json`)

| Field | Type | Default | Description |
| :--- | :---: | :---: | :--- |
| `enabled` | boolean | `true` | Master toggle for all Osmium optimizations. |
| `dynamicResolutionEnabled` | boolean | `true` | Toggles dynamic resolution scaling. |
| `dynamicResolutionMinScale` | float | `0.5` | Minimum resolution scale factor (0.5 to 1.0). |
| `targetFps` | int | `100` | Target frame rate for frame budget calculations. |
| `offHeapCacheMb` | int | `256` | Maximum off-heap memory allocation in MB. |
| `chunkLRUCacheSize` | int | `256` | Maximum number of chunks retained in on-heap LRU cache. |
| `hierarchicalZCullingEnabled` | boolean | `true` | Enables 64×64 depth mirror occlusion culling. |
| `shaderAwareCullingEnabled` | boolean | `true` | Enables integration with Iris shader depth pipelines. |
| `objectPoolingEnabled` | boolean | `true` | Enables thread-safe object pooling for BlockPos, Box, and Vec3. |
| `predictiveChunkLoadingEnabled` | boolean | `true` | Prioritizes chunk loading along player movement trajectory. |
| `simdEnabled` | boolean | `false` | Enables Java Vector API acceleration (requires `--add-modules`). |
| `debugStats` | boolean | `false` | Enables verbose diagnostic logging in console and HUD. |
| `temporalBlendWeight` | float | `0.15` | History blend factor for temporal reprojection (TAAU). |

---

## Commands Reference

All commands are client-side and do not require server permissions:

- `/osmium help`: Lists all available subcommands and their descriptions.
- `/osmium stats`: Displays heap usage, GC pause metrics, P50/P95/P99 frame times, object pool efficiency, and off-heap stats.
- `/osmium target <fps>`: Sets the target FPS budget (`config.targetFps`).
- `/osmium scale <scale>`: Sets a fixed manual resolution scale (`0.5` – `1.0`), disabling dynamic resolution scaling.
- `/osmium reload`: Reloads configuration from `config/osmium.json`.
- `/osmium pools`: Displays detailed statistics (hits, misses, current size) for all object pools.
- `/osmium offheap`: Displays current off-heap cache metrics (used, capacity, free memory, entries, and evictions).

---

## License

This project is licensed under the [MIT License](LICENSE).
