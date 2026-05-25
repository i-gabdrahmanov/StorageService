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

| Категория | Что ищем | Замена |
|---|---|---|
| packages | `com.storage.*` пакеты и пути | `com.example.*` |
| passwords | `password:`, `*_PASSWORD=`, `username:` | `<REDACTED>` |
| database | JDBC URL, `POSTGRES_DB` | `jdbc:postgresql://dbhost:5432/appdb` |
| redis | `REDIS_HOST`, `SPRING_DATA_REDIS_HOST` | `redis-host` |
| memcached | `getAddresses("...")`, `MEMCACHED_HOST` | `memcached-host:11211` |
| zookeeper | `"zookeeper:2181"`, `ZK_HOSTS`, ZK-пути | `zk-host:2181` |
| kafka | `bootstrap-servers` | `kafka-host:9092` |
| app_names | `rootProject.name`, `spring.application.name` | `ExampleService` |

Порядок замены пакетов: от длинных к коротким, чтобы `com.storage.storageservice` заменилось на `com.example.app`, а не на `com.example.storageservice`.

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
