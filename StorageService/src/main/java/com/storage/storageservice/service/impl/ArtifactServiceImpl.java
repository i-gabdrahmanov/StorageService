package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.repository.ArtifactRepository;
import com.storage.storageservice.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtifactServiceImpl implements ArtifactService {

    public final ArtifactRepository artifactRepository;
    @Override
    @Transactional
    public void addNewArtifact(ArtifactDto dto) {
       addNewArtifactRecursive(dto, null);
    }

    private void addNewArtifactRecursive(ArtifactDto dto, Artifact parent) {
        Artifact artifact = new Artifact();
        artifact.setName(dto.getName());
        artifact.setSurname(dto.getSurname());
        artifact.setPayload(dto.getPayload());
        if (parent != null) {
            artifact.setParent(parent);
        }
        if (dto.getChildren() != null) {
            dto.getChildren().forEach(c -> addNewArtifactRecursive(c, artifact));
        }
        artifactRepository.save(artifact);
    }
}
