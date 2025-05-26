package com.storage.storageservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DocumentTypeDto {

    @NotBlank
    private String documentTypeName;
    private UUID documentTypeId;
}
