package com.storage.storageservice.service

import com.storage.storageservice.dto.ContractDto

interface ContractService {

    fun addNewContract(dto: ContractDto)

    fun findByName(name: String): ContractDto

    fun generateContracts(count: Int)
}