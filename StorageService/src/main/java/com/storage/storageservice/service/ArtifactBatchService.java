package com.storage.storageservice.service;

import com.storage.storageservice.model.Artifact;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ArtifactBatchService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveBatch(List<Artifact> batch);
}

