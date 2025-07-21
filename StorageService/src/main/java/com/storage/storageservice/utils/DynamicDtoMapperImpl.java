package com.storage.storageservice.utils;

import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicDtoMapperImpl implements DynamicDtoMapper {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public <T> T mapToDto(Tuple tuple, Set<String> fields, Class<T> dtoClass) {
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

    private <T> void setNestedField(T dto, String fieldPath, Object value,
                                    Map<String, Field> dtoFields) throws Exception {
        String[] parts = fieldPath.split("\\.");
        Object current = dto;
        Map<String, Field> currentFields = dtoFields;

        for (int i = 0; i < parts.length - 1; i++) {
            Field field = currentFields.get(parts[i]);
            if (field == null) continue;

            field.setAccessible(true);
            Object nested = field.get(current);

            if (nested == null) {
                // Обработка списков
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
            current = nested;

            // Обновляем currentFields для следующей итерации
            if (current instanceof List) {
                // Для списков получаем тип элементов списка
                List<?> list = (List<?>) current;
                if (!list.isEmpty()) {
                    currentFields = getCachedFields(list.get(0).getClass());
                } else {
                    // Если список пуст, пытаемся определить тип из поля
                    ParameterizedType listType = (ParameterizedType) field.getGenericType();
                    Class<?> listElementType = (Class<?>) listType.getActualTypeArguments()[0];
                    currentFields = getCachedFields(listElementType);
                }
            } else {
                currentFields = getCachedFields(field.getType());
            }
        }

        Field targetField = currentFields.get(parts[parts.length - 1]);
        if (targetField != null) {
            targetField.setAccessible(true);
            Object convertedValue = convertValue(value, targetField.getType());

            // Если текущий объект - список, добавляем значение в список
            if (current instanceof List) {
                ((List<Object>) current).add(convertedValue);
            } else {
                targetField.set(current, convertedValue);
            }
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        // Простые преобразования типов
        if (targetType == String.class) return value.toString();
        if (targetType == Long.class && value instanceof Integer) return ((Integer) value).longValue();
        if (targetType == Integer.class && value instanceof Long) return ((Long) value).intValue();

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
        if (listType.getGenericSuperclass() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) listType.getGenericSuperclass();
            return (Class<?>) pt.getActualTypeArguments()[0];
        }
        return Object.class;
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
}