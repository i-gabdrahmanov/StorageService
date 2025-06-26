package com.storage.storageservice.config;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.Tuple;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching
public class IgniteConfig {

    @Bean
    public IgniteClient igniteClient() {
        return IgniteClient.builder()
                .addresses("ignite:10800")
                .connectTimeout(5000)
                .build();
    }

    @Bean
    public CacheManager cacheManager(IgniteClient igniteClient) {
        return new IgniteSpringCacheManager(igniteClient);
    }

    static class IgniteSpringCacheManager implements CacheManager {
        private final IgniteClient igniteClient;
        private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

        public IgniteSpringCacheManager(IgniteClient igniteClient) {
            this.igniteClient = igniteClient;
        }

        @Override
        public Cache getCache(String name) {
            return cacheMap.computeIfAbsent(name,
                    n -> new IgniteSpringCache(igniteClient, n));
        }

        @Override
        public Collection<String> getCacheNames() {
            return Collections.unmodifiableSet(cacheMap.keySet());
        }
    }

    static class IgniteSpringCache implements Cache {
        private final IgniteClient igniteClient;
        private final String name;

        public IgniteSpringCache(IgniteClient igniteClient, String name) {
            this.igniteClient = igniteClient;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return igniteClient;
        }

        @Override
        public ValueWrapper get(Object key) {
            return () -> igniteClient.tables().table(name).recordView().get(null, (Tuple) key);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return null;
        }

        @Override
        public void put(Object key, Object value) {

        }

        @Override
        public void evict(Object key) {

        }

        @Override
        public void clear() {

        }
    }
}