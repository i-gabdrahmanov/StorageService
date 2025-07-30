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
        
        log.debug("Expanding field: {} with hierarchy: {}", field, fieldHierarchy.keySet());
        
        // Check if there are more specific fields for this field
        boolean hasMoreSpecificFields = fieldHierarchy.getOrDefault(field, Collections.emptySet())
            .stream()
            .anyMatch(specificField -> specificField.startsWith(field + "."));
        
        log.debug("Field {} has more specific fields: {}", field, hasMoreSpecificFields);
        
        if (hasMoreSpecificFields) {
            // Don't expand this field - more specific fields will handle it
            log.debug("Skipping expansion of {} due to more specific fields", field);
            return result;
        }
        
        // Check if this field should be expanded to all its sub-fields
        boolean shouldExpand = shouldExpandField(field, rootDtoClass, currentPrefix);
        log.debug("Field {} should expand: {}", field, shouldExpand);
        
        if (shouldExpand) {
            Set<String> subFields = getAllSubFields(field, rootDtoClass, currentPrefix);
            result.addAll(subFields);
            log.debug("Expanded field {} to sub-fields: {}", field, subFields);
            
            // Special case: if this is a nested document field (like documents.signer), 
            // always ensure the parent document ID is included for proper grouping
            if (field.startsWith("documents.") && !field.equals("documents.id")) {
                result.add("documents.id");
                log.debug("Added documents.id for proper document grouping when expanding {}", field);
            }
        } else {
            // This is a leaf field, include as-is
            result.add(field);
            log.debug("Added field {} as leaf field", field);
            
            // Also ensure documents.id is included if this is a documents sub-field
            if (field.startsWith("documents.") && !field.equals("documents.id")) {
                result.add("documents.id");
                log.debug("Added documents.id for proper document grouping for leaf field {}", field);
            }
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
        return getAllSubFieldsWithDepth(fieldPath, rootDtoClass, currentPrefix, 0);
    }
    
    /**
     * Get all sub-fields for a given field path with depth control
     */
    private static Set<String> getAllSubFieldsWithDepth(String fieldPath, Class<?> rootDtoClass, String currentPrefix, int depth) {
        // Prevent infinite recursion - limit depth to 3 levels
        if (depth > 3) {
            log.debug("Maximum expansion depth reached for field: {}, stopping recursion", fieldPath);
            return new HashSet<>();
        }
        
        Set<String> subFields = new HashSet<>();
        
        log.debug("Getting sub-fields for: {} from root class: {} (depth: {})", fieldPath, rootDtoClass.getSimpleName(), depth);
        
        try {
            Class<?> fieldType = getFieldTypeByPath(fieldPath, rootDtoClass);
            log.debug("Field type for {}: {}", fieldPath, fieldType != null ? fieldType.getSimpleName() : "null");
            
            if (fieldType == null) return subFields;
            
            // Handle collection types
            if (isCollectionOfDto(fieldType)) {
                log.debug("Field {} is a collection of DTOs", fieldPath);
                fieldType = getCollectionElementType(fieldType, fieldPath, rootDtoClass);
                log.debug("Collection element type: {}", fieldType != null ? fieldType.getSimpleName() : "null");
            }
            
            if (isDto(fieldType)) {
                Set<String> allFieldsInDto = getAllFieldsFromDto(fieldType);
                log.debug("All fields in DTO {}: {}", fieldType.getSimpleName(), allFieldsInDto);
                
                // Always ensure ID is included for collection items to enable proper grouping
                boolean isCollectionField = fieldPath.contains(".") && 
                    fieldPath.split("\\.")[0].endsWith("s"); // Simple heuristic for collections like "documents", "children", etc.
                
                if (isCollectionField) {
                    subFields.add(fieldPath + ".id");
                    log.debug("Added ID field for collection: {}.id", fieldPath);
                }
                
                for (String subField : allFieldsInDto) {
                    String fullSubField = fieldPath + "." + subField;
                    
                    // Prevent circular references: don't include documents field within documents expansion
                    if (fieldPath.startsWith("documents.") && subField.equals("documents")) {
                        log.debug("Skipping circular reference: {} -> {}", fieldPath, fullSubField);
                        continue;
                    }
                    
                    // Prevent other potential circular references
                    if (isCircularReference(fieldPath, subField)) {
                        log.debug("Detected circular reference: {} -> {}, skipping", fieldPath, fullSubField);
                        continue;
                    }
                    
                    // Check if this sub-field is itself a DTO that needs expansion
                    try {
                        Class<?> subFieldType = getFieldTypeByPath(fullSubField, rootDtoClass);
                        if (isDto(subFieldType)) {
                            log.debug("Sub-field {} is a DTO, expanding it further (depth: {})", fullSubField, depth + 1);
                            // Recursively expand DTO sub-fields with increased depth
                            Set<String> nestedFields = getAllSubFieldsWithDepth(fullSubField, rootDtoClass, currentPrefix, depth + 1);
                            subFields.addAll(nestedFields);
                            log.debug("Expanded DTO sub-field {} to: {}", fullSubField, nestedFields);
                        } else {
                            subFields.add(fullSubField);
                            log.debug("Added primitive sub-field: {}", fullSubField);
                        }
                    } catch (Exception e) {
                        // If we can't determine the type, just add it as-is
                        subFields.add(fullSubField);
                        log.debug("Added sub-field (type unknown): {}", fullSubField);
                    }
                }
                
                log.debug("Total sub-fields for {} (depth: {}): {}", fieldPath, depth, subFields);
            } else {
                log.debug("Field {} is not a DTO, type: {}", fieldPath, fieldType.getSimpleName());
            }
        } catch (Exception e) {
            log.warn("Could not get sub-fields for: {}", fieldPath, e);
        }
        
        return subFields;
    }
    
    /**
     * Check for circular references to prevent infinite loops
     */
    private static boolean isCircularReference(String currentPath, String subField) {
        String[] pathParts = currentPath.split("\\.");
        
        // Check if we're about to create a loop back to a parent in the path
        for (String part : pathParts) {
            if (part.equals(subField)) {
                return true;
            }
        }
        
        return false;
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
        log.debug("Getting collection element type for field: {} in class: {}", fieldPath, rootDtoClass.getSimpleName());
        
        Field field = fieldHint.length > 0 ? fieldHint[0] : 
                     getFieldFromClass(rootDtoClass, fieldPath.split("\\.")[0]);
        
        log.debug("Found field: {} for path: {}", field != null ? field.getName() : "null", fieldPath);
        
        if (field != null && field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) field.getGenericType();
            Type[] typeArgs = paramType.getActualTypeArguments();
            log.debug("Type arguments for {}: {} (count: {})", fieldPath, typeArgs.length > 0 ? typeArgs[0] : "none", typeArgs.length);
            
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                Class<?> elementType = (Class<?>) typeArgs[0];
                log.debug("Collection element type for {}: {}", fieldPath, elementType.getSimpleName());
                return elementType;
            }
        } else {
            log.debug("Field {} has no generic type or is not parameterized", fieldPath);
        }
        
        log.debug("Could not determine collection element type for {}, returning null", fieldPath);
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
    
    /**
     * Enhanced method to handle dynamic field expansion with smart resolution
     * This version properly handles one-to-many relationships like multiple documents
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
        
        // Track joins to avoid duplicate joins for the same path
        Map<String, From<?, ?>> createdJoins = new HashMap<>();
        
        // Then resolve each field to a Selection
        return expandedFields.stream()
                .map(field -> resolveSelectionWithJoinTracking(from, field, cb, createdJoins))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Enhanced resolveSelection that tracks joins to prevent duplicate joins
     * and properly handles one-to-many relationships
     */
    public static Selection<?> resolveSelectionWithJoinTracking(
            From<?, ?> from,
            String fieldPath,
            CriteriaBuilder cb,
            Map<String, From<?, ?>> joinTracker
    ) {
        try {
            String[] parts = fieldPath.split("\\.");
            From<?, ?> currentFrom = from;
            StringBuilder currentPath = new StringBuilder();

            // For all parts of the path except the last one, create or reuse joins
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (i > 0) currentPath.append(".");
                currentPath.append(part);
                String joinPath = currentPath.toString();
                
                // Check if we already created a join for this path
                if (joinTracker.containsKey(joinPath)) {
                    currentFrom = joinTracker.get(joinPath);
                    log.debug("Reusing existing join for path: {}", joinPath);
                } else {
                    // Check for existing joins in the current From
                    boolean foundExistingJoin = false;
                    for (Join<?, ?> existingJoin : currentFrom.getJoins()) {
                        if (existingJoin.getAttribute().getName().equals(part)) {
                            currentFrom = existingJoin;
                            joinTracker.put(joinPath, currentFrom);
                            foundExistingJoin = true;
                            log.debug("Found and reused existing join for: {}", part);
                            break;
                        }
                    }
                    
                    // If no existing join found, create a new one
                    if (!foundExistingJoin) {
                        // For one-to-many relationships (like documents), use LEFT JOIN to get all related records
                        JoinType joinType = JoinType.LEFT;
                        
                        // Special handling for collection relationships
                        if (isCollectionField(currentFrom, part)) {
                            log.debug("Creating LEFT JOIN for collection field: {}", part);
                            joinType = JoinType.LEFT;
                        }
                        
                        Join<?, ?> newJoin = currentFrom.join(part, joinType);
                        currentFrom = newJoin;
                        joinTracker.put(joinPath, currentFrom);
                        log.debug("Created new {} join for: {} (full path: {})", joinType, part, joinPath);
                    }
                }
            }

            // For the last part, get the field path
            String lastPart = parts[parts.length - 1];
            Path<?> path = currentFrom.get(lastPart);
            Selection<?> selection = path.alias(fieldPath);
            
            log.debug("Resolved field path {} to selection with alias {}", fieldPath, fieldPath);
            return selection;
            
        } catch (Exception e) {
            log.warn("Failed to resolve selection for path: {} - Error: {}", fieldPath, e.getMessage());
            log.debug("Full exception for path: {}", fieldPath, e);
            return null;
        }
    }
    
    /**
     * Check if a field is a collection field (one-to-many relationship)
     */
    private static boolean isCollectionField(From<?, ?> from, String fieldName) {
        try {
            // Get the attribute to check if it's a collection using the entity type
            jakarta.persistence.metamodel.EntityType<?> entityType = 
                (jakarta.persistence.metamodel.EntityType<?>) from.getModel();
            
            jakarta.persistence.metamodel.Attribute<?, ?> attribute = 
                entityType.getAttribute(fieldName);
            
            return attribute.isCollection();
        } catch (Exception e) {
            log.debug("Could not determine if field {} is collection, defaulting to false", fieldName);
            return false;
        }
    }
    
    public static Selection<?> resolveSelection(
            From<?, ?> from,
            String fieldPath,
            CriteriaBuilder cb
    ) {
        // Use the enhanced version with empty join tracker for backward compatibility
        return resolveSelectionWithJoinTracking(from, fieldPath, cb, new HashMap<>());
    }
}
