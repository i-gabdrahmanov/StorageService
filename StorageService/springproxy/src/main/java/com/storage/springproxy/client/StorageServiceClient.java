package com.storage.springproxy.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.storage.springproxy.client.ApiPaths.*;

@Service
public class StorageServiceClient {

    private final WebClient webClient;

    @Autowired
    public StorageServiceClient(WebClient storageServiceWebClient) {
        this.webClient = storageServiceWebClient;
    }

    // Artifact endpoints
    public Mono<Void> addNewArtifact(Object request) {
        return webClient.post()
                .uri(ARTIFACT_NEW.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> generateSomeArtifacts(int count) {
        return webClient.post()
                .uri(ARTIFACT_GENERATE.getPath(), count)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Object> getArtifactByJsonField(String key, String value) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ARTIFACT_BASE.getPath())
                        .queryParam("key", key)
                        .queryParam("value", value)
                        .build())
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> getArtifactByNativeJsonFields(Map<String, Object> request) {
        return webClient.post()
                .uri(ARTIFACT_JSON.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> getCustomArtifactById(Object request) {
        return webClient.post()
                .uri(ARTIFACT_CUSTOM_FIELDS.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Contract endpoints
    public Mono<Void> addNewContract(Object dto) {
        return webClient.post()
                .uri(CONTRACT_NEW.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Object> getContractByName(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(CONTRACT_BASE.getPath())
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Void> generateContracts(int count) {
        return webClient.post()
                .uri(CONTRACT_GENERATE.getPath(), count)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // DocumentType endpoints
    public Mono<Void> addNewDocumentType(Object request) {
        return webClient.post()
                .uri(DOCUMENT_TYPE_NEW.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Object> getDocumentType(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DOCUMENT_TYPE_BASE.getPath())
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToMono(Object.class);
    }

    // DynamicDocument endpoints
    public Mono<Void> addNewDynamicDocument(Object request) {
        return webClient.post()
                .uri(DYNAMIC_DOCUMENT_NEW.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // PropertyType endpoints
    public Mono<Void> addNewPropertyType(Object request) {
        return webClient.post()
                .uri(PROPERTY_TYPE_NEW.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Object> getPropertyTypeByNameAndDocType(String name, Object request) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(PROPERTY_TYPE_SEARCH.getPath())
                        .queryParam("name", name)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Insurance endpoints
    public Mono<Void> addNewInsurance(Object dto) {
        return webClient.post()
                .uri(INSURANCE_NEW.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // PrimaryCache endpoints
    public Mono<String> getPrimaryCacheValue(String key) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PRIMARY_CACHE_BASE.getPath())
                        .queryParam("key", key)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<Void> addToPrimaryCache(String key, String value) {
        return webClient.post()
                .uri(PRIMARY_CACHE_ADD.getPath(), key, value)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Zk endpoints
    public Mono<String> getZkConfig() {
        return webClient.get()
                .uri(ZK_CONFIG.getPath())
                .retrieve()
                .bodyToMono(String.class);
    }

    // Root endpoint
    public Mono<Void> root() {
        return webClient.get()
                .uri(ROOT.getPath())
                .retrieve()
                .bodyToMono(Void.class);
    }
}