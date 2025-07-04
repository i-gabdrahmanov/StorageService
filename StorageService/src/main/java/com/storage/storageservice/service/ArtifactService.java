package com.storage.storageservice.service;

import com.storage.storageservice.dto.ArtifactDto;

import java.util.Map;

public interface ArtifactService {

    void addNewArtifact(ArtifactDto dto);

    void generateSomeArtifacts(int count);

    ArtifactDto getArtByJsonField(String key, String value);

    ArtifactDto getArtByNativeJsonFields(Map<String, Object> request);
}
