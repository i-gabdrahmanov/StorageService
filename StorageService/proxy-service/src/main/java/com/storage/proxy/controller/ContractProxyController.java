package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;

@Controller("/api/v2/contract")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ContractProxyController {

    @Inject
    private StorageServiceClient storageServiceClient;

    @Post("/new")
    public HttpResponse<Void> addNewContract(@Body Object dto) {
        return storageServiceClient.addNewContract(dto);
    }

    @Get
    public HttpResponse<Object> getByName(@QueryValue String name) {
        return storageServiceClient.getContractByName(name);
    }

    @Post("/{count}/generate")
    public HttpResponse<Void> generateContracts(@PathVariable int count) {
        return storageServiceClient.generateContracts(count);
    }
} 