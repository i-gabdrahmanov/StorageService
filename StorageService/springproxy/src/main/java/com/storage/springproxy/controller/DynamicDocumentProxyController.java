package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/dd")
public class DynamicDocumentProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public DynamicDocumentProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/new")
    public Mono<Void> addNewDynamicDocument(@RequestBody Object request) {
        return storageServiceClient.addNewDynamicDocument(request);
    }
}