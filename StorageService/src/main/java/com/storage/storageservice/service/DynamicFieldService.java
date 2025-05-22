package com.storage.storageservice.service;

public interface DynamicFieldService {

    void addField(DynamicFieldDto dto);

    List<DynamicFieldDto> getFieldsByDocumentType(Long documentTypeId);
}
