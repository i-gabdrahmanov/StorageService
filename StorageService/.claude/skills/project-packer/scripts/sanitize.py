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
]

# Phase B: regex patterns grouped by category
# Each entry: (category_name, [(pattern, replacement, flags)])
SANITIZE_RULES: list[tuple[str, list[tuple[str, str, int]]]] = [
    ('passwords', [
        (r'(password\s*:\s*)["\']?[^"\'\s\n#]+["\']?', r'\1"<REDACTED>"', re.IGNORECASE),
        (r'(password\s*=\s*)[^\s\n#]+', r'\1<REDACTED>', re.IGNORECASE),
        (r'(\w*PASSWORD\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1<REDACTED>', re.IGNORECASE),
        (r'(username\s*:\s*)["\']?[^"\'\s\n#]+["\']?', r'\1"<REDACTED>"', re.IGNORECASE),
        (r'(\w*_?USER\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1<REDACTED>', re.IGNORECASE),
    ]),
    ('database', [
        (r'jdbc:postgresql://[^\s"\'#]+', 'jdbc:postgresql://dbhost:5432/appdb', 0),
        (r'(POSTGRES_DB\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1appdb', 0),
        (r'(pg_isready\s+-U\s+)\S+(\s+-d\s+)\S+', r'\1user\2appdb', 0),
    ]),
    ('redis', [
        (r'(REDIS_HOST\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1redis-host', re.IGNORECASE),
        (r'(SPRING_DATA_REDIS_HOST\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1redis-host', 0),
        (r'(SPRING_DATA_REDIS_PORT\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\16379', 0),
        (r'(\$\{REDIS_HOST:)[^}]+(})', r'\1redis-host\2', 0),
        (r'(\$\{REDIS_PORT:)[^}]+(})', r'\g<1>6379\2', 0),
    ]),
    ('memcached', [
        (r'(getAddresses\s*\(\s*")[^"]+(")', r'\1memcached-host:11211\2', 0),
        (r'(MEMCACHED_HOST\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1memcached-host', 0),
        (r'(MEMCACHED_PORT\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\g<1>11211', 0),
    ]),
    ('zookeeper', [
        (r'"zookeeper:\d+"', '"zk-host:2181"', 0),
        (r'(ZOOKEEPER_CONNECT_STRING\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1zk-host:2181', 0),
        (r'(ZK_HOSTS\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1zk-host:2181', 0),
        (r'(ZOO_SERVERS\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1server.1=zk-host:2888:3888;2181', 0),
        (r'"/zookeeper/[^"]*"', '"/zookeeper/app/config"', 0),
        (r'(hostname:\s*)zookeeper', r'\1zk-host', 0),
        (r'(container_name:\s*)zookeeper', r'\1zk-host', 0),
    ]),
    ('kafka', [
        (r'(bootstrap[._-]servers\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1kafka-host:9092', re.IGNORECASE),
        (r'(KAFKA_BOOTSTRAP_SERVERS\s*[:=]\s*)["\']?[^"\'\s\n#]+["\']?', r'\1kafka-host:9092', 0),
    ]),
    ('app_names', [
        (r"(rootProject\.name\s*=\s*')[^']+(')", r"\1ExampleService\2", 0),
        (r'(name:\s*)StorageService', r'\1example-service', 0),
        (r'(container_name:\s*)redis', r'\1cache', 0),
    ]),
]


def sanitize_packages(text: str) -> tuple[str, dict[str, int]]:
    counts: dict[str, int] = {}
    for old, new in PACKAGE_RENAMES:
        n = text.count(old)
        if n > 0:
            text = text.replace(old, new)
            counts[f'{old} -> {new}'] = n
    return text, counts


def sanitize_patterns(text: str) -> tuple[str, dict[str, int]]:
    counts: dict[str, int] = {}
    for category, rules in SANITIZE_RULES:
        total = 0
        for pattern, replacement, flags in rules:
            text, n = re.subn(pattern, replacement, text, flags=flags)
            total += n
        counts[category] = total
    return text, counts


def sanitize(text: str) -> tuple[str, dict[str, int]]:
    text, pkg_counts = sanitize_packages(text)
    text, pat_counts = sanitize_patterns(text)

    all_counts: dict[str, int] = {}
    pkg_total = sum(pkg_counts.values())
    if pkg_total:
        all_counts['packages'] = pkg_total
    all_counts.update(pat_counts)
    return text, all_counts


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Sanitize a merged project file: remove passwords, addresses, package names'
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
    print(f'Sanitization report ({total} total replacements):')
    for category, count in counts.items():
        print(f'  {category:>16}: {count} replacements')

    if args.dry_run:
        print('\nDry run — no file written.')
        return 0

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(sanitized, encoding='utf-8')
    print(f'\nSanitized output -> {args.output}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
