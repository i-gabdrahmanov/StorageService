package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/primaryCache")
public class PrimaryCacheProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public PrimaryCacheProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @GetMapping
    public Mono<String> getPrimaryCacheValue(@RequestParam String key) {
        return storageServiceClient.getPrimaryCacheValue(key);
    }

    @PostMapping("/{key}/{value}/add")
    public Mono<Void> addToPrimaryCache(@PathVariable String key, @PathVariable String value) {
        return storageServiceClient.addToPrimaryCache(key, value);
    }
}