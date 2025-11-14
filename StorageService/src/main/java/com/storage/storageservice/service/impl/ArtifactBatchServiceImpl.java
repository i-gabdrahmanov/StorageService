package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.mapper.ArtifactMapper;
import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.repository.ArtifactRepository;
import com.storage.storageservice.service.ArtifactBatchService;
import com.storage.storageservice.service.ArtifactCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtifactBatchServiceImpl implements ArtifactBatchService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactMapper artifactMapper;
    private final ArtifactCacheService artifactCacheService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void saveBatch(List<Artifact> batch) {
        List<Artifact> saved = artifactRepository.saveAll(batch);
        List<ArtifactDto> cacheList = saved.stream()
                .map(artifactMapper::toDto)
                .toList();
        artifactCacheService.multiset(cacheList);
    }
}
