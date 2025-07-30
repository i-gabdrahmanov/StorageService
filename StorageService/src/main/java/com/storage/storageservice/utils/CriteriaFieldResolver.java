package com.storage.storageservice.utils;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CriteriaFieldResolver {
    
    private static final Map<Class<?>, Set<String>> CLASS_FIELDS_CACHE = new HashMap<>();
    
    /**
     * Universal recursive field expansion that works with any DTO structure
     * 
     * @param requiredFields Set of field specifications from the request
     * @param rootDtoClass The root DTO class to start expansion from
     * @return Set of actual fields to select
     */
    public static Set<String> expandFields(Set<String> requiredFields, Class<?> rootDtoClass) {
        Set<String> expandedFields = new HashSet<>();
        
        // Group fields by their prefix to detect parent-child relationships
        Map<String, Set<String>> fieldHierarchy = buildFieldHierarchy(requiredFields);
        
        for (String field : requiredFields) {
            expandedFields.addAll(expandSingleField(field, fieldHierarchy, rootDtoClass, ""));
        }
        
        return expandedFields;
    }
    
    /**
     * Build a hierarchy map to understand parent-child field relationships
     */
    private static Map<String, Set<String>> buildFieldHierarchy(Set<String> requiredFields) {
        Map<String, Set<String>> hierarchy = new HashMap<>();
        
        for (String field : requiredFields) {
            String[] parts = field.split("\\.");
            StringBuilder currentPath = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) currentPath.append(".");
                currentPath.append(parts[i]);
                String path = currentPath.toString();
                
                hierarchy.computeIfAbsent(path, k -> new HashSet<>());
                
                // If this is not the full field, mark that there are more specific fields
                if (i < parts.length - 1) {
                    hierarchy.get(path).add(field);
                }
            }
        }
        
        return hierarchy;
    }
    
    /**
     * Expand a single field based on whether more specific fields exist
     */
    private static Set<String> expandSingleField(String field, Map<String, Set<String>> fieldHierarchy, 
                                               Class<?> rootDtoClass, String currentPrefix) {
        Set<String> result = new HashSet<>();
        
        // Check if there are more specific fields for this field
        boolean hasMoreSpecificFields = fieldHierarchy.getOrDefault(field, Collections.emptySet())
            .stream()
            .anyMatch(specificField -> specificField.startsWith(field + "."));
        
        if (hasMoreSpecificFields) {
            // Don't expand this field - more specific fields will handle it
            return result;
        }
        
        // Check if this field should be expanded to all its sub-fields
        if (shouldExpandField(field, rootDtoClass, currentPrefix)) {
            result.addAll(getAllSubFields(field, rootDtoClass, currentPrefix));
        } else {
            // This is a leaf field, include as-is
            result.add(field);
        }
        
        return result;
    }
    
    /**
     * Determine if a field should be expanded to all its sub-fields
     */
    private static boolean shouldExpandField(String fieldPath, Class<?> rootDtoClass, String currentPrefix) {
        try {
            Class<?> fieldType = getFieldTypeByPath(fieldPath, rootDtoClass);
            return fieldType != null && (isDto(fieldType) || isCollectionOfDto(fieldType));
        } catch (Exception e) {
            log.debug("Could not determine if field should be expanded: {}", fieldPath, e);
            return false;
        }
    }
    
    /**
     * Get all sub-fields for a given field path
     */
    private static Set<String> getAllSubFields(String fieldPath, Class<?> rootDtoClass, String currentPrefix) {
        Set<String> subFields = new HashSet<>();
        
        try {
            Class<?> fieldType = getFieldTypeByPath(fieldPath, rootDtoClass);
            if (fieldType == null) return subFields;
            
            // Handle collection types
            if (isCollectionOfDto(fieldType)) {
                fieldType = getCollectionElementType(fieldType, fieldPath, rootDtoClass);
            }
            
            if (isDto(fieldType)) {
                Set<String> allFieldsInDto = getAllFieldsFromDto(fieldType);
                for (String subField : allFieldsInDto) {
                    subFields.add(fieldPath + "." + subField);
                }
            }
        } catch (Exception e) {
            log.debug("Could not get sub-fields for: {}", fieldPath, e);
        }
        
        return subFields;
    }
    
    /**
     * Get the type of a field by following the path through the DTO structure
     */
    private static Class<?> getFieldTypeByPath(String fieldPath, Class<?> rootDtoClass) {
        String[] parts = fieldPath.split("\\.");
        Class<?> currentType = rootDtoClass;
        
        for (String part : parts) {
            Field field = getFieldFromClass(currentType, part);
            if (field == null) return null;
            
            currentType = field.getType();
            
            // Handle collections
            if (Collection.class.isAssignableFrom(currentType)) {
                currentType = getCollectionElementType(currentType, fieldPath, rootDtoClass, field);
                if (currentType == null) return null;
            }
        }
        
        return currentType;
    }
    
    /**
     * Get all fields from a DTO class (cached for performance)
     */
    private static Set<String> getAllFieldsFromDto(Class<?> dtoClass) {
        return CLASS_FIELDS_CACHE.computeIfAbsent(dtoClass, clazz -> {
            Set<String> fields = new HashSet<>();
            Field[] declaredFields = clazz.getDeclaredFields();
            
            for (Field field : declaredFields) {
                // Skip static fields and synthetic fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                    field.isSynthetic()) {
                    continue;
                }
                fields.add(field.getName());
            }
            
            return fields;
        });
    }
    
    /**
     * Get a field from a class, checking inheritance hierarchy
     */
    private static Field getFieldFromClass(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Get the element type of a collection
     */
    private static Class<?> getCollectionElementType(Class<?> collectionType, String fieldPath, 
                                                   Class<?> rootDtoClass, Field... fieldHint) {
        Field field = fieldHint.length > 0 ? fieldHint[0] : 
                     getFieldFromClass(rootDtoClass, fieldPath.split("\\.")[0]);
        
        if (field != null && field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) field.getGenericType();
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        
        return null;
    }
    
    /**
     * Check if a class is a DTO (by naming convention)
     */
    private static boolean isDto(Class<?> clazz) {
        return clazz != null && clazz.getSimpleName().endsWith("Dto");
    }
    
    /**
     * Check if a type is a collection of DTOs
     */
    private static boolean isCollectionOfDto(Class<?> type) {
        return Collection.class.isAssignableFrom(type);
    }
    
    /**
     * Original method for backward compatibility - uses ArtifactDto as root
     */
    public static Set<String> expandFields(Set<String> requiredFields) {
        try {
            Class<?> artifactDtoClass = Class.forName("com.storage.storageservice.dto.ArtifactDto");
            return expandFields(requiredFields, artifactDtoClass);
        } catch (ClassNotFoundException e) {
            log.error("ArtifactDto class not found", e);
            return requiredFields;
        }
    }
    
    public static Selection<?> resolveSelection(
            From<?, ?> from,
            String fieldPath,
            CriteriaBuilder cb
    ) {
        try {
            String[] parts = fieldPath.split("\\.");
            From<?, ?> currentFrom = from;

            // Для всех частей пути кроме последней
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                // Используем существующий join, если он есть
                boolean foundJoin = false;
                for (Join<?, ?> join : currentFrom.getJoins()) {
                    if (join.getAttribute().getName().equals(part)) {
                        currentFrom = join;
                        foundJoin = true;
                        break;
                    }
                }
                // Если join не найден, создаем новый
                if (!foundJoin) {
                    currentFrom = currentFrom.join(part, JoinType.LEFT);
                }
            }

            // Для последней части просто возвращаем get с алиасом
            Path<?> path = currentFrom.get(parts[parts.length - 1]);
            log.debug("Resolved path {} to {}", fieldPath, path);
            return path.alias(fieldPath);
        } catch (Exception e) {
            log.warn("Failed to resolve selection for path: {}", fieldPath, e);
            return null;
        }
    }
    
    /**
     * Enhanced method to handle dynamic field expansion with smart resolution
     */
    public static List<Selection<?>> resolveSelections(
            From<?, ?> from,
            Set<String> requiredFields,
            CriteriaBuilder cb
    ) {
        // First expand the fields based on the logic
        Set<String> expandedFields = expandFields(requiredFields);
        
        log.info("Original fields: {}", requiredFields);
        log.info("Expanded fields: {}", expandedFields);
        
        // Then resolve each field to a Selection
        return expandedFields.stream()
                .map(field -> resolveSelection(from, field, cb))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
