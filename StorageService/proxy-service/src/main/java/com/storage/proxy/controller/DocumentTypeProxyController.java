package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@Controller("/api/v2/documentType")
@ExecuteOn(TaskExecutors.BLOCKING)
public class DocumentTypeProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Post("/new")
    public void addNewDocumentType(@Body Object request) {
        storageServiceClient.addNewDocumentType(request);
    }

    @Get
    public HttpResponse<Object> getDocumentType(@QueryValue("name") String documentTypeName) {
        return storageServiceClient.getDocumentType(documentTypeName);
    }
} 