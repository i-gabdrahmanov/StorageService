package com.storage.storageservice.model;

import jakarta.persistence.Entity;

@Entity
public class DynamicFieldInfoStr extends DynamicFieldInfoCommon<String> {
    @Override
    //@Column(name = "property_value", length = 2000)
    public String getPropertyValue() {
        return super.getPropertyValue();
    }

    @Override
    public void setPropertyValue(String propertyValue) {
        super.setPropertyValue(propertyValue);
    }
}