package com.storage.storageservice.utils;

import jakarta.persistence.Tuple;

import java.util.Set;

public interface DynamicDtoMapper {

    <T> T mapToDto(Tuple tuple, Set<String> fields, Class<T> dtoClass);
}
