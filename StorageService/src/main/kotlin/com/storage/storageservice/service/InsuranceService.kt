package com.storage.storageservice.service

import com.storage.storageservice.dto.InsuranceDto

interface InsuranceService {

    fun newInsurance(dto: InsuranceDto)
}