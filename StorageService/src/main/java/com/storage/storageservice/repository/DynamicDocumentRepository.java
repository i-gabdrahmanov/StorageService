package com.storage.storageservice.repository;

import com.storage.storageservice.model.DynamicDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DynamicDocumentRepository extends JpaRepository<DynamicDocument, UUID> {
}
