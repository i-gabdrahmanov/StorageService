package com.storage.storageservice.utils;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility for building dynamic JPA EntityGraph using dot-separated attribute paths.
 *
 * Examples of paths:
 * - "employee"
 * - "parent.employee"
 * - "parent.employee.staffpost.department"
 */
public final class EntityGraphUtils {

    private EntityGraphUtils() {
    }

    /**
     * Builds a fetch graph (structure only) for the given root entity and attribute paths.
     * The returned graph can be applied via Hibernate 6 API:
     *  query.unwrap(org.hibernate.query.Query.class)
     *       .applyGraph(graph, org.hibernate.graph.GraphSemantic.FETCH);
     */
    public static <T> EntityGraph<T> buildFetchGraph(EntityManager entityManager,
                                                     Class<T> rootClass,
                                                     Collection<String> attributePaths) {
        EntityGraph<T> graph = entityManager.createEntityGraph(rootClass);
        if (attributePaths == null || attributePaths.isEmpty()) {
            return graph;
        }

        // Track created subgraphs to avoid duplicates for shared prefixes
        Map<String, Subgraph<?>> subgraphByPath = new HashMap<>();
        // Avoid duplicating leaf attribute nodes on the same parent
        Map<String, Set<String>> leafNodesByPath = new HashMap<>();

        for (String rawPath : attributePaths) {
            if (rawPath == null) {
                continue;
            }
            String path = rawPath.trim();
            if (path.isEmpty()) {
                continue;
            }

            String[] parts = path.split("\\.");

            if (parts.length == 1) {
                // Single attribute on root
                addLeafIfAbsent(graph, leafNodesByPath, "", parts[0]);
                continue;
            }

            // Build or reuse subgraphs for all parents except the last attribute
            String prefixKey = null;
            Subgraph<?> parent = null;
            for (int i = 0; i < parts.length - 1; i++) {
                prefixKey = (i == 0) ? parts[0] : prefixKey + "." + parts[i];
                Subgraph<?> current = subgraphByPath.get(prefixKey);
                if (current == null) {
                    current = (parent == null)
                            ? graph.addSubgraph(parts[i])
                            : parent.addSubgraph(parts[i]);
                    subgraphByPath.put(prefixKey, current);
                }
                parent = current;
            }

            String leaf = parts[parts.length - 1];
            if (parent == null) {
                // Should not happen for parts.length > 1, but safe-guard
                addLeafIfAbsent(graph, leafNodesByPath, "", leaf);
            } else {
                String parentKey = Objects.requireNonNull(prefixKey);
                Set<String> leaves = leafNodesByPath.computeIfAbsent(parentKey, k -> new HashSet<>());
                if (leaves.add(leaf)) {
                    parent.addAttributeNodes(leaf);
                }
            }
        }

        return graph;
    }

    /**
     * Varargs convenience overload.
     */
    @SafeVarargs
    public static <T> EntityGraph<T> buildFetchGraph(EntityManager entityManager,
                                                     Class<T> rootClass,
                                                     String... attributePaths) {
        return buildFetchGraph(entityManager, rootClass, java.util.Arrays.asList(attributePaths));
    }

    private static void addLeafIfAbsent(EntityGraph<?> graph,
                                        Map<String, Set<String>> leafNodesByPath,
                                        String parentKey,
                                        String leafAttribute) {
        Set<String> leaves = leafNodesByPath.computeIfAbsent(parentKey, k -> new HashSet<>());
        if (leaves.add(leafAttribute)) {
            graph.addAttributeNodes(leafAttribute);
        }
    }
}

