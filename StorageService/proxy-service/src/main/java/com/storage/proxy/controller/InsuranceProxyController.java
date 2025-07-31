package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@Controller("/api/v2/insurance")
@ExecuteOn(TaskExecutors.BLOCKING)
public class InsuranceProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Post("/new")
    public HttpResponse<Void> addNewInsurance(@Body Object dto) {
        return storageServiceClient.addNewInsurance(dto);
    }
} 