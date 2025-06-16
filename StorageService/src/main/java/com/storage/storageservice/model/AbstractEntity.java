package com.storage.storageservice.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractEntity implements IEntity<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;
}