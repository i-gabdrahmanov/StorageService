package com.storage.storageservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertyTypeDto {

    @NotBlank
    private String propertyName;
    @NotBlank
    private UUID documentTypeId;
}
