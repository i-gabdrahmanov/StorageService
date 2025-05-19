package com.storage.storageservice.controller

import com.storage.storageservice.dto.ContractDto
import com.storage.storageservice.service.ContractService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v2/contract")
class ContractController(
    private val contractService: ContractService
) {

    @PostMapping("new")
    fun addNewContract(@RequestBody dto: ContractDto): ResponseEntity<Unit> {
        contractService.addNewContract(dto)
        return ResponseEntity.ok().build()
    }

    @GetMapping
    fun getByName(@RequestParam name: String): ResponseEntity<ContractDto> {
        val response = contractService.findByName(name)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{count}/generate")
    fun generateContracts(@PathVariable count: Int): ResponseEntity<Unit> {
        contractService.generateContracts(count)
        return ResponseEntity.ok().build()
    }
}