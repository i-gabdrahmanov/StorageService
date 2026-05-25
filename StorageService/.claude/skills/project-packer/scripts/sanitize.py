#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# Phase A: package/path renames, longest-first to avoid partial matches
PACKAGE_RENAMES = [
    ('com.storage.storageservice', 'com.example.app'),
    ('com/storage/storageservice', 'com/example/app'),
    ('com.storage.springproxy', 'com.example.springproxy'),
    ('com/storage/springproxy', 'com/example/springproxy'),
    ('com.storage.proxy', 'com.example.proxy'),
    ('com/storage/proxy', 'com/example/proxy'),
    ('com.storage', 'com.example'),
    ('com/storage', 'com/example'),
    ('StorageService', 'ExampleService'),
    ('storageService', 'exampleService'),
    ('storage-service', 'example-service'),
    ('storage_service', 'example_service'),
    ('storageservice', 'exampleservice'),
]

# Phase B: lines to DELETE entirely (regex matched against each line)
# These target config/compose/properties/Dockerfile — not Java source
DELETE_LINE_PATTERNS = [
    # --- Credentials ---
    re.compile(r'password\s*[:=]', re.IGNORECASE),
    re.compile(r'username\s*[:=]', re.IGNORECASE),
    re.compile(r'\w*_?PASSWORD\s*[:=]', re.IGNORECASE),
    re.compile(r'\w*_?USER\s*[:=]', re.IGNORECASE),
    re.compile(r'\w*_?SECRET\s*[:=]', re.IGNORECASE),
    re.compile(r'\w*_?TOKEN\s*[:=]', re.IGNORECASE),
    re.compile(r'\w*_?API_KEY\s*[:=]', re.IGNORECASE),
    re.compile(r'\w*_?CREDENTIALS?\s*[:=]', re.IGNORECASE),

    # --- JDBC / Datasource ---
    re.compile(r'jdbc:\w+://'),
    re.compile(r'\w*DATASOURCE_URL\s*[:=]', re.IGNORECASE),
    re.compile(r'^\s*url:\s*jdbc:', re.IGNORECASE),

    # --- Postgres env ---
    re.compile(r'POSTGRES_DB\s*[:=]'),
    re.compile(r'POSTGRES_SHARED_BUFFERS\s*[:=]'),
    re.compile(r'POSTGRES_EFFECTIVE_CACHE_SIZE\s*[:=]'),
    re.compile(r'pg_isready'),

    # --- Redis ---
    re.compile(r'SPRING_DATA_REDIS_HOST\s*[:=]'),
    re.compile(r'SPRING_DATA_REDIS_PORT\s*[:=]'),
    re.compile(r'REDIS_HOST\s*[:=]', re.IGNORECASE),
    re.compile(r'REDIS_PORT\s*[:=]', re.IGNORECASE),
    re.compile(r'redis-cli'),
    re.compile(r'redis-server'),

    # --- Memcached ---
    re.compile(r'MEMCACHED_HOST\s*[:=]'),
    re.compile(r'MEMCACHED_PORT\s*[:=]'),

    # --- ZooKeeper ---
    re.compile(r'ZK_HOSTS\s*[:=]'),
    re.compile(r'ZOO_SERVERS\s*[:=]'),
    re.compile(r'ZOO_MY_ID\s*[:=]'),
    re.compile(r'ZOOKEEPER_CONNECT_STRING\s*[:=]', re.IGNORECASE),
    re.compile(r'ZOO_4LW_COMMANDS_WHITELIST\s*[:=]'),
    re.compile(r'echo\s+ruok\s*\|'),

    # --- Kafka ---
    re.compile(r'bootstrap[._-]servers\s*[:=]', re.IGNORECASE),
    re.compile(r'KAFKA_BOOTSTRAP_SERVERS\s*[:=]'),

    # --- Service URLs / internal addresses ---
    re.compile(r'STORAGE_SERVICE_URL\s*[:=]'),
    re.compile(r'MICRONAUT_SERVER_PORT\s*[:=]'),
    re.compile(r'SERVER_PORT\s*[:=]'),
    re.compile(r'HTTP_PORT\s*[:=]'),
    re.compile(r'JAVA_TOOL_OPTIONS\s*[:=]'),
    re.compile(r'SPRING_PROFILES_ACTIVE\s*[:=]'),

    # --- Compose infrastructure ---
    re.compile(r'^\s*hostname:\s*\S'),
    re.compile(r'^\s*container_name:\s*\S'),
    re.compile(r'^\s*shm_size:'),

    # --- Port mappings in compose (e.g. "5432:5432") ---
    re.compile(r'^\s*-\s*"\d+:\d+"'),

    # --- Any line with localhost / 127.0.0.1 (not in Java code) ---
    re.compile(r'localhost'),
    re.compile(r'127\.0\.0\.1'),

    # --- Internal http:// URLs pointing to services ---
    re.compile(r'http://\w+:\d+'),

    # --- Healthchecks with infrastructure refs ---
    re.compile(r'curl\s'),
    re.compile(r'actuator/health'),
    re.compile(r'nc\s'),

    # --- IP addresses ---
    re.compile(r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}'),
]

# Patterns that should NOT trigger deletion even if a DELETE pattern matches.
# This protects Java source code, imports, class names, annotations, etc.
KEEP_LINE_PATTERNS = [
    re.compile(r'^\s*(package|import)\s'),
    re.compile(r'^\s*(public|private|protected|static|final|abstract|class|interface|enum|void|return|throw|new |if |else|for |while|try|catch)\b'),
    re.compile(r'^\s*@\w'),          # annotations (@Value, @Bean, etc.)
    re.compile(r'^\s*\*'),           # javadoc
    re.compile(r'^\s*//===FILE:'),   # file markers
    re.compile(r'^\s*//===END_FILE'),
    re.compile(r'^\s*//===MERGED'),
    re.compile(r'^\s*\w+\s*\('),     # method calls (e.g. builder.setXxx(...))
    re.compile(r'^\s*\.\w+\('),      # chained calls (.setHost(...))
    re.compile(r'^\s*}'),            # closing braces
]

# Phase C: replacements inside Java string literals (keep line, replace value)
JAVA_LITERAL_REPLACEMENTS = [
    (re.compile(r'"memcached:\d+"'), '"${MEMCACHED_ADDRESS}"'),
    (re.compile(r'"zookeeper:\d+"'), '"${ZOOKEEPER_ADDRESS}"'),
    (re.compile(r'"localhost:\d+"'), '"${SERVICE_ADDRESS}"'),
    (re.compile(r'"http://localhost:\d+"'), '"${SERVICE_URL}"'),
    (re.compile(r'"http://localhost:\d+/[^"]*"'), '"${SERVICE_URL}"'),
    (re.compile(r'"/zookeeper/[^"]*"'), '"${ZOOKEEPER_PATH}"'),
    # @Value defaults with localhost
    (re.compile(r'(\$\{[^}]*:)http://localhost:\d+[^}]*(})'), r'\1${SERVICE_URL}\2'),
    (re.compile(r'(\$\{[^}]*:)localhost(})'), r'\1${CONFIGURE_HOST}\2'),
    # Spring placeholders with default values containing hosts/ports
    (re.compile(r'\$\{REDIS_HOST:[^}]+}'), '${REDIS_HOST}'),
    (re.compile(r'\$\{REDIS_PORT:[^}]+}'), '${REDIS_PORT}'),
]

# Phase D: app name sanitization (safe regex replacements)
APP_NAME_RULES = [
    (re.compile(r"(rootProject\.name\s*=\s*')[^']+(')", re.IGNORECASE), r"\1ExampleService\2"),
    (re.compile(r'(name:\s*)StorageService'), r'\1example-service'),
]


def sanitize_packages(text: str) -> tuple[str, dict[str, int]]:
    counts: dict[str, int] = {}
    for old, new in PACKAGE_RENAMES:
        n = text.count(old)
        if n > 0:
            text = text.replace(old, new)
            counts[f'{old} -> {new}'] = n
    return text, counts


def is_java_or_code_line(line: str) -> bool:
    return any(p.search(line) for p in KEEP_LINE_PATTERNS)


def should_delete_line(line: str) -> bool:
    if is_java_or_code_line(line):
        return False
    return any(p.search(line) for p in DELETE_LINE_PATTERNS)


def sanitize_lines(text: str) -> tuple[str, dict[str, int]]:
    lines = text.split('\n')
    result: list[str] = []
    deleted = 0

    for line in lines:
        if should_delete_line(line):
            deleted += 1
        else:
            result.append(line)

    return '\n'.join(result), {'deleted_lines': deleted}


def sanitize_java_literals(text: str) -> tuple[str, dict[str, int]]:
    total = 0
    for pattern, replacement in JAVA_LITERAL_REPLACEMENTS:
        text, n = pattern.subn(replacement, text)
        total += n
    return text, {'java_literals': total}


def sanitize_app_names(text: str) -> tuple[str, dict[str, int]]:
    total = 0
    for pattern, replacement in APP_NAME_RULES:
        text, n = pattern.subn(replacement, text)
        total += n
    return text, {'app_names': total}


def sanitize(text: str) -> tuple[str, dict[str, int]]:
    all_counts: dict[str, int] = {}

    text, pkg_counts = sanitize_packages(text)
    pkg_total = sum(pkg_counts.values())
    if pkg_total:
        all_counts['packages'] = pkg_total

    text, del_counts = sanitize_lines(text)
    all_counts.update(del_counts)

    text, lit_counts = sanitize_java_literals(text)
    all_counts.update(lit_counts)

    text, name_counts = sanitize_app_names(text)
    all_counts.update(name_counts)

    return text, all_counts


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Sanitize a merged project file: delete passwords, addresses, package names'
    )
    parser.add_argument('input_file', type=Path, help='Path to merged file')
    parser.add_argument('-o', '--output', type=Path, required=True, help='Output sanitized file')
    parser.add_argument('--dry-run', action='store_true', help='Show counts without writing')
    args = parser.parse_args()

    if not args.input_file.is_file():
        print(f'Error: {args.input_file} not found', file=sys.stderr)
        return 1

    text = args.input_file.read_text(encoding='utf-8', errors='replace')
    sanitized, counts = sanitize(text)

    total = sum(counts.values())
    print(f'Sanitization report ({total} total actions):')
    for category, count in counts.items():
        print(f'  {category:>16}: {count}')

    if args.dry_run:
        print('\nDry run — no file written.')
        return 0

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(sanitized, encoding='utf-8')
    print(f'\nSanitized output -> {args.output}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
