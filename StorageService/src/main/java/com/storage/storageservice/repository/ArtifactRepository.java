package com.storage.storageservice.repository;

import com.storage.storageservice.model.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {
}
