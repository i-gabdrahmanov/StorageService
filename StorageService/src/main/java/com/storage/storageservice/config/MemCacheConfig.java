package com.storage.storageservice.config;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MemCacheConfig {

    @Bean(name = "primaryCache")
    public MemcachedClient primaryCacheClient() throws IOException {
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(
                AddrUtil.getAddresses("memcached:11211"));
        builder.setCommandFactory(new BinaryCommandFactory());
        return builder.build();
    }
}
