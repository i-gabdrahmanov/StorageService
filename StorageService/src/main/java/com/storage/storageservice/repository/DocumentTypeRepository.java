package com.storage.storageservice.repository;

import com.storage.storageservice.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {
}
