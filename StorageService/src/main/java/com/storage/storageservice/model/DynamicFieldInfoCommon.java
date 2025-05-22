package com.storage.storageservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class DynamicFieldInfoCommon<T> extends DynamicFieldInfo<T> {

    @Column(name = "property_value")
    private T propertyValue;
}