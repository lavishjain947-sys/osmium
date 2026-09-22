package com.osmium.core;

import com.osmium.OsmiumConfig;
import com.osmium.OsmiumConstants;
import com.osmium.chunk.ChunkLRUCache;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

public final class MemoryOrchestrator implements Runnable {
    public enum PressureLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private static volatile boolean running = false;
    private static Thread daemonThread;

    private static volatile PressureLevel currentLevel = PressureLevel.LOW;
    private static volatile long heapUsedMb = 0;
    private static volatile long heapMaxMb = 0;
    private static volatile long gcPauseTotalMs = 0;
    private static volatile long gcCollectionCount = 0;
    private static volatile long allocationRateMbPerSec = 0;

    private static long lastHeapUsedBytes = 0;
    private static long lastPollTimeNanos = 0;
    private static long lastCriticalWarnTime = 0;

    private MemoryOrchestrator() {}

    public static synchronized void start(OsmiumConfig cfg) {
        if (running) return;
        running = true;
        daemonThread = new Thread(new MemoryOrchestrator(), "Osmium-MemoryOrchestrator");
        daemonThread.setDaemon(true);
        daemonThread.setPriority(Thread.NORM_PRIORITY - 1);
        daemonThread.start();
        OsmiumConstants.LOGGER.info("[Osmium] Memory Orchestrator daemon started.");
    }

    public static synchronized void stop() {
        running = false;
        if (daemonThread != null) {
            daemonThread.interrupt();
            daemonThread = null;
        }
    }

    public static synchronized void shutdown() {
        stop();
    }

    @Override
    public void run() {
        while (running) {
            try {
                poll();
                Thread.sleep(OsmiumConstants.MEMORY_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // Prevent daemon from terminating on unexpected error
            }
        }
    }

    private static void poll() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        long used = heapUsage.getUsed();
        long max = heapUsage.getMax() > 0 ? heapUsage.getMax() : Runtime.getRuntime().totalMemory();

        heapUsedMb = used / (1024L * 1024L);
        heapMaxMb = max / (1024L * 1024L);

        long now = System.nanoTime();
        if (lastPollTimeNanos > 0) {
            long timeDeltaNanos = now - lastPollTimeNanos;
            long bytesDelta = used - lastHeapUsedBytes;
            if (bytesDelta > 0 && timeDeltaNanos > 0) {
                double sec = timeDeltaNanos / 1_000_000_000.0;
                allocationRateMbPerSec = (long) ((bytesDelta / (1024.0 * 1024.0)) / sec);
            }
        }
        lastPollTimeNanos = now;
        lastHeapUsedBytes = used;

        // GC metrics
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalGcTime = 0;
        long totalGcCount = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long time = gc.getCollectionTime();
            long count = gc.getCollectionCount();
            if (time > 0) totalGcTime += time;
            if (count > 0) totalGcCount += count;
        }
        gcPauseTotalMs = totalGcTime;
        gcCollectionCount = totalGcCount;

        // Calculate pressure level
        double heapRatio = max > 0 ? (double) used / (double) max : 0.5;

        if (heapRatio < 0.60) {
            currentLevel = PressureLevel.LOW;
        } else if (heapRatio < 0.75) {
            currentLevel = PressureLevel.MEDIUM;
        } else if (heapRatio < 0.90) {
            currentLevel = PressureLevel.HIGH;
            ChunkLRUCache.trim(0.5);
            ObjectPool.shrinkAll(0.5);
        } else {
            currentLevel = PressureLevel.CRITICAL;
            OffHeapCache.evictColdest(50);
            ChunkLRUCache.trim(0.25);
            ObjectPool.shrinkAll(0.25);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCriticalWarnTime > 30000L) {
                lastCriticalWarnTime = currentTime;
                OsmiumConstants.LOGGER.warn("[Osmium] Critical memory pressure detected: {} MB / {} MB used ({:.1f}%). Evicting caches.",
                        heapUsedMb, heapMaxMb, heapRatio * 100.0);
            }
        }
    }

    public static PressureLevel getPressureLevel() {
        return currentLevel;
    }

    public static long getHeapUsedMb() {
        return heapUsedMb;
    }

    public static long getHeapMaxMb() {
        return heapMaxMb;
    }

    public static long getGcPauseTotalMs() {
        return gcPauseTotalMs;
    }

    public static long getGcCollectionCount() {
        return gcCollectionCount;
    }

    public static long getAllocationRateMbPerSec() {
        return allocationRateMbPerSec;
    }
}
