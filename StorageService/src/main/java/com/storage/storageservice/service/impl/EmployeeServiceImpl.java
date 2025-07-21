package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.EmployeeDto;
import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.model.Employee;
import com.storage.storageservice.repository.ArtifactRepository;
import com.storage.storageservice.repository.EmployeeRepository;
import com.storage.storageservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final ArtifactRepository artifactRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public void addAndLinkEmployeeToArtifact(EmployeeDto dto) {
        Artifact artifact = artifactRepository.findById(UUID.fromString(dto.getId())).orElseThrow();
        Employee employee = Employee.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .build();
        employeeRepository.save(employee);
        artifact.setEmployee(employee);
    }
}
