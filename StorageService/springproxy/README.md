# SpringProxy Service

SpringProxy Service - это прокси-сервис на базе Spring Boot с использованием Spring WebFlux, который предоставляет реактивное API для взаимодействия с основным StorageService.

## Особенности

- **Spring Boot 3.4.5** с Java 21
- **Spring WebFlux** для реактивного программирования
- **WebClient** для HTTP-запросов с настроенными таймаутами
- **Mono** для асинхронной обработки запросов и ответов
- **ApiPaths enum** для централизованного управления путями API
- **Connection Pooling** для эффективного управления соединениями
- Проксирование всех эндпоинтов основного StorageService

## Структура проекта

```
springproxy/
├── build.gradle                     # Конфигурация сборки
├── README.md                        # Документация
├── README-docker.md                 # Docker документация  
├── Dockerfile                       # Docker образ
├── docker-compose.override.yml      # Override для разработки
├── .dockerignore                    # Исключения для Docker
├── src/main/java/com/storage/springproxy/
│   ├── SpringProxyApplication.java      # Главный класс приложения
│   ├── config/
│   │   └── WebClientConfig.java         # Конфигурация WebClient
│   ├── client/
│   │   ├── ApiPaths.java                # Enum с путями API
│   │   └── StorageServiceClient.java    # Клиент для взаимодействия с StorageService
│   ├── example/
│   │   ├── ApiPathsExample.java         # Пример использования ApiPaths
│   │   └── WebClientTimeoutExample.java # Пример настройки таймаутов
│   └── controller/                      # REST контроллеры
│       ├── ArtifactProxyController.java
│       ├── ContractProxyController.java
│       ├── DocumentTypeProxyController.java
│       ├── DynamicDocumentProxyController.java
│       ├── InsuranceProxyController.java
│       ├── PrimaryCacheProxyController.java
│       ├── PropertyTypeProxyController.java
│       ├── RootProxyController.java
│       └── ZkProxyController.java
└── src/main/resources/
    ├── application.yml                  # Основная конфигурация
    ├── application-dev.yml              # Конфигурация для разработки
    ├── application-prod.yml             # Конфигурация для production
    └── application-docker.yml           # Конфигурация для Docker
```

## Конфигурация

### Основные настройки в `application.yml`:

```yaml
server:
  port: 8081                          # Порт SpringProxy сервиса

storage-service:
  url: http://localhost:8080          # URL основного StorageService

# WebClient timeout настройки (все в миллисекундах)
webclient:
  timeout:
    connection: 5000     # 5 секунд - время установления TCP соединения
    response: 30000      # 30 секунд - время ожидания ответа от сервера
    read: 30000          # 30 секунд - время чтения данных
    write: 30000         # 30 секунд - время записи данных
  pool:
    max-connections: 100        # Максимум соединений в пуле
    max-idle-time: 20000       # 20 секунд - время простоя соединения
```

### Профили конфигурации:

**Development (`application-dev.yml`):**
- Мягкие таймауты (60 секунд) для удобства отладки
- Подробное логирование всех HTTP операций
- Меньше соединений в пуле (50)

**Production (`application-prod.yml`):**
- Жесткие таймауты (15 секунд) для быстрого fail-fast
- Минимальное логирование для производительности  
- Больше соединений в пуле (200)

### Запуск с профилями:
```bash
# Development
./gradlew :springproxy:bootRun --args='--spring.profiles.active=dev'

# Production  
./gradlew :springproxy:bootRun --args='--spring.profiles.active=prod'
```

## API Endpoints

Все эндпоинты проксируют соответствующие эндпоинты основного StorageService:

### Artifact API
- `POST /api/v2/artifact/new` - Создание нового артефакта
- `POST /api/v2/artifact/{count}/generate` - Генерация артефактов
- `GET /api/v2/artifact?key={key}&value={value}` - Поиск по JSON полю
- `POST /api/v2/artifact/json` - Поиск по нативным JSON полям
- `POST /api/v2/artifact/customFields` - Поиск по кастомным полям

### Contract API
- `POST /api/v2/contract/new` - Создание нового контракта
- `GET /api/v2/contract?name={name}` - Получение контракта по имени
- `POST /api/v2/contract/{count}/generate` - Генерация контрактов

### DocumentType API
- `POST /api/v2/documentType/new` - Создание нового типа документа
- `GET /api/v2/documentType?name={name}` - Получение типа документа

### И другие API...

## Запуск

### Локальная разработка

1. Убедитесь, что основной StorageService запущен на порту 8080
2. Запустите SpringProxy сервис:

```bash
./gradlew :springproxy:bootRun
```

Сервис будет доступен на порту 8083.

### Docker запуск

```bash
# Сборка и запуск всех сервисов (включая SpringProxy)
docker-compose up --build

# Запуск только SpringProxy
docker-compose up springproxy
```

SpringProxy будет доступен на порту 8083: http://localhost:8083

### Health Check

```bash
# Проверка состояния приложения
curl http://localhost:8083/actuator/health

# Все Actuator эндпоинты
curl http://localhost:8083/actuator
```

Подробная документация по Docker: [README-docker.md](README-docker.md)

## Отличия от proxy-service

| Характеристика | proxy-service (Micronaut) | springproxy (Spring Boot) |
|---|---|---|
| Фреймворк | Micronaut 4.3.8 | Spring Boot 3.4.5 |
| HTTP Client | Micronaut HTTP Client | Spring WebClient |
| Асинхронность | Void/HttpResponse | Mono<Void>/Mono<Object> |
| Конфигурация | @Client аннотация | @Bean WebClient |
| Управление путями | Строковые литералы | ApiPaths enum |
| Таймауты | Базовые | Настроенные (Connection/Response/Read/Write) |
| Connection Pool | Автоматический | Кастомизированный с мониторингом |
| Профили | Нет | Dev/Prod/Docker конфигурации |
| Порт | 8081 | 8083 |
| Docker | Нет | Dockerfile + docker-compose |
| Health Check | Нет | Spring Boot Actuator |

## ApiPaths Enum

Все пути API централизованы в enum `ApiPaths`:

```java
// Пример использования
public Mono<Void> addNewArtifact(Object request) {
    return webClient.post()
            .uri(ARTIFACT_NEW.getPath())  // вместо "/api/v2/artifact/new"
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class);
}
```

### Преимущества ApiPaths enum:
- ✅ Централизованное управление путями API
- ✅ Защита от опечаток в URL
- ✅ Удобный рефакторинг при изменении путей
- ✅ Автодополнение в IDE
- ✅ Лучшая читаемость кода

## WebClient с таймаутами

### Настроенные таймауты:

**🕐 Connection Timeout (5s)** - время установления TCP соединения
**⏱️ Response Timeout (30s)** - время ожидания начала ответа от сервера  
**📖 Read Timeout (30s)** - время чтения данных из сокета
**📝 Write Timeout (30s)** - время записи данных в сокет

### Connection Pool:
- **Max Connections:** 100 одновременных соединений
- **Max Idle Time:** 20 секунд для неактивных соединений
- **Max Life Time:** 10 минут максимальное время жизни
- **Pending Acquire:** 10 секунд ожидания соединения из пула

### Преимущества:
- ✅ Предотвращение зависания запросов
- ✅ Быстрое обнаружение проблем с сетью  
- ✅ Эффективное использование ресурсов
- ✅ Улучшенная отзывчивость приложения
- ✅ Контролируемое поведение под нагрузкой

