package com.storage.storageservice.controller;

import com.storage.storageservice.dto.PropertyTypeDto;
import com.storage.storageservice.service.PropertyTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v2/propertyType")
@RequiredArgsConstructor
public class PropertyTypeController {

    private final PropertyTypeService propertyTypeService;

    @RequestMapping("new")
    void addNewPropertyType(@RequestBody @Valid PropertyTypeDto request) {
        propertyTypeService.addPropertyType(request);
    }
}
