package com.storage.storageservice.service;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.dto.CustomArtifactRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

public interface ArtifactService {

    void addNewArtifact(ArtifactDto dto);

    void generateSomeArtifacts(int count);

    ArtifactDto getArtByJsonField(String key, String value);

    ArtifactDto getArtByNativeJsonFields(Map<String, Object> request);

    ArtifactDto getCustomById(CustomArtifactRequest request);

    @Transactional
    ArtifactDto getById(UUID id);
}
