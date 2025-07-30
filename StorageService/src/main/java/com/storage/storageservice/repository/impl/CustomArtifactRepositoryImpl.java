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

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomArtifactRepositoryImpl implements CustomArtifactRepository {

    private final EntityManager em;
    private final Map<Class<?>, Set<String>> entityFieldsCache = new HashMap<>();
    private final Map<Class<?>, Set<String>> entityRelationsCache = new HashMap<>();

    @Override
    public List<Tuple> findProjectedById(UUID id, Set<String> fields) {
        log.info("Starting query for artifact ID: {} with requested fields: {}", id, fields);
        
        // First, let's verify how many documents exist for this artifact
        try {
            Long documentCount = em.createQuery(
                "SELECT COUNT(d) FROM Document d WHERE d.artifact.id = :artifactId", Long.class)
                .setParameter("artifactId", id)
                .getSingleResult();
            log.info("Found {} documents in database for artifact {}", documentCount, id);
            
            // Also check how many have signers
            Long documentsWithSigners = em.createQuery(
                "SELECT COUNT(d) FROM Document d WHERE d.artifact.id = :artifactId AND d.signer IS NOT NULL", Long.class)
                .setParameter("artifactId", id)
                .getSingleResult();
            log.info("Found {} documents with signers for artifact {}", documentsWithSigners, id);
            
        } catch (Exception e) {
            log.warn("Failed to count documents for verification", e);
        }
        
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createQuery(Tuple.class);
        Root<Artifact> root = query.from(Artifact.class);

        // Use the enhanced field resolution logic
        List<Selection<?>> selections = CriteriaFieldResolver.resolveSelections(root, fields, cb);
        
        log.info("Resolved {} selections from {} requested fields", selections.size(), fields.size());
        
        // Log all the joins that were created
        Set<Join<Artifact, ?>> allJoins = root.getJoins();
        log.info("Created {} joins: {}", allJoins.size(), 
            allJoins.stream()
                .map(join -> join.getAttribute().getName() + "(" + join.getJoinType() + ")")
                .collect(Collectors.joining(", ")));

        // Important: For one-to-many relationships, we need to ensure DISTINCT is used
        // if we're selecting parent fields to avoid duplicates, but NOT if we want all child records
        boolean hasCollectionFields = fields.stream().anyMatch(field -> 
            field.startsWith("documents.") || field.equals("documents"));
        
        log.info("Collection fields detected: {} (fields containing 'documents': {})", 
            hasCollectionFields, 
            fields.stream().filter(f -> f.startsWith("documents") || f.equals("documents")).collect(Collectors.toList()));
        
        query.multiselect(selections)
                .where(cb.equal(root.get("id"), id));

        // If we have collection fields, we don't want DISTINCT because we want all related records
        if (!hasCollectionFields) {
            query.distinct(true);
            log.info("Applied DISTINCT to query since no collection fields detected");
        } else {
            log.info("NOT applying DISTINCT since collection fields detected: we want all related records");
        }

        TypedQuery<Tuple> typedQuery = em.createQuery(query);
        
        // For debugging: log the generated SQL
        try {
            String sqlQuery = typedQuery.unwrap(org.hibernate.query.Query.class).getQueryString();
            log.info("Generated SQL: {}", sqlQuery);
        } catch (Exception e) {
            log.debug("Could not extract SQL query string", e);
        }

        List<Tuple> results = typedQuery.getResultList();
        log.info("Query returned {} tuples for artifact ID: {}", results.size(), id);
        
        // Log details about each tuple for debugging
        for (int i = 0; i < results.size(); i++) {
            Tuple tuple = results.get(i);
            log.debug("Tuple {}: documents.id = {}, documents.signer.id = {}", 
                i, 
                tryGetValue(tuple, "documents.id"),
                tryGetValue(tuple, "documents.signer.id"));
        }
        
        if (!results.isEmpty()) {
            Tuple firstResult = results.get(0);
            Set<String> availableAliases = firstResult.getElements().stream()
                .map(TupleElement::getAlias)
                .collect(Collectors.toSet());
            log.info("Available tuple aliases: {}", String.join(", ", availableAliases));
            
            // Log some sample values for debugging
            if (results.size() > 1) {
                log.info("Multiple tuples detected - this is expected for one-to-many relationships");
                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    Tuple tuple = results.get(i);
                    log.debug("Tuple {}: documents.id = {}", i, 
                        tryGetValue(tuple, "documents.id"));
                }
            } else if (results.size() == 1 && hasCollectionFields) {
                log.warn("Only 1 tuple returned but collection fields were requested - this might indicate a problem");
                log.warn("Expected multiple tuples for one-to-many relationship");
            }
        }

        return results;
    }
    
    /**
     * Helper method to safely get a value from a tuple
     */
    private Object tryGetValue(Tuple tuple, String alias) {
        try {
            return tuple.get(alias);
        } catch (Exception e) {
            return "N/A";
        }
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
                getAllFieldsFromDtoRecursive(rootDtoClass, field, result, prefixToSpecific, fields);
            } else {
                // Проверяем, не является ли поле вложенной связью без уточняющих
                String[] parts = field.split("\\.");
                Class<?> currentDto = rootDtoClass;
                boolean isRelation = false;
                for (int i = 0; i < parts.length - 1; i++) {
                    Field f = getFieldSafe(currentDto, parts[i]);
                    if (f == null) break;
                    Class<?> t = f.getType();
                    if (Collection.class.isAssignableFrom(t)) {
                        t = getCollectionElementType(f);
                    }
                    if (isDto(t)) {
                        currentDto = t;
                        isRelation = true;
                    } else {
                        isRelation = false;
                        break;
                    }
                }
                if (isRelation && !prefixToSpecific.containsKey(field)) {
                    // Разворачиваем на все поля соответствующего DTO
                    Field lastField = getFieldSafe(currentDto, parts[parts.length - 1]);
                    if (lastField != null) {
                        Class<?> lastType = lastField.getType();
                        if (Collection.class.isAssignableFrom(lastType)) {
                            lastType = getCollectionElementType(lastField);
                        }
                        if (isDto(lastType)) {
                            for (Field subField : lastType.getDeclaredFields()) {
                                result.add(field + "." + subField.getName());
                            }
                        }
                    }
                } else if (!prefixToSpecific.containsKey(field)) {
                    result.add(field);
                }
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
                // Всегда добавляем id для коллекции
                result.add(field + ".id");
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