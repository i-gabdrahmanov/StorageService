package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@Controller("/api/v2/propertyType")
@ExecuteOn(TaskExecutors.BLOCKING)
public class PropertyTypeProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Post("/new")
    public void addNewPropertyType(@Body Object request) {
        storageServiceClient.addNewPropertyType(request);
    }

    @Post("/search") // Изменили на POST так как есть тело запроса
    public HttpResponse<Object> getPropertyTypeByNameAndDocType(
            @QueryValue("name") String propertyTypeName,
            @Body Object request) {
        return storageServiceClient.getPropertyTypeByNameAndDocType(propertyTypeName, request);
    }
} 