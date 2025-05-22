package com.storage.storageservice.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class DynamicFieldInfo<T> extends AbstractEntity {

    //private DynamicDocument dynamicDocument;
}