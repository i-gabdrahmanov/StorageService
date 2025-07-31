package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

import java.util.Map;

@Controller("/api/v2/artifact")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ArtifactProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Post("/new")
    public void addNewArtifact(@Body Object request) {
        storageServiceClient.addNewArtifact(request);
    }

    @Post("/{count}/generate")
    public void generateSomeArtifacts(@PathVariable int count) {
        storageServiceClient.generateSomeArtifacts(count);
    }

    @Get
    public HttpResponse<Object> getByJsonField(@QueryValue String key, @QueryValue String value) {
        return storageServiceClient.getArtifactByJsonField(key, value);
    }

    @Post("/json")  // Изменили на POST
    public HttpResponse<Object> getByNativeJsonFields(@Body Map<String, Object> request) {
        return storageServiceClient.getArtifactByNativeJsonFields(request);
    }

    @Post("/customFields")  // Изменили на POST
    public HttpResponse<Object> getCustomRequestById(@Body Object request) {
        return storageServiceClient.getCustomArtifactById(request);
    }
} 