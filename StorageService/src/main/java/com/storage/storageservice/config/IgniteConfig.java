package com.storage.storageservice.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;


@Configuration
public class IgniteConfig {

    @Bean
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Жесткие настройки (переопределяют любые другие конфигурации)
        cfg.setIgniteInstanceName("spring-ignite-cluster");
        cfg.setPeerClassLoadingEnabled(true);

        // Настройка discovery
        TcpDiscoverySpi disco = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Collections.singletonList("ignite:47500"));
        disco.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(disco);

        // Логирование конфигурации
        System.out.println("=== IGNITE CONFIGURATION ===");
        System.out.println("PeerClassLoading: " + cfg.isPeerClassLoadingEnabled());
        System.out.println("Discovery addresses: " + ipFinder.getRegisteredAddresses());

        return Ignition.start(cfg);
    }


    @Bean
    public IgniteCache<Integer, String> igniteCache(Ignite ignite) {
        return ignite.getOrCreateCache("myCache");
    }
}