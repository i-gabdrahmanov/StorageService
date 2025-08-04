package com.storage.springproxy.example;

import com.storage.springproxy.client.ApiPaths;

/**
 * Пример использования enum ApiPaths для демонстрации централизованного управления путями API
 */
public class ApiPathsExample {
    
    public static void main(String[] args) {
        System.out.println("=== Примеры использования ApiPaths enum ===");
        System.out.println();
        
        // Artifact endpoints
        System.out.println("Artifact API:");
        System.out.println("  Создание артефакта: " + ApiPaths.ARTIFACT_NEW);
        System.out.println("  Генерация артефактов: " + ApiPaths.ARTIFACT_GENERATE);
        System.out.println("  Базовый путь артефактов: " + ApiPaths.ARTIFACT_BASE);
        System.out.println("  JSON поиск: " + ApiPaths.ARTIFACT_JSON);
        System.out.println("  Кастомные поля: " + ApiPaths.ARTIFACT_CUSTOM_FIELDS);
        System.out.println();
        
        // Contract endpoints
        System.out.println("Contract API:");
        System.out.println("  Создание контракта: " + ApiPaths.CONTRACT_NEW);
        System.out.println("  Базовый путь контрактов: " + ApiPaths.CONTRACT_BASE);
        System.out.println("  Генерация контрактов: " + ApiPaths.CONTRACT_GENERATE);
        System.out.println();
        
        // Cache endpoints
        System.out.println("Cache API:");
        System.out.println("  Базовый путь кэша: " + ApiPaths.PRIMARY_CACHE_BASE);
        System.out.println("  Добавление в кэш: " + ApiPaths.PRIMARY_CACHE_ADD);
        System.out.println();
        
        // Root
        System.out.println("System API:");
        System.out.println("  Root endpoint: " + ApiPaths.ROOT);
        System.out.println("  ZooKeeper config: " + ApiPaths.ZK_CONFIG);
        
        System.out.println();
        System.out.println("=== Преимущества использования enum ===");
        System.out.println("✅ Централизованное управление путями API");
        System.out.println("✅ Защита от опечаток в URL");
        System.out.println("✅ Удобный рефакторинг при изменении путей");
        System.out.println("✅ Автодополнение в IDE");
        System.out.println("✅ Лучшая читаемость кода");
    }
}