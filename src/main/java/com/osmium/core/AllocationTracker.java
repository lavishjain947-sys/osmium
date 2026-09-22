package com.osmium.core;

import com.osmium.OsmiumConstants;
import com.osmium.util.MemoryMath;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class AllocationTracker {
    private static final Map<String, AtomicLong> ALLOCATIONS = new ConcurrentHashMap<>();

    private AllocationTracker() {}

    public static void record(String site, long bytes) {
        ALLOCATIONS.computeIfAbsent(site, k -> new AtomicLong()).addAndGet(bytes);
    }

    public static void dumpTop(int n) {
        OsmiumConstants.LOGGER.info("=== Top {} Allocation Sites ===", n);
        ALLOCATIONS.entrySet().stream()
            .sorted(Comparator.comparingLong((Map.Entry<String, AtomicLong> e) -> e.getValue().get()).reversed())
            .limit(n)
            .forEach(e -> OsmiumConstants.LOGGER.info("  {} -> {}", e.getKey(), MemoryMath.humanReadable(e.getValue().get())));
    }

    public static void reset() {
        ALLOCATIONS.clear();
    }
}
