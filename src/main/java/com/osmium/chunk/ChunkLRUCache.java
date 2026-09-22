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
        for (int s = 0; s < 32; s++) {
            Long oldHandle = offHeapHandles.remove(chunkKey * 32 + s);
            if (oldHandle != null) {
                OffHeapCache.free(oldHandle);
            }
        }
    }

    public static synchronized WorldChunk get(long chunkKey) {
        return lruMap.get(chunkKey);
    }

    public static synchronized void remove(long chunkKey) {
        lruMap.remove(chunkKey);
        for (int s = 0; s < 32; s++) {
            Long handle = offHeapHandles.remove(chunkKey * 32 + s);
            if (handle != null) {
                OffHeapCache.free(handle);
            }
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
            ChunkSection[] sections = chunk.getSectionArray();
            if (sections == null || sections.length == 0) return;

            OsmiumConfig cfg = OsmiumConfig.get();
            boolean useSvdag = (cfg != null && cfg.svdagEnabled);

            for (int s = 0; s < sections.length; s++) {
                if (sections[s] == null) continue;
                short[] sectionData = new short[4096];
                int idx = 0;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            var state = sections[s].getBlockState(x, y, z);
                            sectionData[idx++] = (short) Block.getRawIdFromState(state);
                        }
                    }
                }

                long handle = useSvdag ? SvdagStorage.store(sectionData) : -1;
                if (handle == -1) {
                    byte[] compressed = ChunkDataCompressor.compress(sectionData);
                    handle = OffHeapCache.store(compressed);
                }
                if (handle != -1) {
                    offHeapHandles.put(key * 32 + s, handle);
                }
            }
        } catch (Throwable t) {
            // Never let cache eviction crash the game.
            OsmiumConstants.LOGGER.debug("Osmium: onEvict failed for chunk {}", key, t);
        }
    }

    public static short[] loadSection(long chunkKey, int sectionIndex) {
        Long handle = offHeapHandles.get(chunkKey * 32 + sectionIndex);
        if (handle == null) return null;

        OsmiumConfig cfg = OsmiumConfig.get();
        if (cfg != null && cfg.svdagEnabled) {
            short[] svdagData = SvdagStorage.load(handle);
            if (svdagData != null) return svdagData;
        }

        byte[] compressed = OffHeapCache.load(handle);
        if (compressed == null) return null;
        return ChunkDataCompressor.decompress(compressed);
    }

    public static short[][] loadAllSections(long chunkKey) {
        short[][] sections = new short[32][];
        boolean any = false;
        for (int s = 0; s < 32; s++) {
            sections[s] = loadSection(chunkKey, s);
            if (sections[s] != null) any = true;
        }
        return any ? sections : null;
    }

    public static synchronized int size() {
        return lruMap.size();
    }

    public static int capacity() {
        return capacity;
    }
}
