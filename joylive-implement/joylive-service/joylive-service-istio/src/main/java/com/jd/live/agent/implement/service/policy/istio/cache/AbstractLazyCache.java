package com.jd.live.agent.implement.service.policy.istio.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLazyCache<K, V> {

    private static final int STATUS_NOT_LOADED = 0;
    private static final int STATUS_LOADING = 1;
    private static final int STATUS_LOADED = 2;

    private CountDownLatch initLatch = new CountDownLatch(1);
    private final AtomicInteger cacheStatus = new AtomicInteger(0); // 0: not loaded, 1: loading, 2: loaded
    protected Map<K, V> cache = Collections.emptyMap();

    public V get(K key) {
        if (cacheStatus.get() != STATUS_LOADED) {
            if (cacheStatus.compareAndSet(STATUS_NOT_LOADED, STATUS_LOADING)) {
                loadResourceAsync();
            }
            try {
                initLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return cache.get(key);
    }

    public boolean exists(K key) {
        return get(key) != null;
    }

    protected abstract void loadResourceAsync();

    protected final void onResourceReady(Map<K, V> resource) {
        if (resource != null && !resource.isEmpty()) {
            this.cache = resource;
        }
        if (cacheStatus.compareAndSet(STATUS_LOADING, STATUS_LOADED)) {
            initLatch.countDown();
        }
    }

    protected void onResourceError(Throwable e) {
        cacheStatus.compareAndSet(STATUS_LOADING, STATUS_NOT_LOADED);
        initLatch.countDown();
        initLatch = new CountDownLatch(1);
    }
}
