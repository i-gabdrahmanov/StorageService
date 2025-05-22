package com.storage.storageservice.model;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class DynamicFieldInfo extends AbstractEntity {

    @ManyToOne
    private DynamicDocument dynamicDocument;

    @OneToOne
    private PropertyType propertyType;
}