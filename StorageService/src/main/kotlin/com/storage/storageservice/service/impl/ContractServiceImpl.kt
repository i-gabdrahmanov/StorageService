package com.storage.storageservice.service.impl

import com.storage.storageservice.dto.ContractDto
import com.storage.storageservice.model.Contract
import com.storage.storageservice.repository.ContractRepository
import com.storage.storageservice.service.ContractService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.module.FindException

@Service
class ContractServiceImpl(
    private val contractRepository: ContractRepository
) : ContractService {
    @Transactional
    override fun addNewContract(dto: ContractDto) {
        val contract = Contract()
        contract.contractText = dto.contractText
        contract.name = dto.name
        contract.surname = dto.surname
        contractRepository.save(contract)
    }

    @Transactional
    override fun findByName(name: String): ContractDto {
        val contract = contractRepository.findByName(name)
            ?: throw FindException("Контракт не найден")
        return ContractDto(
            contract.name,
            contract.surname,
            contract.contractText
        )
    }

    @Transactional
    override fun generateContracts(count: Int) {
        var counter = 0
       val resultList = ArrayList<Contract>()
        while (counter < count) {
            val contract = Contract()
            contract.name = List(40) {
                ('a'..'z').random()
            }.joinToString("")
            contract.surname = List(40) {
                ('a'..'z').random()
            }.joinToString("")
            contract.contractText = List(100) {
                ('a'..'z').random()
            }.joinToString("")
            resultList.add(contract)
            counter++
        }
       contractRepository.saveAll(resultList)
    }
}