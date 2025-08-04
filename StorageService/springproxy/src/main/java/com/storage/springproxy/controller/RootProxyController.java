package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class RootProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public RootProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @GetMapping("/")
    public Mono<Void> root() {
        return storageServiceClient.root();
    }
}