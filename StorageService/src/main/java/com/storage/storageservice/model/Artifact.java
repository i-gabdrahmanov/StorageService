package com.storage.storageservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Artifact extends AbstractEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Artifact parent;

    @OneToMany(mappedBy = "parent")
    private List<Artifact> children;

    @ManyToOne
    private Employee employee;

    @OneToMany
    private List<Document> documents;
}
