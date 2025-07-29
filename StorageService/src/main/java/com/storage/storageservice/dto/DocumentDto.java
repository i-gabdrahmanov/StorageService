package com.storage.storageservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto {
    private String id;
    private String name;
    private String surname;
    private LocalDateTime createDateTime;
    private String type; // discriminator
    // Insurance fields
    private String vehicleType;
    // Contract fields
    private String contractText;
    // Можно добавить другие специфичные поля для других наследников
    private SignerDto signer;
} 