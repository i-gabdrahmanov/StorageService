---
name: project-packer
description: Упаковывает Java/Spring проект в один текстовый файл, распаковывает обратно и анонимизирует (удаляет пароли, адреса БД/Redis/Kafka/ZooKeeper, пакеты организации). Используй, когда нужно склеить проект для LLM-контекста, анонимизировать код для шаринга или передать проект третьим лицам без утечки данных.
---

# Project Packer

Три Python-скрипта для упаковки, распаковки и анонимизации Java/Spring проекта. Stdlib only, без внешних зависимостей.

## Зачем

- Подготовить весь проект как один текстовый файл для LLM-контекста
- Анонимизировать код перед передачей третьим лицам
- Round-trip: merge → sanitize → split = рабочий анонимизированный проект

## Полный пайплайн

```bash
# 1. Упаковать проект в один файл
python3 .claude/skills/project-packer/scripts/merge.py . -o /tmp/merged.txt

# 2. Анонимизировать
python3 .claude/skills/project-packer/scripts/sanitize.py /tmp/merged.txt -o /tmp/clean.txt

# 3. Распаковать анонимизированный проект
python3 .claude/skills/project-packer/scripts/split.py /tmp/clean.txt -o /tmp/clean-project --force
```

## Скрипты

### merge.py — Упаковка проекта

```bash
python3 .claude/skills/project-packer/scripts/merge.py <project-root> -o <output.txt> [--ext .java,.yml] [--include-tests]
```

| Аргумент | Описание |
|---|---|
| `project-root` | Путь к корню проекта |
| `-o, --output` | Путь к выходному файлу |
| `--ext` | Расширения через запятую (по умолчанию: .java, .kt, .yml, .yaml, .properties, .gradle, .groovy, .xml) |
| `--include-tests` | Включить тестовые директории (по умолчанию исключены) |

Исключаемые директории: `.git`, `target`, `build`, `.gradle`, `.venv`, `.idea`, `node_modules`, `.claude`, `.gigaide`

Также включаются файлы по имени: `Dockerfile`, `compose.yaml`, `docker-compose.*`

### split.py — Распаковка

```bash
python3 .claude/skills/project-packer/scripts/split.py <merged.txt> -o <output-dir> [--force] [--dry-run]
```

| Аргумент | Описание |
|---|---|
| `merged.txt` | Путь к склеенному файлу |
| `-o, --output` | Директория для распаковки |
| `--force` | Записывать в непустую директорию |
| `--dry-run` | Показать файлы без записи |

Встроена защита от path traversal (запрет `..` и абсолютных путей).

### sanitize.py — Анонимизация

```bash
python3 .claude/skills/project-packer/scripts/sanitize.py <merged.txt> -o <sanitized.txt> [--dry-run]
```

| Аргумент | Описание |
|---|---|
| `merged.txt` | Путь к склеенному файлу |
| `-o, --output` | Путь к очищенному файлу |
| `--dry-run` | Показать отчёт без записи |

## Что санитизируется

Санитизация работает агрессивно — строки с чувствительными данными **полностью удаляются** из конфигов, а в Java-коде адреса заменяются на плейсхолдеры.

### Удаление строк (конфиги, compose, properties)

| Категория | Что удаляется |
|---|---|
| Credentials | Строки с `password`, `username`, `*_PASSWORD`, `*_USER`, `*_SECRET`, `*_TOKEN`, `*_API_KEY` |
| Database | JDBC URL, `POSTGRES_DB`, `POSTGRES_*`, `DATASOURCE_URL`, `pg_isready` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `SPRING_DATA_REDIS_*`, `redis-cli`, `redis-server` |
| Memcached | `MEMCACHED_HOST`, `MEMCACHED_PORT` |
| ZooKeeper | `ZK_HOSTS`, `ZOO_SERVERS`, `ZOO_MY_ID`, `*ZOOKEEPER_CONNECT_STRING` |
| Kafka | `bootstrap-servers`, `KAFKA_BOOTSTRAP_SERVERS` |
| Addresses | Любые строки с `localhost`, `127.0.0.1`, `http://<host>:<port>` |
| Infrastructure | `STORAGE_SERVICE_URL`, `JAVA_TOOL_OPTIONS`, `SPRING_PROFILES_ACTIVE`, порт-маппинги, healthcheck-команды, `hostname:`, `container_name:` |

### Замены в Java-коде (структура кода сохраняется)

| Оригинал | Замена |
|---|---|
| `"memcached:11211"` | `"${MEMCACHED_ADDRESS}"` |
| `"zookeeper:2181"` | `"${ZOOKEEPER_ADDRESS}"` |
| `"http://localhost:8080"` | `"${SERVICE_URL}"` |
| `"/zookeeper/storageservice/app"` | `"${ZOOKEEPER_PATH}"` |
| `@Value("${...host:localhost}")` | `@Value("${...host:${CONFIGURE_HOST}}")` |

### Переименование пакетов

| Оригинал | Замена |
|---|---|
| `com.storage.storageservice` | `com.example.app` |
| `com.storage.springproxy` | `com.example.springproxy` |
| `com.storage.proxy` | `com.example.proxy` |
| `com.storage` | `com.example` |

Порядок замены: от длинных к коротким, чтобы `com.storage.storageservice` заменилось на `com.example.app`, а не на `com.example.storageservice`. Пути в файловых маркерах тоже переименовываются (`com/storage/` → `com/example/`).

## Структура скилла

```
.claude/skills/project-packer/
├── SKILL.md
├── scripts/
│   ├── merge.py
│   ├── split.py
│   └── sanitize.py
└── test/
```
