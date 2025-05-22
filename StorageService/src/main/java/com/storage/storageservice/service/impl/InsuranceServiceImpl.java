package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.InsuranceDto;
import com.storage.storageservice.model.Insurance;
import com.storage.storageservice.repository.InsuranceRepository;
import com.storage.storageservice.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class InsuranceServiceImpl implements InsuranceService {

    private final InsuranceRepository insuranceRepository;

    @Override
    @Transactional
    public void newInsurance(InsuranceDto dto) {
        Insurance insurance = new Insurance();
        insurance.setVehicleType(dto.getVehicleType());
        insurance.setName(dto.getName());
        insurance.setSurname(dto.getSurname());
        insuranceRepository.save(insurance);
    }
}