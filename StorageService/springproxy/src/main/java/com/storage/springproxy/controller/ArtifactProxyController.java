package com.storage.springproxy.controller;

import com.storage.springproxy.client.StorageServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/artifact")
public class ArtifactProxyController {

    private final StorageServiceClient storageServiceClient;

    @Autowired
    public ArtifactProxyController(StorageServiceClient storageServiceClient) {
        this.storageServiceClient = storageServiceClient;
    }

    @PostMapping("/new")
    public Mono<Void> addNewArtifact(@RequestBody Object request) {
        return storageServiceClient.addNewArtifact(request);
    }

    @PostMapping("/{count}/generate")
    public Mono<Void> generateSomeArtifacts(@PathVariable int count) {
        return storageServiceClient.generateSomeArtifacts(count);
    }

    @GetMapping
    public Mono<Object> getByJsonField(@RequestParam String key, @RequestParam String value) {
        return storageServiceClient.getArtifactByJsonField(key, value);
    }

    @PostMapping("/json")
    public Mono<Object> getByNativeJsonFields(@RequestBody Map<String, Object> request) {
        return storageServiceClient.getArtifactByNativeJsonFields(request);
    }

    @PostMapping("/customFields")
    public Mono<Object> getCustomRequestById(@RequestBody Object request) {
        return storageServiceClient.getCustomArtifactById(request);
    }
}