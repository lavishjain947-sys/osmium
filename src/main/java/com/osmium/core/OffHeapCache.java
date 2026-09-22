package com.osmium.core;

import com.osmium.OsmiumConstants;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class OffHeapCache {
    private static boolean FALLBACK_MODE = false;
    private static Arena arena;
    private static MemorySegment slab;
    private static long capacity;
    private static long bumpPointer = 0;
    private static final Object ALLOC_LOCK = new Object();

    public static final class FreeBlock {
        long offset;
        long size;

        FreeBlock(long offset, long size) {
            this.offset = offset;
            this.size = size;
        }
    }

    public static final class EntryMeta {
        final long offset;
        final int size;

        EntryMeta(long offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }

    private static final List<FreeBlock> freeList = new ArrayList<>();
    private static final Map<Long, EntryMeta> lruIndex = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<Long, byte[]> fallbackMap = new ConcurrentHashMap<>();
    private static final AtomicLong handleCounter = new AtomicLong(1);
    private static final AtomicLong usedMemory = new AtomicLong(0);
    private static final AtomicLong totalEvictions = new AtomicLong(0);

    private OffHeapCache() {}

    public static void init(int mb) {
        synchronized (ALLOC_LOCK) {
            long bytes = (long) mb * 1024L * 1024L;
            capacity = bytes;
            bumpPointer = 0;
            freeList.clear();
            lruIndex.clear();
            fallbackMap.clear();

            try {
                arena = Arena.ofShared();
                slab = arena.allocate(capacity);
                FALLBACK_MODE = false;
                OsmiumConstants.LOGGER.info("[Osmium] Off-heap cache initialized with {} MB using Java 21 FFM API.", mb);
            } catch (Throwable t) {
                FALLBACK_MODE = true;
                arena = null;
                slab = null;
                OsmiumConstants.LOGGER.warn("[Osmium] FFM API off-heap allocation failed ({}). Falling back to on-heap cache.", t.getMessage());
            }
        }
    }

    public static long store(byte[] data) {
        if (data == null || data.length == 0) return -1;

        if (FALLBACK_MODE) {
            long handle = handleCounter.getAndIncrement();
            fallbackMap.put(handle, data);
            usedMemory.addAndGet(data.length);
            return handle;
        }

        synchronized (ALLOC_LOCK) {
            int needed = data.length;
            long offset = allocateBlock(needed);

            if (offset == -1) {
                // Slab full, evict cold entries until enough space exists
                while (offset == -1 && !lruIndex.isEmpty()) {
                    evictColdest(1);
                    offset = allocateBlock(needed);
                }
            }

            if (offset == -1) {
                // Cannot allocate even after evicting all entries
                return -1;
            }

            MemorySegment srcSeg = MemorySegment.ofArray(data);
            MemorySegment.copy(srcSeg, 0, slab, offset, needed);

            long handle = handleCounter.getAndIncrement();
            lruIndex.put(handle, new EntryMeta(offset, needed));
            usedMemory.addAndGet(needed);
            return handle;
        }
    }

    private static long allocateBlock(int size) {
        // 1. Search free-list (first-fit)
        for (int i = 0; i < freeList.size(); i++) {
            FreeBlock block = freeList.get(i);
            if (block.size >= size) {
                long offset = block.offset;
                if (block.size == size) {
                    freeList.remove(i);
                } else {
                    block.offset += size;
                    block.size -= size;
                }
                return offset;
            }
        }

        // 2. Bump pointer allocation
        if (bumpPointer + size <= capacity) {
            long offset = bumpPointer;
            bumpPointer += size;
            return offset;
        }

        return -1;
    }

    public static byte[] load(long handle) {
        if (FALLBACK_MODE) {
            return fallbackMap.get(handle);
        }

        synchronized (ALLOC_LOCK) {
            EntryMeta meta = lruIndex.get(handle);
            if (meta == null) return null;

            byte[] out = new byte[meta.size];
            MemorySegment dstSeg = MemorySegment.ofArray(out);
            MemorySegment.copy(slab, meta.offset, dstSeg, 0, meta.size);
            return out;
        }
    }

    public static void free(long handle) {
        if (FALLBACK_MODE) {
            byte[] removed = fallbackMap.remove(handle);
            if (removed != null) {
                usedMemory.addAndGet(-removed.length);
            }
            return;
        }

        synchronized (ALLOC_LOCK) {
            EntryMeta meta = lruIndex.remove(handle);
            if (meta != null) {
                usedMemory.addAndGet(-meta.size);
                releaseBlock(meta.offset, meta.size);
            }
        }
    }

    private static void releaseBlock(long offset, long size) {
        FreeBlock newBlock = new FreeBlock(offset, size);
        freeList.add(newBlock);
        coalesceFreeList();
    }

    private static void coalesceFreeList() {
        if (freeList.size() <= 1) return;

        freeList.sort(Comparator.comparingLong(b -> b.offset));
        List<FreeBlock> merged = new ArrayList<>();
        FreeBlock current = freeList.get(0);

        for (int i = 1; i < freeList.size(); i++) {
            FreeBlock next = freeList.get(i);
            if (current.offset + current.size == next.offset) {
                current.size += next.size;
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        freeList.clear();
        freeList.addAll(merged);
    }

    public static void evictColdest(int n) {
        if (FALLBACK_MODE) {
            int count = 0;
            Iterator<Long> it = fallbackMap.keySet().iterator();
            while (it.hasNext() && count < n) {
                byte[] val = fallbackMap.remove(it.next());
                if (val != null) {
                    usedMemory.addAndGet(-val.length);
                    totalEvictions.incrementAndGet();
                    count++;
                }
            }
            return;
        }

        synchronized (ALLOC_LOCK) {
            int count = 0;
            Iterator<Map.Entry<Long, EntryMeta>> it = lruIndex.entrySet().iterator();
            while (it.hasNext() && count < n) {
                Map.Entry<Long, EntryMeta> entry = it.next();
                it.remove();
                EntryMeta meta = entry.getValue();
                usedMemory.addAndGet(-meta.size);
                releaseBlock(meta.offset, meta.size);
                totalEvictions.incrementAndGet();
                count++;
            }
        }
    }

    public static long usedBytes() {
        return usedMemory.get();
    }

    public static long capacityBytes() {
        return capacity;
    }

    public static int entryCount() {
        return FALLBACK_MODE ? fallbackMap.size() : lruIndex.size();
    }

    public static long evictions() {
        return totalEvictions.get();
    }

    public static boolean isFallbackMode() {
        return FALLBACK_MODE;
    }
}
