package com.storage.storageservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceDto {
    private String name;
    private String surname;
    private String vehicleType;
}
