package com.storage.storageservice.service;

import com.storage.storageservice.dto.ArtifactDto;

public interface ArtifactService {

    void addNewArtifact(ArtifactDto dto);

    void generateSomeArtifacts(int count);
}
