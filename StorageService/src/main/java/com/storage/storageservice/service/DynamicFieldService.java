package com.storage.storageservice.service;

import com.storage.storageservice.dto.DynamicFieldDto;

import java.util.List;

public interface DynamicFieldService {

    void addField(DynamicFieldDto dto);

    List<DynamicFieldDto> getFieldsByDocumentType(Long documentTypeId);
}
