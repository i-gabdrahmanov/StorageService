package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/propertyType")
public class PropertyTypeProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public PropertyTypeProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/new")
    public Mono<Void> addNewPropertyType(@RequestBody Object request) {
        return storageServiceClient.addNewPropertyType(request);
    }

    @PostMapping("/search")
    public Mono<Object> getPropertyTypeByNameAndDocType(@RequestParam String name, @RequestBody Object request) {
        return storageServiceClient.getPropertyTypeByNameAndDocType(name, request);
    }
}