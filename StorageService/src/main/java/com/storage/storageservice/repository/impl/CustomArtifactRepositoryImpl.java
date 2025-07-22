package com.storage.storageservice.repository.impl;

import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.repository.CustomArtifactRepository;
import com.storage.storageservice.utils.CriteriaFieldResolver;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomArtifactRepositoryImpl implements CustomArtifactRepository {

    private final EntityManager em;
    private final Map<Class<?>, Set<String>> entityFieldsCache = new HashMap<>();
    private final Map<Class<?>, Set<String>> entityRelationsCache = new HashMap<>();

    @Override
    public List<Tuple> findProjectedById(UUID id, Set<String> fields) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
        Root<Artifact> root = query.from(Artifact.class);
        Map<String, Join<?, ?>> joins = new HashMap<>();

        // Если поля не указаны, получаем все поля из сущности
        if (fields == null || fields.isEmpty()) {
            fields = getAllFieldsFromEntity(root);
        } else {
            // Проверяем каждое поле на необходимость получения всех полей
            Set<String> expandedFields = new HashSet<>();
            for (String field : fields) {
                if (field.contains(".")) {
                    String prefix = field.substring(0, field.indexOf("."));
                    // Создаем join, если еще не создан
                    if (isRelation(root.getJavaType(), prefix) && !joins.containsKey(prefix)) {
                        joins.put(prefix, root.join(prefix, JoinType.LEFT));
                    }
                    expandedFields.add(field);
                    // Добавляем id для связанной сущности, если его нет
                    expandedFields.add(prefix + ".id");
                } else {
                    // Проверяем, является ли поле связью
                    if (isRelation(root.getJavaType(), field)) {
                        Join<?, ?> join = root.join(field, JoinType.LEFT);
                        joins.put(field, join);
                        Set<String> entityFields = getEntityFields(join.getJavaType());
                        if (!entityFields.isEmpty()) {
                            // Всегда добавляем id для связанной сущности
                            expandedFields.add(field + ".id");
                            // Если нет уточняющих полей после точки, добавляем все поля
                            if (!fields.stream().anyMatch(f -> f.startsWith(field + "."))) {
                                entityFields.forEach(entityField -> 
                                    expandedFields.add(field + "." + entityField));
                            }
                        }
                    } else {
                        expandedFields.add(field);
                    }
                }
            }
            fields = expandedFields;
        }

        log.info("Expanded fields: {}", fields);
        log.info("Created joins for: {}", joins.keySet());

        List<Selection<?>> selections = fields.stream()
                .map(field -> CriteriaFieldResolver.resolveSelection(root, field, cb))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        query.multiselect(selections)
                .where(cb.equal(root.get("id"), id));

        TypedQuery<Tuple> typedQuery = em.createQuery(query);
        log.info("Generated SQL: {}", typedQuery.unwrap(org.hibernate.query.Query.class).getQueryString());

        List<Tuple> results = typedQuery.getResultList();
        log.info("Query results size: {}", results.size());
        if (!results.isEmpty()) {
            Tuple firstResult = results.get(0);
            log.info("Available tuple elements: {}", 
                firstResult.getElements().stream()
                    .map(TupleElement::getAlias)
                    .collect(Collectors.joining(", ")));
        }

        return results;
    }

    private Set<String> getAllFieldsFromEntity(Root<Artifact> root) {
        Set<String> allFields = new HashSet<>();
        Class<?> entityClass = root.getJavaType();
        
        // Получаем все базовые поля
        allFields.addAll(getEntityFields(entityClass));
        
        // Получаем все поля из связанных сущностей
        Set<String> relations = getEntityRelations(entityClass);
        for (String relation : relations) {
            Join<?, ?> join = root.join(relation, JoinType.LEFT);
            Set<String> joinEntityFields = getEntityFields(join.getJavaType());
            // Всегда добавляем id для связанной сущности
            allFields.add(relation + ".id");
            joinEntityFields.forEach(field -> allFields.add(relation + "." + field));
        }

        return allFields;
    }

    private boolean isRelation(Class<?> entityClass, String fieldName) {
        return getEntityRelations(entityClass).contains(fieldName);
    }

    private Set<String> getEntityRelations(Class<?> entityClass) {
        return entityRelationsCache.computeIfAbsent(entityClass, this::extractEntityRelations);
    }

    private Set<String> extractEntityRelations(Class<?> entityClass) {
        Set<String> relations = new HashSet<>();
        Class<?> currentClass = entityClass;
        
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(OneToMany.class) ||
                    field.isAnnotationPresent(ManyToOne.class) ||
                    field.isAnnotationPresent(OneToOne.class) ||
                    field.isAnnotationPresent(ManyToMany.class)) {
                    relations.add(field.getName());
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return relations;
    }

    private Set<String> getEntityFields(Class<?> entityClass) {
        return entityFieldsCache.computeIfAbsent(entityClass, this::extractEntityFields);
    }

    private Set<String> extractEntityFields(Class<?> entityClass) {
        Set<String> fields = new HashSet<>();
        Class<?> currentClass = entityClass;
        
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // Пропускаем статические поля и поля с аннотацией @Transient
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    field.isAnnotationPresent(Transient.class)) {
                    continue;
                }

                // Пропускаем поля-коллекции и связи с другими сущностями
                if (Collection.class.isAssignableFrom(field.getType()) ||
                    field.isAnnotationPresent(OneToMany.class) ||
                    field.isAnnotationPresent(ManyToOne.class) ||
                    field.isAnnotationPresent(OneToOne.class) ||
                    field.isAnnotationPresent(ManyToMany.class)) {
                    continue;
                }

                fields.add(field.getName());
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fields;
    }
}