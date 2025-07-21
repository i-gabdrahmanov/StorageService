package com.storage.storageservice.repository.impl;

import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.model.Artifact_;
import com.storage.storageservice.repository.CustomArtifactRepository;
import com.storage.storageservice.utils.CriteriaFieldResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.Query;

@Repository
@RequiredArgsConstructor
public class CustomArtifactRepositoryImpl implements CustomArtifactRepository {

    private final EntityManager em;

    @Override
    public List<Tuple> findProjectedById(UUID id, Set<String> fields) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
        Root<Artifact> root = query.from(Artifact.class);

        if (fields == null || fields.isEmpty()) {
            fields = getAllFieldsFromEntity(root);
        }

        List<Selection<?>> selections = fields.stream()
                .map(field -> CriteriaFieldResolver.resolveSelection(root, field, cb))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        query.multiselect(selections)
                .where(cb.equal(root.get(Artifact_.ID), id));

        return em.createQuery(query).getResultList();
    }

    private Set<String> getAllFieldsFromEntity(Root<Artifact> root) {
        Set<String> allFields = new HashSet<>();

        // Получаем все поля из корневой сущности
        for (SingularAttribute<? super Artifact, ?> attr : root.getModel().getSingularAttributes()) {
            allFields.add(attr.getName());
        }

        // Добавляем связанные сущности (для вложенных DTO)
        for (PluralAttribute<? super Artifact, ?, ?> attr : root.getModel().getPluralAttributes()) {
            allFields.add(attr.getName());
        }

        return allFields;
    }
}