package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/zk")
public class ZkProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public ZkProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @GetMapping
    public Mono<String> getZkConfig() {
        return storageServiceClient.getZkConfig();
    }
}