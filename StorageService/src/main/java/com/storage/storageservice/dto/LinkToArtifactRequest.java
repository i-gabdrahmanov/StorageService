package com.storage.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class LinkToArtifactRequest {

    private UUID documentId;
    private UUID artifactId;
}
