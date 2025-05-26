package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.model.DocumentType;
import com.storage.storageservice.repository.DocumentTypeRepository;
import com.storage.storageservice.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.module.FindException;
import java.util.UUID;

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

    @Override
    @Transactional(readOnly = true)
    public DocumentType getDocumentTypeById(@NotNull UUID id) {
        return documentTypeRepository.findById(id)
                .orElseThrow(() -> new FindException("DocumentType by id %s not found".formatted(id)));
    }
}
