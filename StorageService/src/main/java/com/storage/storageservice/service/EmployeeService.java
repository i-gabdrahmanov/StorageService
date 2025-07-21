package com.storage.storageservice.service;

import com.storage.storageservice.dto.EmployeeDto;

public interface EmployeeService {

    void addAndLinkEmployeeToArtifact(EmployeeDto dto);
}
