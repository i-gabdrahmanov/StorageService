package com.storage.storageservice.service;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.dto.PropertyTypeDto;

public interface PropertyTypeService {

    void addPropertyType(PropertyTypeDto dto);

    PropertyTypeDto getPropertyTypeByNameAndDocType(String propertyName, DocumentTypeDto documentTypeDto);
}
