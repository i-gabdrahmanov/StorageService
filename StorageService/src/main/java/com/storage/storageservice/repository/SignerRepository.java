package com.storage.storageservice.repository;

import com.storage.storageservice.model.Signer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SignerRepository extends JpaRepository<Signer, UUID> {
} 