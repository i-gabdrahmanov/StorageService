package com.storage.storageservice.controller;

import com.storage.storageservice.dto.EmployeeDto;
import com.storage.storageservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "api/v2/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping(value = "add")
    public void addAndLinkEmployeeToArtifact(@RequestBody EmployeeDto dto) {
        employeeService.addAndLinkEmployeeToArtifact(dto);
    }
}
