package com.storage.storageservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.storageservice.service.ZkConfigWatcher;
import jakarta.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class ZkConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                "zookeeper:2181",
                new ExponentialBackoffRetry(1000, 3)
        );
        client.start();
        return client;
    }

    @Bean
    public ZkConfigWatcher zkJsonConfigWatcher(CuratorFramework client, AtomicReference<AppParamsConfig> currentAppConfig) {
        ZkConfigWatcher watcher = new ZkConfigWatcher(client, objectMapper, currentAppConfig);
        watcher.startWatching("storageservice/app");
        return watcher;
    }

     @Bean
     public AtomicReference<AppParamsConfig> currentAppConfig() {
        return new AtomicReference<>();
     }

    @PreDestroy
    public void destroy() {
        curatorFramework().close();
    }
}
