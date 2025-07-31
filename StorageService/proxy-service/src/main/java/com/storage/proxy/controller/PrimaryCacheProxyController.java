package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@Controller("/api/v2/primaryCache")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PrimaryCacheProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Get
    public String get(@QueryValue String key) {
        return storageServiceClient.getPrimaryCacheValue(key);
    }

    @Post("/{key}/{value}/add")
    public void add(@PathVariable String key, @PathVariable String value) {
        storageServiceClient.addToPrimaryCache(key, value);
    }
} 