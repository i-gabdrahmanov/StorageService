package com.storage.storageservice.service.impl;

import com.storage.storageservice.dto.DynamicDocumentDto;
import com.storage.storageservice.dto.DynamicFieldDto;
import com.storage.storageservice.model.DynamicDocument;
import com.storage.storageservice.model.DynamicFieldInfoStr;
import com.storage.storageservice.repository.DynamicDocumentRepository;
import com.storage.storageservice.repository.PropertyTypeRepository;
import com.storage.storageservice.service.DynamicDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DynamicDocumentServiceImpl implements DynamicDocumentService {

    private final DynamicDocumentRepository dynamicDocumentRepository;
    private final PropertyTypeRepository propertyTypeRepository;

    @Override
    @Transactional
    public void addNewDocument(DynamicDocumentDto dto) {
        DynamicDocument dynamicDocument = new DynamicDocument();
        dynamicDocument.setName(dto.getName());
        dynamicDocument.setSurname(dto.getSurname());

        fillDynamicFields(dto, dynamicDocument);
        dynamicDocumentRepository.save(dynamicDocument);
    }

    private void fillDynamicFields (DynamicDocumentDto dto, DynamicDocument target) {
        dto.getDynamicFields()
              .stream().collect(Collectors.groupingBy(DynamicFieldDto::getContentType))
              .forEach((key, value) -> {
                  if (key.equalsIgnoreCase("string")) {
                      List<DynamicFieldInfoStr> resultList = value.stream()
                              .map(list -> {
                                  DynamicFieldInfoStr str = new DynamicFieldInfoStr();
                                  str.setDynamicDocument(target);
                                  str.setPropertyType(propertyTypeRepository
                                          .findByPropertyNameAndDocumentTypeId(list.getPropertyName(), dto.getDocumentTypeId())
                                          .orElseThrow());
                                  str.setPropertyValue((String) list.getValue());
                                  return str;
                              })
                              .toList();
                      target.getStringDynamicFields().addAll(resultList);
                  }
              });
    }
}
