package com.storage.storageservice.controller

import com.storage.storageservice.dto.InsuranceDto
import com.storage.storageservice.service.InsuranceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v2/insurance")
class InsuranceController(
    private val insuranceService: InsuranceService
) {

    @PostMapping("new")
    fun addNewInsurance(@RequestBody dto: InsuranceDto): ResponseEntity<Unit> {
        insuranceService.newInsurance(dto)
        return ResponseEntity.ok().build()
    }
}