package com.storage.storageservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class DocumentType extends AbstractEntity {
    @Column(name = "document_name")
    private String documentName;

    @OneToMany(fetch = FetchType.LAZY)
    private List<PropertyType> propertyTypes;
}
