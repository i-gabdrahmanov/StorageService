package com.storage.proxy.controller;

import com.storage.proxy.client.StorageServiceClient;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/v2/zk")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ZkProxyController {

    private static final Logger LOG = LoggerFactory.getLogger(ZkProxyController.class);

    @Inject
    private StorageServiceClient storageServiceClient;

    @Get
    public String getConfig() {
        try {
            return storageServiceClient.getZkConfig();
        } catch (Exception e) {
            LOG.error("Failed to get ZK config from storage service", e);
            throw new HttpStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Storage service unavailable");
        }
    }
} 