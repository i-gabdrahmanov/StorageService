package com.storage.springproxy.example;


/**
 * Пример работы с таймаутами WebClient
 */
public class WebClientTimeoutExample {
    
    public static void main(String[] args) {
        System.out.println("=== Настройки таймаутов WebClient ===");
        System.out.println();
        
        System.out.println("🕐 Типы таймаутов:");
        System.out.println("1. Connection Timeout (5s) - время установления TCP соединения");
        System.out.println("2. Response Timeout (30s)  - время ожидания начала ответа");
        System.out.println("3. Read Timeout (30s)      - время чтения данных из сокета");
        System.out.println("4. Write Timeout (30s)     - время записи данных в сокет");
        System.out.println();
        
        System.out.println("🏊 Connection Pool настройки:");
        System.out.println("• Max Connections: 100     - максимум одновременных соединений");
        System.out.println("• Max Idle Time: 20s      - время жизни неактивного соединения");
        System.out.println("• Max Life Time: 10min    - максимальное время жизни соединения");
        System.out.println("• Pending Acquire: 10s    - время ожидания соединения из пула");
        System.out.println();
        
        System.out.println("⚙️ Профили конфигурации:");
        System.out.println("• Default: сбалансированные настройки для разработки");
        System.out.println("• Dev:     мягкие таймауты (60s) для отладки");
        System.out.println("• Prod:    жесткие таймауты (15s) для production");
        System.out.println();
        
        System.out.println("🔥 Преимущества настроенных таймаутов:");
        System.out.println("✅ Предотвращение зависания запросов");
        System.out.println("✅ Быстрое обнаружение проблем с сетью");
        System.out.println("✅ Эффективное использование ресурсов");
        System.out.println("✅ Улучшенная отзывчивость приложения");
        System.out.println("✅ Контролькруемое поведение под нагрузкой");
        System.out.println();
        
        System.out.println("🚨 Обработка исключений:");
        System.out.println("• WebClientRequestException - проблемы с соединением");
        System.out.println("• TimeoutException - превышение таймаута");
        System.out.println("• ConnectException - невозможность установить соединение");
        System.out.println();
        
        System.out.println("📝 Пример обработки ошибок:");
        System.out.println("storageServiceClient.getArtifact()");
        System.out.println("    .timeout(Duration.ofSeconds(10))  // Дополнительный таймаут");
        System.out.println("    .onErrorResume(WebClientRequestException.class, ex -> {");
        System.out.println("        log.error(\"Connection error: {}\", ex.getMessage());");
        System.out.println("        return Mono.empty();");
        System.out.println("    })");
        System.out.println("    .onErrorResume(TimeoutException.class, ex -> {");
        System.out.println("        log.error(\"Timeout error: {}\", ex.getMessage());");
        System.out.println("        return Mono.empty();");
        System.out.println("    });");
        
        System.out.println();
        System.out.println("🌍 Запуск с профилями:");
        System.out.println("• Dev:  --spring.profiles.active=dev");
        System.out.println("• Prod: --spring.profiles.active=prod");
    }
}