package com.storage.storageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PropertyTypeDto {

    @NotBlank
    private String propertyName;
    @NotNull
    private UUID documentTypeId;
}
