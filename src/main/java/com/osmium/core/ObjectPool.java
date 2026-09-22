package com.osmium.core;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ObjectPool<T> {
    private static final List<ObjectPool<?>> ALL_POOLS = new CopyOnWriteArrayList<>();

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final int maxSize;
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();

    public ObjectPool(Supplier<T> f, Consumer<T> r, int max) {
        this.factory = f;
        this.resetter = r;
        this.maxSize = max;
        ALL_POOLS.add(this);
    }

    public T acquire() {
        T obj = queue.poll();
        if (obj != null) {
            hits.increment();
            return obj;
        }
        misses.increment();
        return factory.get();
    }

    public void release(T obj) {
        if (obj == null) return;
        if (queue.size() < maxSize) {
            if (resetter != null) {
                resetter.accept(obj);
            }
            queue.offer(obj);
        }
    }

    public void shrink(double factor) {
        int targetSize = (int) (queue.size() * factor);
        while (queue.size() > targetSize && !queue.isEmpty()) {
            queue.poll();
        }
    }

    public static void shrinkAll(double factor) {
        for (ObjectPool<?> pool : ALL_POOLS) {
            pool.shrink(factor);
        }
    }

    public int size() {
        return queue.size();
    }

    public long hits() {
        return hits.sum();
    }

    public long misses() {
        return misses.sum();
    }
}
