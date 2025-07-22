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

        Set<String> getChildFields() {
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
        
        log.info("Processing collection field: {} of type: {}", fieldName, elementType);
        
        // Если нет уточняющих полей, получаем все поля для типа
        if (nestedFields.isEmpty()) {
            nestedFields = getAllFieldsFromClass(elementType);
            log.info("Using all fields for collection elements: {}", nestedFields);
        }
        
        // Создаем список объектов
        List<Object> items = new ArrayList<>();
        Map<Object, Object> processedItems = new HashMap<>();

        for (Tuple tuple : tuples) {
            try {
                // Получаем идентификатор для группировки (например, documents.id)
                Object itemId = tuple.get(fieldName + ".id");
                log.info("Processing item with ID: {}", itemId);
                
                if (itemId == null) {
                    log.debug("Skipping item with null ID");
                    continue;
                }

                // Если объект с таким id уже обработан, пропускаем
                if (processedItems.containsKey(itemId)) {
                    log.debug("Skipping already processed item with ID: {}", itemId);
                    continue;
                }

                Object item = elementType.getDeclaredConstructor().newInstance();
                boolean hasValue = false;

                // Устанавливаем значения полей
                for (String nestedField : nestedFields) {
                    try {
                        String fullPath = fieldName + "." + nestedField;
                        if (hasAlias(tuple, fullPath)) {
                            Object value = tuple.get(fullPath);
                            log.debug("Setting nested field: {} = {}", fullPath, value);
                            if (value != null) {
                                Field itemField = elementType.getDeclaredField(nestedField);
                                itemField.setAccessible(true);
                                itemField.set(item, convertValue(value, itemField.getType()));
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
                    items.add(item);
                    processedItems.put(itemId, item);
                    log.info("Added item with ID: {}", itemId);
                } else {
                    log.debug("Skipping item with no values set");
                }
            } catch (IllegalArgumentException e) {
                log.warn("Failed to process tuple for collection field: {}", fieldName, e);
            }
        }

        if (!items.isEmpty()) {
            log.info("Setting collection field: {} with {} items", fieldName, items.size());
            field.set(dto, items);
        } else {
            log.warn("No items found for collection field: {}", fieldName);
        }
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
        
        // Добавьте здесь дополнительные преобразования типов, если необходимо
        
        return value;
    }
}