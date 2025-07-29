package com.storage.storageservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Signer extends AbstractEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "signer", fetch = FetchType.LAZY)
    private List<Document> documents;

    public void addDocument(Document document) {
        documents.add(document);
        document.setSigner(this);
    }

    public void removeDocument(Document document) {
        documents.remove(document);
        document.setSigner(null);
    }
}
