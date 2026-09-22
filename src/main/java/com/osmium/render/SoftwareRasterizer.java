package com.osmium.render;

import com.osmium.OsmiumConfig;
import com.osmium.OsmiumConstants;

import java.nio.ByteBuffer;

/**
 * Tile-based CPU software rasterizer prototype for GPU-less systems.
 * Uses axis-aligned quad optimizations and early-Z depth rejection
 * to bypass OpenGL pipeline when softwareRenderingEnabled is true.
 */
public final class SoftwareRasterizer {

    private static final int TILE_SIZE = TileBuffer.TILE_SIZE;
    private static TileBuffer currentTile = new TileBuffer();

    private SoftwareRasterizer() {}

    /**
     * Rasterizes an axis-aligned voxel face into the specified tile buffer.
     * Early-Z tests each pixel before computing or writing color.
     */
    public static void rasterizeQuad(TileBuffer tile, float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ, int argbColor) {
        int tileMinX = tile.tileX * TILE_SIZE;
        int tileMinY = tile.tileY * TILE_SIZE;
        int tileMaxX = tileMinX + TILE_SIZE;
        int tileMaxY = tileMinY + TILE_SIZE;

        // Clip quad to tile boundary
        int startX = Math.max(tileMinX, (int) Math.floor(minX));
        int endX = Math.min(tileMaxX, (int) Math.ceil(maxX));
        int startY = Math.max(tileMinY, (int) Math.floor(minY));
        int endY = Math.min(tileMaxY, (int) Math.ceil(maxY));

        if (startX >= endX || startY >= endY) {
            return; // Outside this tile
        }

        float depth = (minZ + maxZ) * 0.5f;

        for (int y = startY; y < endY; y++) {
            int localY = y - tileMinY;
            int rowOffset = localY * TILE_SIZE;

            for (int x = startX; x < endX; x++) {
                int localX = x - tileMinX;
                int pixelIdx = rowOffset + localX;

                // Depth test
                if (depth < tile.depthBuffer[pixelIdx]) {
                    tile.depthBuffer[pixelIdx] = depth;
                    tile.colorBuffer[pixelIdx] = argbColor;
                }
            }
        }
    }

    /**
     * Copies a rendered tile into the full-screen destination ByteBuffer.
     */
    public static void blitTile(TileBuffer tile, ByteBuffer dest, int screenW, int screenH) {
        int startX = tile.tileX * TILE_SIZE;
        int startY = tile.tileY * TILE_SIZE;

        for (int localY = 0; localY < TILE_SIZE; localY++) {
            int globalY = startY + localY;
            if (globalY >= screenH) break;

            int srcOffset = localY * TILE_SIZE;
            int dstOffset = (globalY * screenW + startX) * 4;

            for (int localX = 0; localX < TILE_SIZE; localX++) {
                int globalX = startX + localX;
                if (globalX >= screenW) break;

                int color = tile.colorBuffer[srcOffset + localX];
                dest.putInt(dstOffset + localX * 4, color);
            }
        }
    }

    public static boolean isEnabled() {
        OsmiumConfig cfg = OsmiumConfig.get();
        return cfg != null && cfg.softwareRenderingEnabled;
    }
}
