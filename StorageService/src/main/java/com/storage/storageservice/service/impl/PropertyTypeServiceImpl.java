package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.PropertyTypeDto;
import com.storage.storageservice.model.PropertyType;
import com.storage.storageservice.repository.PropertyTypeRepository;
import com.storage.storageservice.service.DocumentTypeService;
import com.storage.storageservice.service.PropertyTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyTypeServiceImpl implements PropertyTypeService {

    private final PropertyTypeRepository propertyTypeRepository;
    private final DocumentTypeService documentTypeService;

    @Override
    @Transactional
    public void addPropertyType(PropertyTypeDto dto) {
        PropertyType  propertyType = new PropertyType();
        propertyType.setPropertyName(dto.getPropertyName());
        propertyType.setDocumentType(documentTypeService.getDocumentTypeById(dto.getDocumentTypeId()));
        propertyTypeRepository.save(propertyType);
    }
}
