package com.storage.storageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.storageservice.config.AppParamsConfig;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public final class ZkConfigWatcher {

    private final CuratorFramework client;

    private final ObjectMapper mapper;

    private final AtomicReference<AppParamsConfig> currentConfig;
    private CuratorCache cache;

    public ZkConfigWatcher(CuratorFramework client, ObjectMapper objectMapper,AtomicReference<AppParamsConfig> currentConfig) {
        this.client = client;
        this.mapper = objectMapper;
        this.currentConfig = currentConfig;
    }

    public void startWatching(String path) {
        readInitialConfig(path);

        cache = CuratorCache.build(client, path);
        cache.listenable().addListener(
                CuratorCacheListener.builder()
                        .forChanges((oldNode, newNode) -> updateConfig(new String(newNode.getData())))
                        .build()
        );
        cache.start();
    }

    private void readInitialConfig(String path) {
        try {
            byte[] data = client.getData().forPath(path);
            updateConfig(new String(data));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read initial config", e);
        }
    }

    private void updateConfig(String jsonConfig) {
        AppParamsConfig config;
        try {
            config = mapper.readValue(jsonConfig, AppParamsConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse config", e);
        }
        currentConfig.set(config);
    }


    public AppParamsConfig getAppParamsConfig() {
        return currentConfig.get();
    }

    public void stopWatching() {
        if (cache != null) {
            cache.close();
        }
    }

    @PreDestroy
    public void destroy() {
        stopWatching();
    }
}

