package com.osmium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OsmiumConstants {
    private OsmiumConstants() {}

    public static final String MOD_ID = "osmium";
    public static final String MOD_NAME = "Osmium";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("Osmium");

    public static final int DEFAULT_OFF_HEAP_MB = 256;
    public static final int DEFAULT_CHUNK_CACHE = 256;
    public static final int DEFAULT_TARGET_FPS = 100;
    public static final float DEFAULT_MIN_SCALE = 0.5f;
    public static final int FRAME_HISTORY = 60;
    public static final int MEMORY_POLL_MS = 500;
}
