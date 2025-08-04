package com.storage.springproxy.client;

/**
 * Enum содержащий все пути API для StorageService
 */
public enum ApiPaths {
    
    // Root
    ROOT("/"),
    
    // Artifact endpoints
    ARTIFACT_NEW("/api/v2/artifact/new"),
    ARTIFACT_GENERATE("/api/v2/artifact/{count}/generate"),
    ARTIFACT_BASE("/api/v2/artifact"),
    ARTIFACT_JSON("/api/v2/artifact/json"),
    ARTIFACT_CUSTOM_FIELDS("/api/v2/artifact/customFields"),
    
    // Contract endpoints
    CONTRACT_NEW("/api/v2/contract/new"),
    CONTRACT_BASE("/api/v2/contract"),
    CONTRACT_GENERATE("/api/v2/contract/{count}/generate"),
    
    // DocumentType endpoints
    DOCUMENT_TYPE_NEW("/api/v2/documentType/new"),
    DOCUMENT_TYPE_BASE("/api/v2/documentType"),
    
    // DynamicDocument endpoints
    DYNAMIC_DOCUMENT_NEW("/api/v2/dd/new"),
    
    // PropertyType endpoints
    PROPERTY_TYPE_NEW("/api/v2/propertyType/new"),
    PROPERTY_TYPE_SEARCH("/api/v2/propertyType/search"),
    
    // Insurance endpoints
    INSURANCE_NEW("/api/v2/insurance/new"),
    
    // PrimaryCache endpoints
    PRIMARY_CACHE_BASE("/api/v2/primaryCache"),
    PRIMARY_CACHE_ADD("/api/v2/primaryCache/{key}/{value}/add"),
    
    // Zk endpoints
    ZK_CONFIG("/api/v2/zk");
    
    private final String path;
    
    ApiPaths(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    @Override
    public String toString() {
        return path;
    }
}