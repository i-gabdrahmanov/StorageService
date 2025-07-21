package com.storage.storageservice.utils;

import jakarta.persistence.criteria.*;

public class CriteriaFieldResolver {
    public static Selection<?> resolveSelection(
            From<?, ?> from,
            String fieldPath,
            CriteriaBuilder cb
    ) {
        String[] parts = fieldPath.split("\\.");
        From<?, ?> currentFrom = from;

        // Для всех частей пути кроме последней
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            // Всегда делаем join для промежуточных частей пути
            currentFrom = getOrCreateJoin(currentFrom, part);
        }

        // Для последней части просто возвращаем get с алиасом
        return currentFrom.get(parts[parts.length - 1]).alias(fieldPath);
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {
        // Проверяем существующие JOIN'ы
        for (Join<?, ?> join : from.getJoins()) {
            if (join.getAttribute().getName().equals(attribute)) {
                return join;
            }
        }
        // Создаем новый LEFT JOIN
        return from.join(attribute, JoinType.LEFT);
    }
}
