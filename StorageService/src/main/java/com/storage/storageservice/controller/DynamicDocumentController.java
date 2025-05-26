package com.storage.storageservice.controller;

import com.storage.storageservice.dto.DynamicDocumentDto;
import com.storage.storageservice.service.DynamicDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v2/dd")
@RequiredArgsConstructor
public class DynamicDocumentController {

    private final DynamicDocumentService service;

    @PostMapping
    @RequestMapping("new")
    public void addNewDocument(@RequestBody DynamicDocumentDto request) {
        service.addNewDocument(request);
    }
}
