package com.storage.storageservice.utils;

import jakarta.persistence.Tuple;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicDtoMapperImpl implements DynamicDtoMapper {
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public <T> T mapToDto(Tuple tuple, Set<String> fields, Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            Map<String, Field> dtoFields = getCachedFields(dtoClass);

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
            if (field == null) return;

            field.setAccessible(true);
            Object nested = field.get(current);

            if (nested == null) {
                nested = field.getType().getDeclaredConstructor().newInstance();
                field.set(current, nested);
            }
            current = nested;
            currentFields = getCachedFields(field.getType());
        }

        Field targetField = currentFields.get(parts[parts.length - 1]);
        if (targetField != null) {
            targetField.setAccessible(true);
            targetField.set(current, convertValue(value, targetField.getType()));
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        // Простые преобразования типов
        if (targetType == String.class) return value.toString();
        if (targetType == Long.class && value instanceof Integer) return ((Integer) value).longValue();
        if (targetType == Integer.class && value instanceof Long) return ((Long) value).intValue();

        return value;
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