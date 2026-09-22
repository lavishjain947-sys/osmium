package com.osmium.render;

import java.util.Arrays;

/**
 * 32x32 pixel tile buffer for CPU tile-based rasterization on GPU-less systems.
 * Provides independent color and depth buffers for cache-friendly rasterization.
 */
public final class TileBuffer {

    public static final int TILE_SIZE = 32;
    public static final int PIXEL_COUNT = TILE_SIZE * TILE_SIZE;

    public final int[] colorBuffer = new int[PIXEL_COUNT];
    public final float[] depthBuffer = new float[PIXEL_COUNT];

    public int tileX;
    public int tileY;

    public TileBuffer() {
        this(0, 0);
    }

    public TileBuffer(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
        clear(0xFF000000, 1.0f);
    }

    public void setTileCoords(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public void clear(int clearColor, float clearDepth) {
        Arrays.fill(colorBuffer, clearColor);
        Arrays.fill(depthBuffer, clearDepth);
    }

    public boolean isInside(int px, int py) {
        int localX = px - tileX * TILE_SIZE;
        int localY = py - tileY * TILE_SIZE;
        return localX >= 0 && localX < TILE_SIZE && localY >= 0 && localY < TILE_SIZE;
    }

    public int getPixelIndex(int localX, int localY) {
        return localY * TILE_SIZE + localX;
    }
}
