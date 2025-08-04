package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/insurance")
public class InsuranceProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public InsuranceProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/new")
    public Mono<Void> addNewInsurance(@RequestBody Object dto) {
        return storageServiceClient.addNewInsurance(dto);
    }
}