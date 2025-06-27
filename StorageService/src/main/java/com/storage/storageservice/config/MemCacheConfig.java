package com.storage.storageservice.config;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MemCacheConfig {

    @Bean
    public MemcachedClient memcachedClient() throws IOException {
        return new XMemcachedClientBuilder("memcached:11211").build();
    }
}
