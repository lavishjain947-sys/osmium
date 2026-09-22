package com.osmium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class OsmiumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("osmium.json").toFile();
    private static volatile OsmiumConfig INSTANCE;

    public boolean enabled = true;
    public boolean dynamicResolutionEnabled = true;
    public float dynamicResolutionMinScale = OsmiumConstants.DEFAULT_MIN_SCALE;
    public int targetFps = OsmiumConstants.DEFAULT_TARGET_FPS;
    public int offHeapCacheMb = OsmiumConstants.DEFAULT_OFF_HEAP_MB;
    public int chunkLRUCacheSize = OsmiumConstants.DEFAULT_CHUNK_CACHE;
    public boolean hierarchicalZCullingEnabled = true;
    public boolean shaderAwareCullingEnabled = true;
    public boolean objectPoolingEnabled = true;
    public boolean predictiveChunkLoadingEnabled = true;
    public boolean simdEnabled = false;
    public boolean debugStats = false;
    public float temporalBlendWeight = 0.15f;

    /** Static singleton accessor used across the mod. */
    public static OsmiumConfig get() {
        OsmiumConfig local = INSTANCE;
        if (local == null) {
            synchronized (OsmiumConfig.class) {
                local = INSTANCE;
                if (local == null) {
                    local = load();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    public static OsmiumConfig load() {
        OsmiumConfig loaded = null;
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                loaded = GSON.fromJson(reader, OsmiumConfig.class);
            } catch (IOException e) {
                OsmiumConstants.LOGGER.error("[Osmium] Failed to read configuration file: {}", e.getMessage());
            }
        }
        if (loaded == null) {
            loaded = new OsmiumConfig();
            loaded.save();
        }
        INSTANCE = loaded;
        return loaded;
    }

    public void save() {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            OsmiumConstants.LOGGER.error("[Osmium] Failed to write configuration file: {}", e.getMessage());
        }
    }

    public void onChanged() {
        save();
    }
}
