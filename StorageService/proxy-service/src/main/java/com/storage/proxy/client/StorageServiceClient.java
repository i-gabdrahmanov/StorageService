package com.storage.proxy.client;

import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.HttpResponse;

import java.util.Map;

@Client("${storage-service.url}")
public interface StorageServiceClient {

    // Artifact endpoints
    @Post("/api/v2/artifact/new")
    void addNewArtifact(@Body Object request);

    @Post("/api/v2/artifact/{count}/generate")
    void generateSomeArtifacts(@PathVariable int count);

    @Get("/api/v2/artifact{?key,value}")
    HttpResponse<Object> getArtifactByJsonField(@QueryValue String key, @QueryValue String value);

    @Post("/api/v2/artifact/json")  // POST запрос
    HttpResponse<Object> getArtifactByNativeJsonFields(@Body Map<String, Object> request);

    @Post("/api/v2/artifact/customFields")  // POST запрос
    HttpResponse<Object> getCustomArtifactById(@Body Object request);

    // Contract endpoints
    @Post("/api/v2/contract/new")
    HttpResponse<Void> addNewContract(@Body Object dto);

    @Get("/api/v2/contract{?name}")
    HttpResponse<Object> getContractByName(@QueryValue String name);

    @Post("/api/v2/contract/{count}/generate")
    HttpResponse<Void> generateContracts(@PathVariable int count);

    // DocumentType endpoints
    @Post("/api/v2/documentType/new")
    void addNewDocumentType(@Body Object request);

    @Get("/api/v2/documentType{?name}")
    HttpResponse<Object> getDocumentType(@QueryValue String name);

    // DynamicDocument endpoints
    @Post("/api/v2/dd/new")
    void addNewDynamicDocument(@Body Object request);

    // PropertyType endpoints
    @Post("/api/v2/propertyType/new")
    void addNewPropertyType(@Body Object request);

    @Post("/api/v2/propertyType/search{?name}")
    HttpResponse<Object> getPropertyTypeByNameAndDocType(@QueryValue String name, @Body Object request);

    // Insurance endpoints
    @Post("/api/v2/insurance/new")
    HttpResponse<Void> addNewInsurance(@Body Object dto);

    // PrimaryCache endpoints
    @Get("/api/v2/primaryCache{?key}")
    String getPrimaryCacheValue(@QueryValue String key);

    @Post("/api/v2/primaryCache/{key}/{value}/add")
    void addToPrimaryCache(@PathVariable String key, @PathVariable String value);

    // Zk endpoints
    @Get("/api/v2/zk")
    String getZkConfig();

    // Root endpoint
    @Get
    HttpResponse<Void> root();
} 