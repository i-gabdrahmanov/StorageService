package com.storage.storageservice.repository;

import com.storage.storageservice.model.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID>, CustomArtifactRepository {

    @Query(value = """
            SELECT * FROM artifact
            WHERE payload @> jsonb_build_object(:key, :value)
            """, nativeQuery = true)
    Artifact findByJsonField(@Param("key") String key,
                             @Param("value") String value);

    @Query(value = "SELECT * FROM artifact WHERE payload @> CAST(:jsonFilter AS jsonb)",
            nativeQuery = true)
    Artifact findByJson(@Param("jsonFilter") String jsonFilter);
}
