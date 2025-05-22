package com.storage.storageservice.repository;

import com.storage.storageservice.model.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface InsuranceRepository extends JpaRepository<Insurance, UUID> {
}