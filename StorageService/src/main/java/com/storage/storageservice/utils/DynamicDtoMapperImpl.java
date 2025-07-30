package com.storage.storageservice.utils;

import com.storage.storageservice.dto.DocumentDto;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.nio.ByteBuffer;

@Slf4j
@Service
public class DynamicDtoMapperImpl implements DynamicDtoMapper {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public <T> T mapToDto(List<Tuple> tuples, Set<String> fields, Class<T> dtoClass) {
        if (tuples == null || tuples.isEmpty()) return null;
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            Map<String, Field> dtoFields = getCachedFields(dtoClass);
            
            // Группируем поля по первому уровню вложенности
            Map<String, Set<String>> nestedGroups = new HashMap<>();
            for (String field : fields) {
                String[] parts = field.split("\\.", 2);
                if (parts.length > 1) {
                    nestedGroups.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
                } else {
                    nestedGroups.computeIfAbsent(parts[0], k -> new HashSet<>());
                }
            }

            log.info("Nested groups: {}", nestedGroups);

            // Обрабатываем каждую группу полей
            for (Map.Entry<String, Set<String>> entry : nestedGroups.entrySet()) {
                String fieldName = entry.getKey();
                Set<String> groupFields = entry.getValue();

                log.info("Processing field: {} with nested fields: {}", fieldName, groupFields);

                Field field = dtoFields.get(fieldName);
                if (field == null) {
                    log.warn("Field not found in DTO: {}", fieldName);
                    continue;
                }

                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                if (Collection.class.isAssignableFrom(fieldType)) {
                    log.info("Processing collection field: {}", fieldName);
                    processCollectionField(dto, tuples, fieldName, groupFields, field);
                } else {
                    // Обработка простых полей или вложенных объектов
                    Tuple firstTuple = tuples.get(0);
                    if (groupFields.isEmpty()) {
                        // Проверяем, есть ли поля с префиксом fieldName
                        Set<String> availableFields = firstTuple.getElements().stream()
                            .map(TupleElement::getAlias)
                            .filter(alias -> alias.startsWith(fieldName + "."))
                            .collect(Collectors.toSet());

                        if (!availableFields.isEmpty()) {
                            // Если есть поля с таким префиксом, обрабатываем как вложенный объект
                            log.info("Processing as nested object due to available fields: {}", availableFields);
                            processSingleObject(dto, firstTuple, fieldName, new HashSet<>(), field);
                        } else {
                            // Иначе пробуем обработать как простое поле
                            try {
                                Object value = firstTuple.get(fieldName);
                                log.info("Setting simple field: {} = {}", fieldName, value);
                                if (value != null) {
                                    field.set(dto, convertValue(value, fieldType));
                                }
                            } catch (IllegalArgumentException e) {
                                log.warn("Failed to get value for field: {}", fieldName, e);
                            }
                        }
                    } else {
                        // Вложенный объект с указанными полями
                        processSingleObject(dto, firstTuple, fieldName, groupFields, field);
                    }
                }
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to map to DTO", e);
            throw new RuntimeException("Failed to map to DTO", e);
        }
    }

    private static class FieldNode {
        Map<String, FieldNode> children = new HashMap<>();
        boolean isLeaf = true;

        void addPath(String[] path, int index) {
            if (index >= path.length) return;
            
            isLeaf = false;
            String currentPart = path[index];
            children.computeIfAbsent(currentPart, k -> new FieldNode())
                   .addPath(path, index + 1);
        }

        Set<String> getAllNestedFields() {
            Set<String> fields = new HashSet<>();
            for (Map.Entry<String, FieldNode> entry : children.entrySet()) {
                if (entry.getValue().isLeaf) {
                    fields.add(entry.getKey());
                } else {
                    for (String childField : entry.getValue().getAllNestedFields()) {
                        fields.add(entry.getKey() + "." + childField);
                    }
                }
            }
            return fields;
        }
    }

    private FieldNode buildFieldTree(Set<String> fields) {
        FieldNode root = new FieldNode();
        if (fields == null || fields.isEmpty()) {
            return root;
        }

        for (String field : fields) {
            String[] parts = field.split("\\.");
            root.addPath(parts, 0);
        }
        return root;
    }

    private void processFieldNode(Object dto, Tuple tuple, String prefix, FieldNode node, Map<String, Field> dtoFields) throws Exception {
        for (Map.Entry<String, FieldNode> entry : node.children.entrySet()) {
            String fieldName = entry.getKey();
            String fullPath = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
            
            Field field = dtoFields.get(fieldName);
            if (field == null) continue;

            field.setAccessible(true);
            
            if (entry.getValue().isLeaf) {
                // Простое поле
                if (hasAlias(tuple, fullPath)) {
                    Object value = tuple.get(fullPath);
                    if (value != null) {
                        field.set(dto, convertValue(value, field.getType()));
                    }
                }
            } else if (!Collection.class.isAssignableFrom(field.getType())) {
                // Вложенный объект (не коллекция)
                Object nestedDto = field.getType().getDeclaredConstructor().newInstance();
                processFieldNode(nestedDto, tuple, fullPath, entry.getValue(), getCachedFields(field.getType()));
                field.set(dto, nestedDto);
            }
        }
    }

    private void processCollectionField(Object dto, List<Tuple> tuples, String fieldName, Set<String> nestedFields, Field field) throws Exception {
        Class<?> elementType = getListElementTypeFromField(field);
        
        log.info("Processing collection field: {} of type: {} with {} tuples", fieldName, elementType, tuples.size());
        
        // Get all available aliases for this collection
        Set<String> availableAliases = tuples.get(0).getElements().stream()
            .map(TupleElement::getAlias)
            .filter(alias -> alias.startsWith(fieldName + "."))
            .collect(Collectors.toSet());

        log.info("Available aliases for {}: {}", fieldName, availableAliases);

        // Group tuples by document ID to handle multiple documents correctly
        Map<Object, Tuple> documentGroups = new LinkedHashMap<>();
        String idFieldName = fieldName + ".id";
        
        for (Tuple tuple : tuples) {
            try {
                // Try to get the ID field to group documents
                Object documentId = null;
                if (hasAlias(tuple, idFieldName)) {
                    documentId = tuple.get(idFieldName);
                    log.debug("Found document ID: {} in tuple", documentId);
                }
                
                // If no ID available, create a unique key based on multiple distinguishing fields
                if (documentId == null) {
                    // Try to find other distinguishing document fields
                    List<String> potentialIdFields = List.of(
                        fieldName + ".name",
                        fieldName + ".surname", 
                        fieldName + ".createDateTime",
                        fieldName + ".type",
                        fieldName + ".signer.id"
                    );
                    
                    StringBuilder keyBuilder = new StringBuilder("doc_");
                    boolean foundAnyValue = false;
                    
                    // Use a combination of available fields to create a unique key
                    for (String potentialField : potentialIdFields) {
                        if (hasAlias(tuple, potentialField)) {
                            try {
                                Object value = tuple.get(potentialField);
                                keyBuilder.append(potentialField.substring(fieldName.length() + 1))
                                          .append("=").append(value).append(";");
                                if (value != null) {
                                    foundAnyValue = true;
                                }
                                log.debug("Added distinguishing field {} = {} to composite key", potentialField, value);
                            } catch (Exception e) {
                                // Skip this field
                            }
                        }
                    }
                    
                    // If still no distinguishing values, fall back to tuple index
                    if (!foundAnyValue) {
                        // Last resort: use tuple index
                        keyBuilder.append("tuple_").append(tuples.indexOf(tuple));
                        log.debug("No distinguishing fields found, using tuple index as fallback");
                    }
                    
                    documentId = keyBuilder.toString();
                    log.debug("Generated composite document ID: {}", documentId);
                }
                
                // Only add if we haven't seen this document ID or if it has more complete data
                if (!documentGroups.containsKey(documentId) || hasMoreCompleteData(tuple, documentGroups.get(documentId), availableAliases)) {
                    documentGroups.put(documentId, tuple);
                    log.debug("Added/Updated document with ID: {} from tuple index: {}", documentId, tuples.indexOf(tuple));
                }
            } catch (Exception e) {
                log.warn("Error processing tuple for document grouping", e);
                // Fallback: use tuple index as key
                String fallbackId = "tuple_" + tuples.indexOf(tuple);
                documentGroups.put(fallbackId, tuple);
                log.debug("Used fallback ID: {}", fallbackId);
            }
        }

        log.info("Found {} unique documents in {} tuples", documentGroups.size(), tuples.size());

        // Create list of collection items
        List<Object> items = new ArrayList<>();
        
        // Process each unique document
        for (Map.Entry<Object, Tuple> entry : documentGroups.entrySet()) {
            Object documentId = entry.getKey();
            Tuple tuple = entry.getValue();
            
            log.debug("Processing document with ID: {}", documentId);
            
            Object item = elementType.getDeclaredConstructor().newInstance();
            boolean hasValue = false;

            // Set field values for this document
            for (String alias : availableAliases) {
                if (alias.startsWith(fieldName + ".")) {
                    String fieldPart = alias.substring(fieldName.length() + 1);
                    try {
                        Object value = tuple.get(alias);
                        log.debug("Processing field: {} with value: {} (type: {})", 
                            alias, value, value != null ? value.getClass().getSimpleName() : "null");
                        
                        // Set the field even if the value is null - this is important for nested objects
                        Class<?> leafFieldType = getLeafFieldType(elementType, fieldPart);
                        log.debug("Target field type for {}: {}", fieldPart, leafFieldType.getSimpleName());
                        
                        Object convertedValue = convertValue(value, leafFieldType);
                        log.debug("Converted value: {} (type: {})", convertedValue, 
                            convertedValue != null ? convertedValue.getClass().getSimpleName() : "null");
                        
                        setNestedFieldRecursive(item, fieldPart, convertedValue);
                        
                        // Consider it as having a value even if the value is null - the field exists
                        hasValue = true;
                        log.debug("Successfully set field {} = {} for document {}", fieldPart, convertedValue, documentId);
                    } catch (Exception e) {
                        log.error("Failed to set field {} for document {} in collection {}: {}", 
                            fieldPart, documentId, fieldName, e.getMessage(), e);
                    }
                }
            }
            
            // Always include the document if we processed any fields, even if they were null
            // This ensures documents without signers are still included
            if (hasValue || !availableAliases.isEmpty()) {
                items.add(item);
                log.debug("Added document {} to collection (hasValue: {}, availableAliases: {})", 
                    documentId, hasValue, availableAliases.size());
            } else {
                log.warn("Document {} had no processable fields, skipping", documentId);
            }
        }

        if (!items.isEmpty()) {
            log.info("Setting collection field: {} with {} items", fieldName, items.size());
            field.set(dto, items);
        } else {
            log.info("No items found for collection field: {}", fieldName);
        }
    }
    
    /**
     * Check if one tuple has more complete data than another
     */
    private boolean hasMoreCompleteData(Tuple newTuple, Tuple existingTuple, Set<String> availableAliases) {
        int newNonNullCount = 0;
        int existingNonNullCount = 0;
        
        for (String alias : availableAliases) {
            try {
                if (newTuple.get(alias) != null) newNonNullCount++;
                if (existingTuple.get(alias) != null) existingNonNullCount++;
            } catch (Exception e) {
                // Ignore aliases that don't exist in tuple
            }
        }
        
        return newNonNullCount > existingNonNullCount;
    }

    private void processSingleObject(Object dto, Tuple tuple, String fieldName, Set<String> nestedFields, Field field) throws Exception {
        Class<?> fieldType = field.getType();
        
        // Если нет уточняющих полей, получаем все поля для типа
        if (nestedFields.isEmpty()) {
            nestedFields = getAllFieldsFromClass(fieldType);
            log.info("Using all fields for nested object: {}", nestedFields);
        }

        Object nestedDto = fieldType.getDeclaredConstructor().newInstance();
        boolean hasValue = false;

        // Устанавливаем значения полей
        for (String nestedField : nestedFields) {
            try {
                String fullPath = fieldName + "." + nestedField;
                if (hasAlias(tuple, fullPath)) {
                    Object value = tuple.get(fullPath);
                    log.debug("Setting nested field: {} = {}", fullPath, value);
                    if (value != null) {
                        Field nestedObjField = fieldType.getDeclaredField(nestedField);
                        nestedObjField.setAccessible(true);
                        nestedObjField.set(nestedDto, convertValue(value, nestedObjField.getType()));
                        hasValue = true;
                    }
                } else {
                    log.debug("Alias not found: {}", fullPath);
                }
            } catch (Exception e) {
                log.warn("Failed to set nested field: {}", nestedField, e);
            }
        }

        if (hasValue) {
            log.info("Setting nested object field: {}", fieldName);
            field.set(dto, nestedDto);
        } else {
            log.debug("Skipping nested object with no values set");
        }
    }

    private Set<String> getAllFieldsFromClass(Class<?> clazz) {
        Set<String> fields = new HashSet<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // Пропускаем статические поля
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                // Пропускаем поля-коллекции и связи с другими сущностями
                if (Collection.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                fields.add(field.getName());
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fields;
    }

    private boolean hasAlias(Tuple tuple, String alias) {
        try {
            // Проверяем наличие алиаса в результате
            Set<String> availableAliases = tuple.getElements().stream()
                .map(TupleElement::getAlias)
                .collect(Collectors.toSet());
            
            boolean hasAlias = availableAliases.contains(alias);
            if (!hasAlias) {
                log.debug("Alias {} not found. Available aliases: {}", alias, availableAliases);
            }
            return hasAlias;
        } catch (IllegalArgumentException e) {
            log.debug("Failed to check alias: {}", alias, e);
            return false;
        }
    }

    private static Map<String, Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> fields = new HashMap<>();
            Class<?> currentClass = c;
            while (currentClass != null && currentClass != Object.class) {
                for (Field field : currentClass.getDeclaredFields()) {
                    fields.put(field.getName(), field);
                }
                currentClass = currentClass.getSuperclass();
            }
            return fields;
        });
    }

    private static Class<?> getListElementTypeFromField(Field field) {
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) field.getGenericType();
            java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0) {
                return (Class<?>) typeArguments[0];
            }
        }
        return DocumentDto.class;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        // Safeguard: Don't allow entity objects to be set to DTO fields
        if (value.getClass().getPackageName().contains(".model") && 
            targetType.getSimpleName().endsWith("Dto")) {
            log.warn("Attempted to set entity {} to DTO field of type {}. This should not happen.", 
                value.getClass().getSimpleName(), targetType.getSimpleName());
            return null;
        }

        try {
            // Числовые преобразования
            if (Number.class.isAssignableFrom(value.getClass())) {
                Number numValue = (Number) value;
                
                if (targetType == Integer.class || targetType == int.class) {
                    return numValue.intValue();
                }
                if (targetType == Long.class || targetType == long.class) {
                    return numValue.longValue();
                }
                if (targetType == Double.class || targetType == double.class) {
                    return numValue.doubleValue();
                }
                if (targetType == Float.class || targetType == float.class) {
                    return numValue.floatValue();
                }
                if (targetType == Short.class || targetType == short.class) {
                    return numValue.shortValue();
                }
                if (targetType == Byte.class || targetType == byte.class) {
                    return numValue.byteValue();
                }
                if (targetType == BigDecimal.class) {
                    if (value instanceof BigDecimal) {
                        return value;
                    }
                    if (value instanceof BigInteger) {
                        return new BigDecimal((BigInteger) value);
                    }
                    if (value instanceof Double || value instanceof Float) {
                        return BigDecimal.valueOf(numValue.doubleValue());
                    }
                    return new BigDecimal(numValue.toString());
                }
                if (targetType == BigInteger.class) {
                    if (value instanceof BigInteger) {
                        return value;
                    }
                    if (value instanceof BigDecimal) {
                        return ((BigDecimal) value).toBigInteger();
                    }
                    return BigInteger.valueOf(numValue.longValue());
                }
            }

            // Строковые преобразования
            if (targetType == String.class) {
                return value.toString();
            }
            if (value instanceof String) {
                String strValue = (String) value;
                if (targetType == BigDecimal.class) {
                    return new BigDecimal(strValue);
                }
                if (targetType == BigInteger.class) {
                    return new BigInteger(strValue);
                }
                if (targetType == Integer.class || targetType == int.class) {
                    return Integer.parseInt(strValue);
                }
                if (targetType == Long.class || targetType == long.class) {
                    return Long.parseLong(strValue);
                }
                if (targetType == Double.class || targetType == double.class) {
                    return Double.parseDouble(strValue);
                }
                if (targetType == Float.class || targetType == float.class) {
                    return Float.parseFloat(strValue);
                }
                if (targetType == Boolean.class || targetType == boolean.class) {
                    return Boolean.parseBoolean(strValue);
                }
            }

            // Преобразования даты/времени
            if (targetType == LocalDateTime.class) {
                if (value instanceof Timestamp) {
                    return ((Timestamp) value).toLocalDateTime();
                }
                if (value instanceof java.sql.Date) {
                    return ((java.sql.Date) value).toLocalDate().atStartOfDay();
                }
                if (value instanceof String) {
                    return LocalDateTime.parse((String) value);
                }
            }
            if (targetType == LocalDate.class) {
                if (value instanceof java.sql.Date) {
                    return ((java.sql.Date) value).toLocalDate();
                }
                if (value instanceof Timestamp) {
                    return ((Timestamp) value).toLocalDateTime().toLocalDate();
                }
                if (value instanceof String) {
                    return LocalDate.parse((String) value);
                }
            }
            if (targetType == LocalTime.class) {
                if (value instanceof Time) {
                    return ((Time) value).toLocalTime();
                }
                if (value instanceof Timestamp) {
                    return ((Timestamp) value).toLocalDateTime().toLocalTime();
                }
                if (value instanceof String) {
                    return LocalTime.parse((String) value);
                }
            }

            // UUID преобразования
            if (targetType == UUID.class) {
                if (value instanceof String) {
                    return UUID.fromString((String) value);
                }
                if (value instanceof byte[]) {
                    ByteBuffer bb = ByteBuffer.wrap((byte[]) value);
                    return new UUID(bb.getLong(), bb.getLong());
                }
            }

            // Enum преобразования
            if (targetType.isEnum() && value instanceof String) {
                return Enum.valueOf((Class<? extends Enum>) targetType, (String) value);
            }

            log.warn("Unsupported conversion from {} to {}", value.getClass(), targetType);
            return value;
        } catch (Exception e) {
            log.error("Failed to convert value {} to type {}", value, targetType, e);
            return value;
        }
    }

    private void setNestedField(Object obj, String fieldPath, Object value) throws Exception {
        String[] parts = fieldPath.split("\\.");
        Object currentObj = obj;
        Class<?> currentClass = obj.getClass();

        for (int i = 0; i < parts.length - 1; i++) {
            Field field = currentClass.getDeclaredField(parts[i]);
            field.setAccessible(true);
            Object nestedObj = field.get(currentObj);
            if (nestedObj == null) {
                nestedObj = field.getType().getDeclaredConstructor().newInstance();
                field.set(currentObj, nestedObj);
            }
            currentObj = nestedObj;
            currentClass = field.getType();
        }

        Field leafField = currentClass.getDeclaredField(parts[parts.length - 1]);
        leafField.setAccessible(true);
        leafField.set(currentObj, value);
    }

    private void setNestedFieldRecursive(Object obj, String fieldPath, Object value) throws Exception {
        String[] parts = fieldPath.split("\\.", 2);
        String fieldName = parts[0];
        
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        if (parts.length == 1) {
            // This is the leaf field - set the value directly
            field.set(obj, value);
        } else {
            // This is a nested field - need to create or get the nested object
            Object nestedObj = field.get(obj);
            if (nestedObj == null) {
                // Create a new instance of the nested DTO
                Class<?> fieldType = field.getType();
                
                // Make sure we're creating a DTO, not trying to set an entity
                if (isDtoClass(fieldType)) {
                    nestedObj = fieldType.getDeclaredConstructor().newInstance();
                    field.set(obj, nestedObj);
                    log.debug("Created new nested DTO object of type: {} for field: {}", 
                        fieldType.getSimpleName(), fieldName);
                } else {
                    log.warn("Attempted to create non-DTO nested object of type: {} for field: {}", 
                        fieldType.getSimpleName(), fieldName);
                    return;
                }
            }
            
            // Continue with the remaining path on the nested object
            setNestedFieldRecursive(nestedObj, parts[1], value);
        }
    }
    
    /**
     * Check if a class is a DTO class
     */
    private boolean isDtoClass(Class<?> clazz) {
        return clazz != null && clazz.getSimpleName().endsWith("Dto");
    }

    private Class<?> getLeafFieldType(Class<?> rootClass, String fieldPath) throws Exception {
        String[] parts = fieldPath.split("\\.");
        Class<?> currentClass = rootClass;
        for (String part : parts) {
            Field field = currentClass.getDeclaredField(part);
            currentClass = field.getType();
        }
        return currentClass;
    }
}