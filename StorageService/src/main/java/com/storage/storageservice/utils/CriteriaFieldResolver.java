package com.storage.storageservice.utils;

import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CriteriaFieldResolver {
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
}
