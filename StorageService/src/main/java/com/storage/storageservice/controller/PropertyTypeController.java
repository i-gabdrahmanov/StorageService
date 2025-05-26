package com.storage.storageservice.controller;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.dto.PropertyTypeDto;
import com.storage.storageservice.service.PropertyTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v2/propertyType")
@RequiredArgsConstructor
public class PropertyTypeController {

    private final PropertyTypeService propertyTypeService;

    @RequestMapping("new")
    void addNewPropertyType(@RequestBody @Valid PropertyTypeDto request) {
        propertyTypeService.addPropertyType(request);
    }

    @GetMapping
    public ResponseEntity<PropertyTypeDto> getPropertyTypeByNameAndDocType(
            @RequestParam(value = "name") String propertyTypeName,
            @RequestBody @Valid DocumentTypeDto request
    ) {
        return ResponseEntity.ok(propertyTypeService.getPropertyTypeByNameAndDocType(
                propertyTypeName, request
        ));
    }
}
