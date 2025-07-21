package com.storage.storageservice.service;

import com.storage.storageservice.dto.InsuranceDto;
import com.storage.storageservice.dto.LinkToArtifactRequest;

public interface InsuranceService {

    void newInsurance(InsuranceDto dto);

    void linkToArtifact(LinkToArtifactRequest request);
}