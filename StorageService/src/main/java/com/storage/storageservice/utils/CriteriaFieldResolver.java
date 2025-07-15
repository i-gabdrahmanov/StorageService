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

        // Обрабатываем вложенные свойства
        for (int i = 0; i < parts.length - 1; i++) {
            currentFrom = getOrCreateJoin(currentFrom, parts[i]);
        }

        return currentFrom.get(parts[parts.length - 1]).alias(fieldPath);
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {
        // Проверяем существующие JOIN'ы
        for (Join<?, ?> join : from.getJoins()) {
            if (join.getAttribute().getName().equals(attribute)) {
                return join;
            }
        }
        return from.join(attribute, JoinType.LEFT);
    }
}
