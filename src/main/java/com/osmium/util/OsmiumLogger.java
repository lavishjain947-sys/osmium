package com.osmium.util;

import com.osmium.OsmiumConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OsmiumLogger {
    private static final Map<String, Long> RECENT_MESSAGES = new ConcurrentHashMap<>();
    private static final long DEDUPLICATION_WINDOW_MS = 5000L;

    private OsmiumLogger() {}

    private static boolean shouldLog(String msg) {
        long now = System.currentTimeMillis();
        Long lastTime = RECENT_MESSAGES.put(msg, now);
        if (lastTime == null) return true;
        return (now - lastTime) > DEDUPLICATION_WINDOW_MS;
    }

    public static void info(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        if (shouldLog(msg)) {
            OsmiumConstants.LOGGER.info("[Osmium] {}", msg);
        }
    }

    public static void warn(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        if (shouldLog(msg)) {
            OsmiumConstants.LOGGER.warn("[Osmium] {}", msg);
        }
    }

    public static void error(String format, Object... args) {
        String msg = String.format(format.replace("{}", "%s"), args);
        if (shouldLog(msg)) {
            OsmiumConstants.LOGGER.error("[Osmium] {}", msg);
        }
    }
}
