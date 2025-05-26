package com.storage.storageservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DynamicFieldDto {

    private String propertyName;
    private String contentType;
    private Object value;
}
