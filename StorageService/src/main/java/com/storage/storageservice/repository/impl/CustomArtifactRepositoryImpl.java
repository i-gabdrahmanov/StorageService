package com.storage.storageservice.repository.impl;

import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.model.Artifact_;
import com.storage.storageservice.repository.CustomArtifactRepository;
import com.storage.storageservice.utils.CriteriaFieldResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CustomArtifactRepositoryImpl implements CustomArtifactRepository {

    private final EntityManager em;

    @Override
    public Tuple findProjectedById(UUID id, Set<String> fields) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
        Root<Artifact> root = query.from(Artifact.class);

        // Динамический SELECT
        List<Selection<?>> selections = fields.stream()
                .map(field -> CriteriaFieldResolver.resolveSelection(root, field, cb))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // WHERE-условие
        query.multiselect(selections)
                .where(cb.equal(root.get(Artifact_.ID), id));

       return  em.createQuery(query).getSingleResult();
    }

    private Map<String, Object> convertToMap(Tuple tuple, Set<String> fields) {
        Map<String, Object> map = new HashMap<>();
        for (String field : fields) {
            try {
                map.put(field, tuple.get(field));
            } catch (IllegalArgumentException ignored) {
                // Поле не было выбрано в запросе
            }
        }
        return map;
    }
}