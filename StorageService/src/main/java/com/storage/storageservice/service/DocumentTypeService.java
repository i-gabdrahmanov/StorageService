package com.storage.storageservice.service;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.model.DocumentType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface DocumentTypeService {

    void addDocumentType(DocumentTypeDto dto);

    DocumentType getDocumentTypeById(@NotNull UUID id);

    DocumentTypeDto getDocumentByName(String documentName);
}
