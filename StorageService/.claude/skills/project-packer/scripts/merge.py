#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

EXCLUDED_DIRS = {
    '.git', 'target', 'build', '.gradle', '.venv', '.idea',
    'node_modules', '__pycache__', '.claude', '.gigaide', '.mvn',
}

DEFAULT_EXTENSIONS = {
    '.java', '.kt',
    '.yml', '.yaml', '.properties',
    '.gradle', '.groovy',
    '.xml',
}

DEFAULT_FILENAMES = {
    'Dockerfile', 'compose.yaml', 'compose.yml',
    'docker-compose.yaml', 'docker-compose.yml',
    'gradlew', 'gradlew.bat',
}


def collect_files(
    root: Path,
    extensions: set[str],
    filenames: set[str],
    include_tests: bool,
) -> list[Path]:
    result: list[Path] = []
    root_str = str(root)

    for dirpath, dirnames, files in os.walk(root_str):
        dirnames[:] = [
            d for d in dirnames
            if d not in EXCLUDED_DIRS
        ]
        dirnames.sort()

        rel_dir = os.path.relpath(dirpath, root_str)
        if not include_tests:
            parts = Path(rel_dir).parts
            if 'test' in parts or 'tests' in parts:
                continue

        for fname in sorted(files):
            fpath = Path(dirpath) / fname
            ext = fpath.suffix.lower()
            if ext in extensions or fname in filenames:
                result.append(fpath)

    return result


def merge(files: list[Path], root: Path, output: Path) -> tuple[int, int]:
    total_lines = 0
    project_name = root.resolve().name

    with open(output, 'w', encoding='utf-8') as out:
        out.write(
            f'//===MERGED_PROJECT: {project_name}'
            f' | {datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")}'
            f' | {len(files)} files===//\n\n'
        )

        for i, fpath in enumerate(files):
            rel = fpath.relative_to(root).as_posix()
            try:
                content = fpath.read_text(encoding='utf-8', errors='replace')
            except OSError as e:
                print(f'  SKIP {rel}: {e}', file=sys.stderr)
                continue

            lines = content.count('\n') + (1 if content and not content.endswith('\n') else 0)
            total_lines += lines

            out.write(f'//===FILE: {rel}===//\n')
            out.write(content)
            if content and not content.endswith('\n'):
                out.write('\n')
            out.write('//===END_FILE===//\n')
            if i < len(files) - 1:
                out.write('\n')

    return len(files), total_lines


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Merge project files into a single text file'
    )
    parser.add_argument('project_root', type=Path, help='Path to project root')
    parser.add_argument('-o', '--output', type=Path, required=True, help='Output file path')
    parser.add_argument(
        '--ext', type=str, default=None,
        help='Comma-separated extensions to include (e.g. .java,.yml)'
    )
    parser.add_argument(
        '--include-tests', action='store_true',
        help='Include test directories (excluded by default)'
    )
    args = parser.parse_args()

    root = args.project_root.resolve()
    if not root.is_dir():
        print(f'Error: {root} is not a directory', file=sys.stderr)
        return 1

    if args.ext:
        extensions = {e.strip() if e.strip().startswith('.') else f'.{e.strip()}'
                      for e in args.ext.split(',')}
    else:
        extensions = DEFAULT_EXTENSIONS

    files = collect_files(root, extensions, DEFAULT_FILENAMES, args.include_tests)
    if not files:
        print('No files found to merge.', file=sys.stderr)
        return 1

    args.output.parent.mkdir(parents=True, exist_ok=True)
    file_count, total_lines = merge(files, root, args.output)

    ext_counts: Counter[str] = Counter()
    for f in files:
        ext_counts[f.suffix.lower() or f.name] += 1

    print(f'Merged {file_count} files ({total_lines} lines) -> {args.output}')
    for ext, count in ext_counts.most_common():
        print(f'  {ext:>12}: {count} files')

    return 0


if __name__ == '__main__':
    sys.exit(main())
