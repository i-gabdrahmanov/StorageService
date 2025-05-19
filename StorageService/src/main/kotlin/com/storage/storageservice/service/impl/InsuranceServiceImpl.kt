package com.storage.storageservice.service.impl

import com.storage.storageservice.dto.InsuranceDto
import com.storage.storageservice.model.Insurance
import com.storage.storageservice.repository.InsuranceRepository
import com.storage.storageservice.service.InsuranceService
import org.springframework.stereotype.Service

@Service
class InsuranceServiceImpl(
    private val insuranceRepository: InsuranceRepository
) : InsuranceService {
    override fun newInsurance(dto: InsuranceDto) {
        val insurance = Insurance()
        insurance.vehicleType = dto.vehicleType
        insurance.name = dto.name
        insurance.surname = dto.surname
        insuranceRepository.save(insurance)
    }
}