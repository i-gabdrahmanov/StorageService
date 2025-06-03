package com.storage.storageservice.modelpayload;

import lombok.Data;

@Data
public class InsuranceDocument {

    private final String series;
    private final String department;
    private final int price;
}
