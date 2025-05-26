package com.storage.storageservice.repository;

import com.storage.storageservice.model.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PropertyTypeRepository extends JpaRepository<PropertyType, UUID> {

    Optional<PropertyType> findByPropertyNameAndDocumentTypeId(String name, UUID documentTypeId);
}
