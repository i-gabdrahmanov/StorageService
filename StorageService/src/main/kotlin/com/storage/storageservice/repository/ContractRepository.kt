package com.storage.storageservice.repository

import com.storage.storageservice.model.Contract
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ContractRepository : JpaRepository<Contract, UUID> {

    fun findByName(name: String): Contract?
}