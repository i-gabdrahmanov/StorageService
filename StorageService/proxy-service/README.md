# Proxy Service

Micronaut-приложение для проксирования запросов к Storage Service.

## Описание

Данный сервис работает на порту 8081 и проксирует все REST запросы к основному Storage Service (порт 8080).

## Поддерживаемые endpoints

- `/api/v2/artifact/*` - работа с артефактами
- `/api/v2/contract/*` - работа с контрактами  
- `/api/v2/documentType/*` - работа с типами документов
- `/api/v2/dd/*` - работа с динамическими документами
- `/api/v2/propertyType/*` - работа с типами свойств
- `/api/v2/insurance/*` - работа со страховками
- `/api/v2/primaryCache/*` - работа с кешем
- `/api/v2/zk/*` - работа с ZooKeeper
- `/` - корневой endpoint

## Запуск

1. Убедитесь что Storage Service запущен на порту 8080
2. Запустите Proxy Service:

```bash
./gradlew run
```

Или создайте jar и запустите:

```bash
./gradlew build
java -jar build/libs/proxy-service-0.1-all.jar
```

## Конфигурация

- Порт: 8081
- URL Storage Service: http://localhost:8080

Для изменения настроек отредактируйте `src/main/resources/application.yml`

## Примеры использования

```bash
# Вместо обращения к Storage Service напрямую:
curl http://localhost:8080/api/v2/artifact/new -d '{"name":"test"}' -H "Content-Type: application/json"

# Теперь можно обращаться через Proxy Service:
curl http://localhost:8081/api/v2/artifact/new -d '{"name":"test"}' -H "Content-Type: application/json"
``` 