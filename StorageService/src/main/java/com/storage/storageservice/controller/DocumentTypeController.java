package com.storage.storageservice.controller;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.service.DocumentTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v2/documentType")
@RequiredArgsConstructor
public class DocumentTypeController {
    private final DocumentTypeService service;

    @PostMapping("new")
    public void addNewDocumentType(@RequestBody @Valid DocumentTypeDto request) {
        service.addDocumentType(request);
    }

    @GetMapping
    public ResponseEntity<DocumentTypeDto> getDocumentType(@RequestParam("name") String documentTypeName) {
        return ResponseEntity.ok(service.getDocumentByName(documentTypeName));
    }
}
