package com.storage.storageservice.service;

import com.storage.storageservice.dto.DocumentTypeDto;
import com.storage.storageservice.model.DocumentType;
import com.storage.storageservice.repository.DocumentTypeRepository;
import com.storage.storageservice.service.impl.DocumentTypeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.module.FindException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceImplTest {

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @InjectMocks
    private DocumentTypeServiceImpl documentTypeService;

    @Test
    void addDocumentType_ShouldSaveDocumentType() {
        // Given
        DocumentTypeDto dto = new DocumentTypeDto();
        dto.setDocumentTypeName("Test Document Type");

        // When
        documentTypeService.addDocumentType(dto);

        // Then
        verify(documentTypeRepository).save(any(DocumentType.class));
    }

    @Test
    void getDocumentTypeById_ShouldReturnDocumentType_WhenExists() {
        // Given
        UUID id = UUID.randomUUID();
        DocumentType expectedDocumentType = new DocumentType();
        expectedDocumentType.setId(id);
        expectedDocumentType.setName("Test Document Type");

        when(documentTypeRepository.findById(id)).thenReturn(Optional.of(expectedDocumentType));

        // When
        DocumentType result = documentTypeService.getDocumentTypeById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Test Document Type", result.getName());
        verify(documentTypeRepository).findById(id);
    }

    @Test
    void getDocumentTypeById_ShouldThrowException_WhenNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(documentTypeRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        FindException exception = assertThrows(FindException.class, () -> {
            documentTypeService.getDocumentTypeById(id);
        });

        assertEquals("DocumentType by id %s not found".formatted(id), exception.getMessage());
        verify(documentTypeRepository).findById(id);
    }

    @Test
    void getDocumentByName_ShouldReturnDocumentTypeDto_WhenExists() {
        // Given
        String documentName = "Test Document Type";
        DocumentType expectedDocumentType = new DocumentType();
        expectedDocumentType.setId(UUID.randomUUID());
        expectedDocumentType.setName(documentName);

        when(documentTypeRepository.getDocumentTypeByName(documentName)).thenReturn(Optional.of(expectedDocumentType));

        // When
        DocumentTypeDto result = documentTypeService.getDocumentByName(documentName);

        // Then
        assertNotNull(result);
        assertEquals(documentName, result.getDocumentTypeName());
        assertNotNull(result.getDocumentTypeId());
        verify(documentTypeRepository).getDocumentTypeByName(documentName);
    }

    @Test
    void getDocumentByName_ShouldThrowException_WhenNotFound() {
        // Given
        String documentName = "Non Existent Document Type";
        when(documentTypeRepository.getDocumentTypeByName(documentName)).thenReturn(Optional.empty());

        // When & Then
        FindException exception = assertThrows(FindException.class, () -> {
            documentTypeService.getDocumentByName(documentName);
        });

        assertEquals("DocumentType by name %s not found".formatted(documentName), exception.getMessage());
        verify(documentTypeRepository).getDocumentTypeByName(documentName);
    }
}

