# SpringProxy Docker Guide

## 🐳 Запуск в Docker

### Быстрый старт

```bash
# Сборка и запуск всех сервисов (включая SpringProxy)
docker-compose up --build

# Запуск только SpringProxy (требует запущенный основной сервис)
docker-compose up springproxy
```

SpringProxy будет доступен на **порту 8083**: http://localhost:8083

### Доступные сервисы

| Сервис | Порт | Описание |
|--------|------|----------|
| app | 8080 | Основной StorageService |
| proxy-service | 8081 | Micronaut Proxy |
| **springproxy** | **8083** | **Spring Boot Proxy** |
| db | 5432 | PostgreSQL |
| zookeeper | 2181 | Apache ZooKeeper |
| memcached | 11211 | Memcached |

### Health Check

SpringProxy включает health check эндпоинт:

```bash
# Проверка состояния
curl http://localhost:8083/actuator/health

# Подробная информация
curl http://localhost:8083/actuator/health | jq
```

### Логи

```bash
# Просмотр логов SpringProxy
docker-compose logs -f springproxy

# Логи всех сервисов
docker-compose logs -f
```

### Переменные окружения

SpringProxy настраивается через переменные окружения:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
  STORAGE_SERVICE_URL: "http://app:8080"
  SERVER_PORT: 8083
  # Дополнительные настройки таймаутов
  WEBCLIENT_TIMEOUT_CONNECTION: 10000
  WEBCLIENT_TIMEOUT_RESPONSE: 45000
```

### Профили конфигурации

- **docker** - базовый профиль для Docker окружения
- **dev,docker** - разработка в Docker с детальными логами  
- **prod,docker** - production настройки в Docker

### Отладка

Для разработки можно использовать `docker-compose.override.yml`:

```bash
# Запуск с дополнительными опциями для разработки
docker-compose -f docker-compose.yml -f springproxy/docker-compose.override.yml up springproxy
```

Это включает:
- Remote debugging на порту 5006
- Детальное логирование
- JVM настройки для разработки

### Мониторинг

SpringProxy предоставляет Actuator эндпоинты:

```bash
# Метрики
curl http://localhost:8083/actuator/metrics

# Информация о приложении  
curl http://localhost:8083/actuator/info

# Все доступные эндпоинты
curl http://localhost:8083/actuator
```

### Troubleshooting

**Проблема**: SpringProxy не может подключиться к основному сервису
```bash
# Проверьте что основной сервис запущен
docker-compose ps app

# Проверьте логи
docker-compose logs app
docker-compose logs springproxy
```

**Проблема**: Медленные запросы
```bash
# Увеличьте таймауты через переменные окружения
docker-compose exec springproxy sh -c 'echo "WEBCLIENT_TIMEOUT_RESPONSE=60000" >> /app/application.properties'
```

**Проблема**: Health check не проходит
```bash
# Проверьте доступность Actuator
curl http://localhost:8083/actuator/health

# Проверьте логи health check
docker-compose logs springproxy | grep health
```

### Сравнение с другими proxy сервисами

| Характеристика | proxy-service | springproxy |
|---|---|---|
| Порт | 8081 | 8083 |
| Фреймворк | Micronaut | Spring Boot |
| Health Check | Нет | Actuator |
| Отладка | Нет | Remote debugging |
| Мониторинг | Базовый | Actuator metrics |