package com.storage.storageservice.service.impl;

import lombok.RequiredArgsConstructor;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

@Service("primaryCacheService")
@RequiredArgsConstructor
public class CacheServicePrimaryImpl implements CacheService{

    private final MemcachedClient primaryCache;
    private static final int DEFAULT_EXPIRATION = 3600;

    public void set(String key, Object value) throws InterruptedException, TimeoutException, MemcachedException {
        primaryCache.set(key, DEFAULT_EXPIRATION, value);
    }

    public Object get(String key) throws InterruptedException, TimeoutException, MemcachedException {
        return primaryCache.get(key);
    }

    public void delete(String key) throws InterruptedException, TimeoutException, MemcachedException {
        primaryCache.delete(key);
    }

    public long increment(String key, long delta) throws InterruptedException, TimeoutException, MemcachedException {
        return primaryCache.incr(key, delta, 0);
    }
}