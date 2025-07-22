package com.storage.storageservice.utils;

import com.storage.storageservice.dto.DocumentDto;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.persistence.TupleElement;

@Service
public class DynamicDtoMapperImpl implements DynamicDtoMapper {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();


    public <T> T mapToDto(List<Tuple> tuples, Set<String> fields, Class<T> dtoClass) {
        if (tuples == null || tuples.isEmpty()) return null;
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            Map<String, Field> dtoFields = getCachedFields(dtoClass);
            
            // Группируем поля по префиксу (например, documents.*, employee.*)
            Map<String, Set<String>> fieldGroups = new HashMap<>();
            for (String field : fields) {
                String prefix = field.contains(".") ? field.substring(0, field.indexOf(".")) : field;
                fieldGroups.computeIfAbsent(prefix, k -> new HashSet<>()).add(field);
            }

            // Проверяем каждую группу полей
            for (Map.Entry<String, Set<String>> entry : fieldGroups.entrySet()) {
                String prefix = entry.getKey();
                Set<String> groupFields = entry.getValue();

                // Если это documents и нет уточняющих полей, получаем все поля из DocumentDto
                if ("documents".equals(prefix) && groupFields.size() == 1 && groupFields.contains("documents")) {
                    groupFields = getAllFieldsWithPrefix("documents", DocumentDto.class);
                }

                // Обрабатываем поля в зависимости от их типа
                if (groupFields.stream().anyMatch(f -> f.contains("."))) {
                    // Обработка вложенных полей
                    processNestedFields(dto, tuples, groupFields, dtoFields);
                } else {
                    // Обработка простых полей
                    Tuple firstTuple = tuples.get(0);
                    for (String field : groupFields) {
                        try {
                            Object value = firstTuple.get(field);
                            if (value != null) {
                                setField(dto, field, value, dtoFields);
                            }
                        } catch (IllegalArgumentException ignored) {
                            // Поле не найдено в Tuple
                        }
                    }
                }
            }

            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map to DTO", e);
        }
    }

    private <T> void setNestedField(T dto, String fieldPath, Object value,
                                    Map<String, Field> dtoFields) throws Exception {
        String[] parts = fieldPath.split("\\.");
        Object current = dto;
        Map<String, Field> currentFields = dtoFields;
        String currentPath = "";

        for (int i = 0; i < parts.length - 1; i++) {
            Field field = currentFields.get(parts[i]);
            if (field == null) continue;

            field.setAccessible(true);
            Object nested = field.get(current);
            currentPath = currentPath.isEmpty() ? parts[i] : currentPath + "." + parts[i];

            if (nested == null) {
                if (List.class.isAssignableFrom(field.getType())) {
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    Class<?> listElementType = (Class<?>) listType.getActualTypeArguments()[0];
                    nested = new ArrayList<>();
                    field.set(current, nested);
                } else {
                    nested = field.getType().getDeclaredConstructor().newInstance();
                    field.set(current, nested);
                }
            }

            if (nested instanceof List) {
                List<?> list = (List<?>) nested;
                Class<?> elementType = getListElementType(field.getType());

                // Собираем объекты для коллекции
                if (value instanceof List) {
                    List<Tuple> tuples = ((List<?>) value).stream()
                            .filter(Tuple.class::isInstance)
                            .map(Tuple.class::cast)
                            .collect(Collectors.toList());
                    ((List<Object>) nested).addAll(buildObjectsFromTuples(tuples, elementType, currentPath));
                } else if (value instanceof Tuple) {
                    ((List<Object>) nested).addAll(
                            buildObjectsFromTuples(Collections.singletonList((Tuple) value), elementType, currentPath));
                }
                return;
            }

            current = nested;
            currentFields = getCachedFields(field.getType());
        }

        Field targetField = currentFields.get(parts[parts.length - 1]);
        if (targetField != null) {
            targetField.setAccessible(true);
            Object convertedValue = convertValue(value, targetField.getType());
            if (current instanceof List) {
                ((List<Object>) current).add(convertedValue);
            } else {
                targetField.set(current, convertedValue);
            }
        }
    }

    private <T> List<T> buildObjectsFromTuplesWithAllFields(List<Tuple> tuples, Class<T> elementType, String pathPrefix) throws Exception {
        Map<String, T> objectMap = new LinkedHashMap<>();
        
        for (Tuple tuple : tuples) {
            Object id = null;
            try {
                id = tuple.get(pathPrefix + ".id");
            } catch (IllegalArgumentException e) {
                id = UUID.randomUUID().toString();
            }
            
            if (id == null) continue;

            // Создаем объект через рефлексию
            @SuppressWarnings("unchecked")
            T instance = (T) elementType.getDeclaredConstructor().newInstance();

            // Заполняем все доступные поля из Tuple
            for (Field field : elementType.getDeclaredFields()) {
                String fieldPath = pathPrefix + "." + field.getName();
                try {
                    Object fieldValue = tuple.get(fieldPath);
                    if (fieldValue != null) {
                        field.setAccessible(true);
                        // Конвертируем значение в нужный тип
                        Object convertedValue = convertValue(fieldValue, field.getType());
                        field.set(instance, convertedValue);
                    }
                } catch (Exception ignored) {
                }
            }

            objectMap.put(id.toString(), instance);
        }

        return new ArrayList<>(objectMap.values());
    }

    private <T> List<T> buildObjectsFromTuples(List<Tuple> tuples, Class<T> elementType, String pathPrefix) throws Exception {
        Map<String, T> objectMap = new LinkedHashMap<>();
        
        System.out.println("Building objects for type: " + elementType);
        System.out.println("Prefix: " + pathPrefix);
        
        for (Tuple tuple : tuples) {
            // Выводим все доступные алиасы в tuple
            System.out.println("Available aliases in tuple:");
            for (TupleElement<?> element : tuple.getElements()) {
                System.out.println(" - " + element.getAlias() + ": " + tuple.get(element.getAlias()));
            }
            
            Object id = null;
            try {
                id = tuple.get(pathPrefix + ".id");
            } catch (IllegalArgumentException e) {
                id = UUID.randomUUID().toString();
            }
            
            if (id == null) continue;

            @SuppressWarnings("unchecked")
            T instance = (T) elementType.getDeclaredConstructor().newInstance();

            // Заполняем все доступные поля из Tuple
            System.out.println("Setting fields for instance of type: " + instance.getClass());
            for (Field field : elementType.getDeclaredFields()) {
                String fieldPath = pathPrefix + "." + field.getName();
                try {
                    Object fieldValue = tuple.get(fieldPath);
                    System.out.println(" - Setting field: " + field.getName() + " with value: " + fieldValue);
                    if (fieldValue != null) {
                        field.setAccessible(true);
                        Object convertedValue = convertValue(fieldValue, field.getType());
                        System.out.println("   Converted value: " + convertedValue + " of type: " + (convertedValue != null ? convertedValue.getClass() : "null"));
                        field.set(instance, convertedValue);
                    }
                } catch (Exception e) {
                    System.out.println("   Failed to set field: " + field.getName() + " - " + e.getMessage());
                }
            }

            objectMap.put(id.toString(), instance);
        }

        List<T> result = new ArrayList<>(objectMap.values());
        System.out.println("Built " + result.size() + " objects");
        return result;
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        // Простые преобразования типов
        if (targetType == String.class) return value.toString();
        if (targetType == Long.class && value instanceof Number) return ((Number) value).longValue();
        if (targetType == Integer.class && value instanceof Number) return ((Number) value).intValue();
        if (targetType == Double.class && value instanceof Number) return ((Number) value).doubleValue();
        if (targetType == Float.class && value instanceof Number) return ((Number) value).floatValue();
        if (targetType == Boolean.class && value instanceof String) return Boolean.valueOf((String) value);
        if (targetType.isEnum() && value instanceof String) {
            return Enum.valueOf((Class<? extends Enum>) targetType, (String) value);
        }

        // Обработка списков
        if (List.class.isAssignableFrom(targetType) && value instanceof List) {
            List<?> sourceList = (List<?>) value;
            List<Object> targetList = new ArrayList<>();
            for (Object item : sourceList) {
                targetList.add(convertValue(item, getListElementType(targetType)));
            }
            return targetList;
        }

        return value;
    }

    private static Class<?> getListElementType(Class<?> listType) {
        try {
            // Получаем generic type для поля
            if (listType.getGenericSuperclass() instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) listType.getGenericSuperclass();
                return (Class<?>) pt.getActualTypeArguments()[0];
            }
            
            // Проверяем generic interfaces
            for (java.lang.reflect.Type type : listType.getGenericInterfaces()) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) type;
                    return (Class<?>) pt.getActualTypeArguments()[0];
                }
            }
            
            return DocumentDto.class;
        } catch (Exception e) {
            return DocumentDto.class;
        }
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

    private static Map<String, Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> fields = new HashMap<>();
            // Включаем поля суперклассов
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

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }

    private Set<String> getAllFields(Class<?> type) {
        Set<String> fields = new HashSet<>();
        for (Field field : type.getDeclaredFields()) {
            fields.add(field.getName());
        }
        return fields;
    }

    private Set<String> getAllFieldsWithPrefix(String prefix, Class<?> dtoClass) {
        Set<String> fields = new HashSet<>();
        for (Field field : dtoClass.getDeclaredFields()) {
            fields.add(prefix + "." + field.getName());
        }
        return fields;
    }

    private void setField(Object dto, String fieldName, Object value, Map<String, Field> dtoFields) {
        try {
            Field field = dtoFields.get(fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(dto, convertValue(value, field.getType()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private void processNestedFields(Object dto, List<Tuple> tuples, Set<String> fields, Map<String, Field> dtoFields) {
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

        // Обрабатываем каждую группу вложенных полей
        for (Map.Entry<String, Set<String>> entry : nestedGroups.entrySet()) {
            String fieldName = entry.getKey();
            Set<String> nestedFields = entry.getValue();
            
            Field field = dtoFields.get(fieldName);
            if (field == null) continue;
            
            field.setAccessible(true);
            Class<?> fieldType = field.getType();

            try {
                if (Collection.class.isAssignableFrom(fieldType)) {
                    // Обработка коллекций
                    processCollectionField(dto, tuples, fieldName, nestedFields, field);
                } else {
                    // Обработка одиночного объекта
                    processSingleObject(dto, tuples.get(0), fieldName, nestedFields, field);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to process nested field: " + fieldName, e);
            }
        }
    }

    private void processCollectionField(Object dto, List<Tuple> tuples, String fieldName, Set<String> nestedFields, Field field) throws Exception {
        Class<?> elementType = getListElementTypeFromField(field);
        
        // Если нет уточняющих полей, получаем все поля для типа
        if (nestedFields.isEmpty()) {
            nestedFields = getAllFieldsFromClass(elementType);
        }
        
        // Создаем список объектов
        List<Object> items = new ArrayList<>();
        Map<Object, Object> processedItems = new HashMap<>();

        for (Tuple tuple : tuples) {
            try {
                // Получаем идентификатор для группировки (например, documents.id)
                Object itemId = tuple.get(fieldName + ".id");
                if (itemId == null) continue;

                // Если объект с таким id уже обработан, пропускаем
                if (processedItems.containsKey(itemId)) continue;

                Object item = elementType.getDeclaredConstructor().newInstance();
                boolean hasValue = false;

                // Устанавливаем значения полей
                for (String nestedField : nestedFields) {
                    try {
                        String fullPath = fieldName + "." + nestedField;
                        // Проверяем, есть ли такой алиас в результате
                        if (hasAlias(tuple, fullPath)) {
                            Object value = tuple.get(fullPath);
                            if (value != null) {
                                Field itemField = elementType.getDeclaredField(nestedField);
                                itemField.setAccessible(true);
                                itemField.set(item, convertValue(value, itemField.getType()));
                                hasValue = true;
                            }
                        }
                    } catch (Exception ignored) {
                        // Игнорируем ошибки для отдельных полей
                    }
                }

                if (hasValue) {
                    items.add(item);
                    processedItems.put(itemId, item);
                }
            } catch (IllegalArgumentException ignored) {
                // Игнорируем записи без id
            }
        }

        if (!items.isEmpty()) {
            field.set(dto, items);
        }
    }

    private boolean hasAlias(Tuple tuple, String alias) {
        try {
            // Проверяем наличие алиаса в результате
            tuple.get(alias);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void processSingleObject(Object dto, Tuple tuple, String fieldName, Set<String> nestedFields, Field field) throws Exception {
        Class<?> fieldType = field.getType();
        
        // Если нет уточняющих полей, получаем все поля для типа
        if (nestedFields.isEmpty()) {
            nestedFields = getAllFieldsFromClass(fieldType);
        }

        Object nestedDto = fieldType.getDeclaredConstructor().newInstance();
        boolean hasValue = false;

        // Устанавливаем значения полей
        for (String nestedField : nestedFields) {
            try {
                String fullPath = fieldName + "." + nestedField;
                // Проверяем, есть ли такой алиас в результате
                if (hasAlias(tuple, fullPath)) {
                    Object value = tuple.get(fullPath);
                    if (value != null) {
                        Field nestedObjField = fieldType.getDeclaredField(nestedField);
                        nestedObjField.setAccessible(true);
                        nestedObjField.set(nestedDto, convertValue(value, nestedObjField.getType()));
                        hasValue = true;
                    }
                }
            } catch (Exception ignored) {
                // Игнорируем ошибки для отдельных полей
            }
        }

        if (hasValue) {
            field.set(dto, nestedDto);
        }
    }

    private Set<String> getAllFieldsFromClass(Class<?> clazz) {
        Set<String> fields = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            fields.add(field.getName());
        }
        return fields;
    }
}