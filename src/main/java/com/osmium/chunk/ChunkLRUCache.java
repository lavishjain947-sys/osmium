package com.osmium.chunk;

import com.osmium.OsmiumConstants;
import com.osmium.core.OffHeapCache;
import net.minecraft.block.Block;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkLRUCache {
    private static int capacity = 256;
    private static final Map<Long, Long> offHeapHandles = new ConcurrentHashMap<>();

    private static final Map<Long, WorldChunk> lruMap = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, WorldChunk> eldest) {
            if (size() > capacity) {
                onEvict(eldest.getKey(), eldest.getValue());
                return true;
            }
            return false;
        }
    };

    private ChunkLRUCache() {}

    public static synchronized void init(int maxSize) {
        capacity = Math.max(16, maxSize);
        trimToSize(capacity);
    }

    public static synchronized void put(long chunkKey, WorldChunk chunk) {
        if (chunk == null) return;
        lruMap.put(chunkKey, chunk);
        Long oldHandle = offHeapHandles.remove(chunkKey);
        if (oldHandle != null) {
            OffHeapCache.free(oldHandle);
        }
    }

    public static synchronized WorldChunk get(long chunkKey) {
        return lruMap.get(chunkKey);
    }

    public static synchronized void remove(long chunkKey) {
        lruMap.remove(chunkKey);
        Long handle = offHeapHandles.remove(chunkKey);
        if (handle != null) {
            OffHeapCache.free(handle);
        }
    }

    public static synchronized void trim(double factor) {
        int target = Math.max(16, (int) (lruMap.size() * factor));
        trimToSize(target);
    }

    private static void trimToSize(int target) {
        Iterator<Map.Entry<Long, WorldChunk>> it = lruMap.entrySet().iterator();
        while (lruMap.size() > target && it.hasNext()) {
            Map.Entry<Long, WorldChunk> entry = it.next();
            onEvict(entry.getKey(), entry.getValue());
            it.remove();
        }
    }

    private static void onEvict(long key, WorldChunk chunk) {
        try {
            // Extract real block data from the chunk's sections.
            // In 1.21.11, each ChunkSection is a 16x16x16 block volume.
            // We compress the first section's palette data as a proof-of-concept.
            // A full implementation would compress all sections.
            ChunkSection[] sections = chunk.getSectionArray();
            if (sections.length == 0) return;

            // Flatten the first section into a short[4096] for compression.
            // Uses the section's block state container.
            short[] sectionData = new short[4096];
            int idx = 0;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var state = sections[0].getBlockState(x, y, z);
                        sectionData[idx++] = (short) Block.getRawIdFromState(state);
                    }
                }
            }

            byte[] compressed = ChunkDataCompressor.compress(sectionData);
            long handle = OffHeapCache.store(compressed);
            if (handle != -1) {
                offHeapHandles.put(key, handle);
            }
        } catch (Throwable t) {
            // Never let cache eviction crash the game.
            OsmiumConstants.LOGGER.debug("Osmium: onEvict failed for chunk {}", key, t);
        }
    }

    public static synchronized int size() {
        return lruMap.size();
    }

    public static int capacity() {
        return capacity;
    }
}
