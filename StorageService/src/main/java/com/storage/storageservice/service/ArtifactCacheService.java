package com.storage.storageservice.service;

import com.storage.storageservice.dto.ArtifactDto;

import java.util.Collection;

public interface ArtifactCacheService {
    void put(ArtifactDto dto);

    ArtifactDto get(String id);

    void multiset(Collection<ArtifactDto> col);
}
