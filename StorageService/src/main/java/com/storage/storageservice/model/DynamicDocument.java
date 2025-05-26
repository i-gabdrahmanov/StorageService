package com.storage.storageservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "dynamic_documents")
@NoArgsConstructor
@AllArgsConstructor
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class DynamicDocument extends AbstractEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private LocalDateTime createDateTime = LocalDateTime.now();

    @OneToMany(mappedBy = "dynamicDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DynamicFieldInfoStr> stringDynamicFields = new HashSet<>();
}