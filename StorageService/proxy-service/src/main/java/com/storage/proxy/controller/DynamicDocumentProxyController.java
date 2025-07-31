package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@Controller("/api/v2/dd")
@ExecuteOn(TaskExecutors.BLOCKING)
public class DynamicDocumentProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Post("/new")
    public void addNewDocument(@Body Object request) {
        storageServiceClient.addNewDynamicDocument(request);
    }
} 