package com.storage.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ArtifactDto {

    private UUID id;
    private String name;
    private String surname;
    private Map<String, Object> payload;
    private List<ArtifactDto> children;
    private EmployeeDto employee;
}
