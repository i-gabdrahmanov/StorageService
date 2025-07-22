package com.storage.storageservice.repository.impl;

import com.storage.storageservice.model.Artifact;
import com.storage.storageservice.repository.CustomArtifactRepository;
import com.storage.storageservice.utils.CriteriaFieldResolver;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

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

        // Если поля не указаны, получаем все поля из сущности
        if (fields == null || fields.isEmpty()) {
            fields = getAllFieldsFromEntity(root);
        } else {
            // Проверяем каждое поле на необходимость получения всех полей
            Set<String> expandedFields = new HashSet<>();
            for (String field : fields) {
                if (field.contains(".")) {
                    expandedFields.add(field);
                    // Добавляем id для связанной сущности, если его нет
                    String prefix = field.substring(0, field.indexOf("."));
                    expandedFields.add(prefix + ".id");
                } else {
                    // Проверяем, является ли поле связью
                    if (isRelation(root.getJavaType(), field)) {
                        Join<?, ?> join = root.join(field, JoinType.LEFT);
                        Set<String> entityFields = getEntityFields(join.getJavaType());
                        if (!entityFields.isEmpty()) {
                            // Всегда добавляем id для связанной сущности
                            expandedFields.add(field + ".id");
                            entityFields.forEach(entityField -> 
                                expandedFields.add(field + "." + entityField));
                        }
                    } else {
                        expandedFields.add(field);
                    }
                }
            }
            fields = expandedFields;
        }

        List<Selection<?>> selections = fields.stream()
                .map(field -> CriteriaFieldResolver.resolveSelection(root, field, cb))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        query.multiselect(selections)
                .where(cb.equal(root.get("id"), id));

        return em.createQuery(query).getResultList();
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