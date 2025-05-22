package com.storage.storageservice.controller;

import com.storage.storageservice.dto.InsuranceDto;
import com.storage.storageservice.service.InsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v2/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;

    @PostMapping("new")
    public ResponseEntity<Void> addNewInsurance(@RequestBody InsuranceDto dto) {
        insuranceService.newInsurance(dto);
        return ResponseEntity.ok().build();
    }
}