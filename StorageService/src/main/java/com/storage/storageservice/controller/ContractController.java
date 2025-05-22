package com.storage.storageservice.controller;

import com.storage.storageservice.dto.ContractDto;
import com.storage.storageservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v2/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping("new")
    public ResponseEntity<Void> addNewContract(@RequestBody ContractDto dto) {
        contractService.addNewContract(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ContractDto> getByName(@RequestParam String name) {
        ContractDto response = contractService.findByName(name);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{count}/generate")
    public ResponseEntity<Void> generateContracts(@PathVariable int count) {
        contractService.generateContracts(count);
        return ResponseEntity.ok().build();
    }
}