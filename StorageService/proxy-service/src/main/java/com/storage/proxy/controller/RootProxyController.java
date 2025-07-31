package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@ExecuteOn(TaskExecutors.BLOCKING)
public class RootProxyController {

    private static final Logger LOG = LoggerFactory.getLogger(RootProxyController.class);

    @Inject
    private StorageServiceClient storageServiceClient;

    @Get
    public HttpResponse<Void> root() {
        try {
            return storageServiceClient.root();
        } catch (Exception e) {
            LOG.error("Failed to proxy root request to storage service", e);
            throw new HttpStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Storage service unavailable");
        }
    }
} 