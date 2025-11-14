package com.storage.storageservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
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

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Artifact> children = new ArrayList<>();

    @ManyToOne
    private Employee employee;

    public void addChild(Artifact child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Artifact child) {
        children.remove(child);
        child.setParent(null);
    }

    public void setChildren(List<Artifact> children) {
        this.children.clear();
        if (children != null) {
            children.forEach(this::addChild);
        }
    }
}
