package com.storage.storageservice.repository;

import com.storage.storageservice.model.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PropertyTypeRepository extends JpaRepository<PropertyType, UUID> {
}
