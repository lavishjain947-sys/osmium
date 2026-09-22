package com.osmium.pool;

import com.osmium.OsmiumConstants;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public final class EnumValuesCache {
    private static final ConcurrentHashMap<Class<?>, Object[]> CACHE = new ConcurrentHashMap<>();
    private static boolean warned = false;

    private EnumValuesCache() {}

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E[] get(Class<E> cls) {
        if (!warned) {
            warned = true;
            OsmiumConstants.LOGGER.warn("[Osmium] EnumValuesCache active. Callers must not modify returned array.");
        }

        return (E[]) CACHE.computeIfAbsent(cls, c -> {
            try {
                Field f = c.getDeclaredField("$VALUES");
                f.setAccessible(true);
                return (Object[]) f.get(null);
            } catch (Throwable t) {
                // Fallback to standard getEnumConstants if reflection is guarded
                return c.getEnumConstants();
            }
        });
    }

    public static int cachedClassesCount() {
        return CACHE.size();
    }
}
