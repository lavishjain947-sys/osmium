package com.osmium.pool;

import com.osmium.OsmiumConstants;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;

public final class EnumValuesCache {

    private static final ConcurrentHashMap<Class<?>, Object[]> CACHE =
            new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private EnumValuesCache() {}

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E[] get(Class<E> cls) {
        return (E[]) CACHE.computeIfAbsent(cls, c -> {
            try {
                MethodHandles.Lookup privateLookup =
                        MethodHandles.privateLookupIn(c, LOOKUP);
                VarHandle handle = privateLookup.findStaticVarHandle(
                        c, "$VALUES", Enum[].class);
                return (Object[]) handle.get();
            } catch (Throwable t) {
                OsmiumConstants.LOGGER.debug(
                        "EnumValuesCache fallback for {}", c.getName());
                return c.getEnumConstants();
            }
        });
    }

    public static int cachedClassesCount() {
        return CACHE.size();
    }
}
