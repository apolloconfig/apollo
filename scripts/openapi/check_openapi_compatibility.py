#!/usr/bin/env python3
#
# Copyright 2026 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the
# License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. See the License for the specific language governing permissions
# and limitations under the License.

"""Check Apollo OpenAPI specs for incompatible v1 contract changes.

The script intentionally avoids non-standard Python packages so it can run in
CI without a separate toolchain bootstrap. It performs a focused structural scan
for the compatibility rules Apollo cares about first:

* existing paths and HTTP methods must remain available;
* existing operationId values must remain stable;
* existing schemas must remain available;
* existing schemas must not gain new required fields by default.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from pathlib import Path
import re
import sys
from typing import Dict, Iterable, List, Optional, Set, Tuple
from urllib.request import urlopen


HTTP_METHODS = {
    "delete",
    "get",
    "head",
    "options",
    "patch",
    "post",
    "put",
    "trace",
}


@dataclass
class Operation:
  operation_id: Optional[str] = None


@dataclass
class OpenApiSnapshot:
  operations: Dict[Tuple[str, str], Operation] = field(default_factory=dict)
  schemas: Set[str] = field(default_factory=set)
  schema_required: Dict[str, Set[str]] = field(default_factory=dict)


def load_text(source: str) -> str:
  if source.startswith("http://") or source.startswith("https://"):
    with urlopen(source, timeout=30) as response:
      return response.read().decode("utf-8")
  return Path(source).read_text(encoding="utf-8")


def strip_quotes(value: str) -> str:
  value = value.strip()
  if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
    return value[1:-1]
  return value


def parse_inline_required(value: str) -> Set[str]:
  value = value.strip()
  if not value.startswith("[") or not value.endswith("]"):
    return set()
  return {strip_quotes(part) for part in value[1:-1].split(",") if part.strip()}


def parse_spec(text: str) -> OpenApiSnapshot:
  snapshot = OpenApiSnapshot()

  in_paths = False
  current_path: Optional[str] = None
  current_method: Optional[str] = None

  in_components = False
  in_schemas = False
  current_schema: Optional[str] = None
  collecting_required_schema: Optional[str] = None
  required_indent: Optional[int] = None

  for raw_line in text.splitlines():
    if not raw_line.strip():
      continue

    indent = len(raw_line) - len(raw_line.lstrip(" "))
    stripped = raw_line.strip()

    if collecting_required_schema is not None:
      if indent > (required_indent or 0) and stripped.startswith("- "):
        field_name = strip_quotes(stripped[2:])
        snapshot.schema_required.setdefault(collecting_required_schema, set()).add(field_name)
        continue
      if indent <= (required_indent or 0):
        collecting_required_schema = None
        required_indent = None

    if indent == 0:
      key = stripped.split(":", 1)[0]
      in_paths = key == "paths"
      in_components = key == "components"
      in_schemas = False
      current_path = None
      current_method = None
      current_schema = None
      continue

    if in_paths:
      if indent == 2 and stripped.startswith("/"):
        current_path = strip_quotes(stripped.split(":", 1)[0])
        current_method = None
        continue

      if current_path and indent == 4 and ":" in stripped:
        method = stripped.split(":", 1)[0].lower()
        if method in HTTP_METHODS:
          current_method = method
          snapshot.operations[(current_path, method)] = Operation()
          continue

      if current_path and current_method and indent >= 6 and stripped.startswith("operationId:"):
        operation_id = strip_quotes(stripped.split(":", 1)[1])
        snapshot.operations[(current_path, current_method)].operation_id = operation_id
        continue

    if in_components:
      if indent == 2:
        in_schemas = stripped == "schemas:"
        current_schema = None
        continue

      if in_schemas and indent == 4 and re.match(r"^[A-Za-z0-9_.-]+:\s*$", stripped):
        current_schema = stripped.split(":", 1)[0]
        snapshot.schemas.add(current_schema)
        snapshot.schema_required.setdefault(current_schema, set())
        continue

      if in_schemas and current_schema and indent == 6 and stripped.startswith("required:"):
        required_value = stripped.split(":", 1)[1]
        inline_required = parse_inline_required(required_value)
        if inline_required:
          snapshot.schema_required.setdefault(current_schema, set()).update(inline_required)
        else:
          collecting_required_schema = current_schema
          required_indent = indent
        continue

  return snapshot


def operation_key(path: str, method: str) -> str:
  return f"{method.upper()} {path}"


def compare_specs(
    base: OpenApiSnapshot,
    head: OpenApiSnapshot,
    allowed_removed_paths: Iterable[str] = (),
    allowed_removed_operations: Iterable[str] = (),
    allowed_operation_id_changes: Iterable[str] = (),
    allowed_required_additions: Iterable[str] = (),
) -> List[str]:
  issues: List[str] = []
  allowed_removed_path_set = set(allowed_removed_paths)
  allowed_removed_operation_set = set(allowed_removed_operations)
  allowed_operation_id_change_set = set(allowed_operation_id_changes)
  allowed_required_addition_set = set(allowed_required_additions)

  for path, method in sorted(base.operations):
    key = operation_key(path, method)
    if (path, method) not in head.operations:
      if path not in allowed_removed_path_set and key not in allowed_removed_operation_set:
        issues.append(f"Removed operation: {key}")
      continue

    base_operation_id = base.operations[(path, method)].operation_id
    head_operation_id = head.operations[(path, method)].operation_id
    if (
        base_operation_id
        and head_operation_id
        and base_operation_id != head_operation_id
        and key not in allowed_operation_id_change_set
    ):
      issues.append(
          f"Changed operationId for {key}: {base_operation_id} -> {head_operation_id}"
      )

  for schema in sorted(base.schemas):
    if schema not in head.schemas:
      issues.append(f"Removed schema: {schema}")
      continue

    added_required = head.schema_required.get(schema, set()) - base.schema_required.get(
        schema, set()
    )
    for field_name in sorted(added_required):
      key = f"{schema}.{field_name}"
      if key not in allowed_required_addition_set:
        issues.append(f"Added required field to existing schema: {key}")

  return issues


def build_parser() -> argparse.ArgumentParser:
  parser = argparse.ArgumentParser(
      description="Check Apollo OpenAPI specs for incompatible v1 changes."
  )
  parser.add_argument("--base", required=True, help="Baseline OpenAPI spec path or URL")
  parser.add_argument("--head", required=True, help="Candidate OpenAPI spec path or URL")
  parser.add_argument(
      "--allow-removed-path",
      action="append",
      default=[],
      help="Allow every operation under this removed path",
  )
  parser.add_argument(
      "--allow-removed-operation",
      action="append",
      default=[],
      help='Allow one removed operation, formatted like "GET /openapi/v1/apps"',
  )
  parser.add_argument(
      "--allow-operation-id-change",
      action="append",
      default=[],
      help='Allow one operationId change, formatted like "GET /openapi/v1/apps"',
  )
  parser.add_argument(
      "--allow-required-addition",
      action="append",
      default=[],
      help='Allow one new required field, formatted like "OpenAppDTO.appId"',
  )
  return parser


def main(argv: Optional[List[str]] = None) -> int:
  args = build_parser().parse_args(argv)

  base = parse_spec(load_text(args.base))
  head = parse_spec(load_text(args.head))
  issues = compare_specs(
      base,
      head,
      allowed_removed_paths=args.allow_removed_path,
      allowed_removed_operations=args.allow_removed_operation,
      allowed_operation_id_changes=args.allow_operation_id_change,
      allowed_required_additions=args.allow_required_addition,
  )

  if issues:
    print("OpenAPI compatibility check failed:")
    for issue in issues:
      print(f"- {issue}")
    return 1

  print(
      "OpenAPI compatibility check passed: "
      f"{len(base.operations)} operations and {len(base.schemas)} schemas compared."
  )
  return 0


if __name__ == "__main__":
  sys.exit(main())
