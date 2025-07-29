package com.storage.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkToSignerRequest {
    private String signerId;
    private String documentId;
} 