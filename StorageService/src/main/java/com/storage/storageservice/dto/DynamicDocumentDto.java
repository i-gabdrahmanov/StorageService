package com.storage.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DynamicDocumentDto {

    private String name;
    private String surname;
    private List<DynamicFieldDto> dynamicFields;
    private UUID documentTypeId;
}
