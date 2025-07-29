package com.storage.storageservice.service;

import com.storage.storageservice.dto.LinkToSignerRequest;
import com.storage.storageservice.dto.SignerDto;

public interface SignerService {
    SignerDto createSigner(SignerDto request);
    SignerDto addDocumentToSigner(LinkToSignerRequest request);
} 