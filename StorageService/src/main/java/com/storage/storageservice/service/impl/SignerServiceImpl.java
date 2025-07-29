package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.DocumentDto;
import com.storage.storageservice.dto.LinkToSignerRequest;
import com.storage.storageservice.dto.SignerDto;
import com.storage.storageservice.model.Document;
import com.storage.storageservice.model.Signer;
import com.storage.storageservice.repository.DocumentRepository;
import com.storage.storageservice.repository.SignerRepository;
import com.storage.storageservice.service.SignerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignerServiceImpl implements SignerService {

    private final SignerRepository signerRepository;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional
    public SignerDto createSigner(SignerDto request) {
        Signer signer = new Signer();
        signer.setFullName(request.getFullName());
        signer.setActive(request.isActive());
        
        signer = signerRepository.save(signer);
        return mapToDto(signer);
    }

    @Override
    @Transactional
    public SignerDto addDocumentToSigner(LinkToSignerRequest request) {
        Signer signer = signerRepository.findById(UUID.fromString(request.getSignerId()))
                .orElseThrow(() -> new EntityNotFoundException("Signer not found"));
        Document document = documentRepository.findById(UUID.fromString(request.getDocumentId()))
                .orElseThrow(() -> new EntityNotFoundException("Document not found"));
        signer.addDocument(document);
        signer = signerRepository.save(signer);
        
        return mapToDto(signer);
    }

    private SignerDto mapToDto(Signer signer) {
        return SignerDto.builder()
                .id(signer.getId().toString())
                .fullName(signer.getFullName())
                .isActive(signer.isActive())
                .documents(signer.getDocuments() != null ? 
                        signer.getDocuments().stream()
                                .map(doc -> DocumentDto.builder()
                                        .id(doc.getId().toString())
                                        .name(doc.getName())
                                        .surname(doc.getSurname())
                                        .createDateTime(doc.getCreateDateTime())
                                        .build())
                                .collect(Collectors.toList()) : null)
                .build();
    }
} 