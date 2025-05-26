package com.storage.storageservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class PropertyTypeDto {

    @NotBlank
    private String propertyName;
    @NotBlank
    private UUID documentTypeId;
}
