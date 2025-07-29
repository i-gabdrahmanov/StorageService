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
import com.storage.storageservice.dto.ArtifactDto;
import com.storage.storageservice.dto.EmployeeDto;
import com.storage.storageservice.dto.DocumentDto;
import com.storage.storageservice.dto.SignerDto;

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

        // Если requiredResponseFields пустой — разворачиваем на все поля ArtifactDto
        if (fields == null || fields.isEmpty()) {
            fields = getAllFieldsFromDto(ArtifactDto.class, "");
        } else {
            fields = expandFieldsRecursive(fields, ArtifactDto.class);
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

    // --- Универсальное рекурсивное разворачивание полей ---
    private Set<String> expandFieldsRecursive(Set<String> fields, Class<?> rootDtoClass) {
        Set<String> result = new HashSet<>();
        Map<String, Set<String>> prefixToSpecific = new HashMap<>();
        for (String field : fields) {
            int idx = field.lastIndexOf('.');
            if (idx > 0) {
                String prefix = field.substring(0, idx);
                prefixToSpecific.computeIfAbsent(prefix, k -> new HashSet<>()).add(field);
            }
        }
        for (String field : fields) {
            if (!field.contains(".")) {
                // Если поле без уточняющих — разворачиваем на все поля соответствующего DTO
                getAllFieldsFromDtoRecursive(rootDtoClass, field, result, prefixToSpecific, fields);
            } else if (!prefixToSpecific.containsKey(field)) {
                result.add(field);
            }
        }
        return result;
    }

    // Рекурсивно добавляет все поля для указанного поля (или коллекции)
    private void getAllFieldsFromDtoRecursive(Class<?> dtoClass, String field, Set<String> result, Map<String, Set<String>> prefixToSpecific, Set<String> allFields) {
        Field dtoField = getFieldSafe(dtoClass, field);
        if (dtoField == null) return;
        Class<?> fieldType = dtoField.getType();
        if (Collection.class.isAssignableFrom(fieldType)) {
            Class<?> elementType = getCollectionElementType(dtoField);
            if (elementType != null && isDto(elementType)) {
                for (Field subField : elementType.getDeclaredFields()) {
                    result.add(field + "." + subField.getName());
                }
            }
        } else if (isDto(fieldType)) {
            for (Field subField : fieldType.getDeclaredFields()) {
                result.add(field + "." + subField.getName());
            }
        } else {
            result.add(field);
        }
    }

    // Получить все поля для корневого DTO (для полного разворачивания)
    private Set<String> getAllFieldsFromDto(Class<?> dtoClass, String prefix) {
        Set<String> result = new HashSet<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            String fullName = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            if (Collection.class.isAssignableFrom(fieldType)) {
                Class<?> elementType = getCollectionElementType(field);
                if (elementType != null && isDto(elementType)) {
                    for (Field subField : elementType.getDeclaredFields()) {
                        result.add(fullName + "." + subField.getName());
                    }
                }
            } else if (isDto(fieldType)) {
                for (Field subField : fieldType.getDeclaredFields()) {
                    result.add(fullName + "." + subField.getName());
                }
            } else {
                result.add(fullName);
            }
        }
        return result;
    }

    private boolean isDto(Class<?> clazz) {
        return clazz.getSimpleName().endsWith("Dto");
    }

    private Field getFieldSafe(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
            current = current.getSuperclass();
        }
        return null;
    }

    private Class<?> getCollectionElementType(Field field) {
        if (field.getGenericType() instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType type = (java.lang.reflect.ParameterizedType) field.getGenericType();
            java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                return (Class<?>) typeArguments[0];
            }
        }
        return null;
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