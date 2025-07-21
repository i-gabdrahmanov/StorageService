package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.InsuranceDto;
import com.storage.storageservice.dto.LinkToArtifactRequest;
import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.model.Insurance;
import com.storage.storageservice.repository.ArtifactRepository;
import com.storage.storageservice.repository.InsuranceRepository;
import com.storage.storageservice.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class InsuranceServiceImpl implements InsuranceService {

    private final InsuranceRepository insuranceRepository;
    private final ArtifactRepository artifactRepository;

    @Override
    @Transactional
    public void newInsurance(InsuranceDto dto) {
        Insurance insurance = new Insurance();
        insurance.setVehicleType(dto.getVehicleType());
        insurance.setName(dto.getName());
        insurance.setSurname(dto.getSurname());
        insuranceRepository.save(insurance);
    }

    @Override
    @Transactional
    public void linkToArtifact(LinkToArtifactRequest request) {
        Artifact artifact = artifactRepository.findById(request.getArtifactId())
                .orElseThrow();
        Insurance insurance = insuranceRepository.findById(request.getDocumentId())
                .orElseThrow();
        insurance.setArtifact(artifact);
    }
}