package com.storage.storageservice.repository;

import jakarta.persistence.Tuple;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CustomArtifactRepository {

    List<jakarta.persistence.Tuple> findProjectedById(UUID id, Set<String> fields);
}
