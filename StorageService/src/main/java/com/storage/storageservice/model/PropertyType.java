package com.storage.storageservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class PropertyType extends AbstractEntity {

    private String propertyName;

    @ManyToOne
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;
}
