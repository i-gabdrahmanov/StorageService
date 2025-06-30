package com.storage.storageservice.service.impl;

import net.rubyeye.xmemcached.exception.MemcachedException;

import java.util.concurrent.TimeoutException;

public interface CacheService {

    void set(String key, Object value) throws InterruptedException, TimeoutException, MemcachedException;

    Object get(String key) throws InterruptedException, TimeoutException, MemcachedException;

    void delete(String key) throws InterruptedException, TimeoutException, MemcachedException;

    long increment(String key, long delta) throws InterruptedException, TimeoutException, MemcachedException;
}
