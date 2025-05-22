package com.storage.storageservice.controller;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v2/documentType")
@RequiredArgsConstructor
public class DocumentTypeController {
    private final DocumentTypeService service;

    @PostMapping("new")
    public void addNewDocumentType(@RequestBody DocumentTypeDto request) {
        service.addDocumentType(request);
    }
}
