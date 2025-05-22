package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.model.DocumentType;
import com.storage.storageservice.repository.DocumentTypeRepository;
import com.storage.storageservice.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentTypeServiceImpl implements DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    @Override
    @Transactional
    public void addDocumentType(DocumentTypeDto dto) {
        DocumentType documentType = new DocumentType();
        documentType.setName(dto.getDocumentTypeName());
        documentTypeRepository.save(documentType);
    }
}
