package com.storage.storageservice.controller;

import com.storage.storageservice.dto.LinkToSignerRequest;
import com.storage.storageservice.dto.SignerDto;
import com.storage.storageservice.service.SignerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v2/signer")
@RequiredArgsConstructor
public class SignerController {

    private final SignerService service;

    @PostMapping
    public ResponseEntity<SignerDto> createSigner(@RequestBody SignerDto request) {
        return ResponseEntity.ok(service.createSigner(request));
    }

    @PostMapping("link")
    public ResponseEntity<SignerDto> addDocumentToSigner(@RequestBody LinkToSignerRequest request) {
        return ResponseEntity.ok(service.addDocumentToSigner(request));
    }
} 