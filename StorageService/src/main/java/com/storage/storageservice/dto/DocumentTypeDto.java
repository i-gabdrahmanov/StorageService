package com.storage.storageservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentTypeDto {

    @NotBlank
    private String documentTypeName;
}
