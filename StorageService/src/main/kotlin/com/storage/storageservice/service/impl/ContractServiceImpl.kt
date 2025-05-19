package com.storage.storageservice.service.impl

import com.storage.storageservice.dto.ContractDto
import com.storage.storageservice.model.Contract
import com.storage.storageservice.repository.ContractRepository
import com.storage.storageservice.service.ContractService
import org.springframework.stereotype.Service
import java.lang.module.FindException

@Service
class ContractServiceImpl(
    private val contractRepository: ContractRepository
) : ContractService {
    override fun addNewContract(dto: ContractDto) {
        val contract = Contract()
        contract.contractText = dto.contractText
        contract.name = dto.name
        contract.surname = dto.surname
        contractRepository.save(contract)
    }

    override fun findByName(name: String): ContractDto {
        val contract = contractRepository.findByName(name)
            ?: throw FindException("Контракт не найден")
        return ContractDto(
            contract.name,
            contract.surname,
            contract.contractText
        )
    }
}