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

    public <T> T mapToDto(jakarta.persistence.Tuple tuple, Set<String> fields, Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            Map<String, Field> dtoFields = getCachedFields(dtoClass);

            // Если fields пуст или null, используем все поля из DTO
            if (fields == null || fields.isEmpty()) {
                fields = dtoFields.keySet();
            }

            for (String fieldPath : fields) {
                try {
                    Object value = tuple.get(fieldPath);
                    if (value != null) {
                        setNestedField(dto, fieldPath, value, dtoFields);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Поле не найдено в Tuple
                }
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map to DTO", e);
        }
    }

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

            // Обрабатываем каждую группу полей
            for (Map.Entry<String, Set<String>> group : fieldGroups.entrySet()) {
                String prefix = group.getKey();
                Field field = dtoFields.get(prefix);
                if (field == null) continue;

                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                if (List.class.isAssignableFrom(fieldType)) {
                    // Если это список, получаем тип элементов из generic типа поля
                    Class<?> elementType = getListElementType(fieldType);
                    System.out.println("Processing list field: " + prefix + " with element type: " + elementType);
                    
                    // Проверяем, есть ли вложенные поля
                    boolean hasNestedFields = group.getValue().stream()
                            .anyMatch(f -> f.contains(".") && f.startsWith(prefix + "."));
                    
                    List<?> items;
                    if (hasNestedFields) {
                        items = buildObjectsFromTuples(tuples, elementType, prefix);
                    } else {
                        items = buildObjectsFromTuplesWithAllFields(tuples, elementType, prefix);
                    }
                    
                    System.out.println("Built items: " + items);
                    if (!items.isEmpty()) {
                        System.out.println("First item type: " + items.get(0).getClass());
                    }
                    
                    field.set(dto, items);
                } else {
                    // Для обычных полей
                    try {
                        // Проверяем, есть ли вложенные поля
                        Set<String> groupFields = group.getValue();
                        if (groupFields.size() == 1 && groupFields.contains(prefix)) {
                            // Простое поле без вложенности
                            Object value = tuples.get(0).get(prefix);
                            if (value != null) {
                                field.set(dto, convertValue(value, fieldType));
                            }
                        } else {
                            // Вложенный объект (например, employee с полями)
                            Object nestedDto = fieldType.getDeclaredConstructor().newInstance();
                            boolean hasAnyValue = false;
                            
                            for (String fullPath : groupFields) {
                                if (fullPath.startsWith(prefix + ".")) {
                                    try {
                                        Object value = tuples.get(0).get(fullPath);
                                        if (value != null) {
                                            String fieldName = fullPath.substring(prefix.length() + 1);
                                            Field nestedField = fieldType.getDeclaredField(fieldName);
                                            nestedField.setAccessible(true);
                                            nestedField.set(nestedDto, convertValue(value, nestedField.getType()));
                                            hasAnyValue = true;
                                        }
                                    } catch (IllegalArgumentException ignored) {
                                        // Поле не найдено в Tuple
                                    }
                                }
                            }
                            
                            if (hasAnyValue) {
                                field.set(dto, nestedDto);
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Поле не найдено в Tuple
                    }
                }
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map to DTO: " + e.getMessage(), e);
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
}