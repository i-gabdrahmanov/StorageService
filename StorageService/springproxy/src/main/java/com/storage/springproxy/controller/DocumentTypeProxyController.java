package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/documentType")
public class DocumentTypeProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public DocumentTypeProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/new")
    public Mono<Void> addNewDocumentType(@RequestBody Object request) {
        return storageServiceClient.addNewDocumentType(request);
    }

    @GetMapping
    public Mono<Object> getDocumentType(@RequestParam String name) {
        return storageServiceClient.getDocumentType(name);
    }
}