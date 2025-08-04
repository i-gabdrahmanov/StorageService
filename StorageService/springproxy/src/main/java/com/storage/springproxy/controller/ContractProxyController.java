package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v2/contract")
public class ContractProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public ContractProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/new")
    public Mono<Void> addNewContract(@RequestBody Object dto) {
        return storageServiceClient.addNewContract(dto);
    }

    @GetMapping
    public Mono<Object> getContractByName(@RequestParam String name) {
        return storageServiceClient.getContractByName(name);
    }

    @PostMapping("/{count}/generate")
    public Mono<Void> generateContracts(@PathVariable int count) {
        return storageServiceClient.generateContracts(count);
    }
}