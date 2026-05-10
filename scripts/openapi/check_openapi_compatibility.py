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

The script uses PyYAML so it can read the OpenAPI document as structured data
instead of relying on indentation-sensitive text matching. It performs a focused
structural scan
for the compatibility rules Apollo cares about first:

* existing paths and HTTP methods must remain available;
* existing operationId values must remain stable;
* existing operation request/response schema references must remain stable;
* existing schemas must remain available;
* existing schema properties must remain available and keep the same shape;
* existing schemas must not gain new required fields by default.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass, field
import json
from pathlib import Path
import sys
from typing import Any, Dict, Iterable, List, Optional, Set, Tuple
from urllib.request import urlopen

try:
  import yaml
except ImportError as exc:
  raise SystemExit("PyYAML is required. Install it with `python3 -m pip install pyyaml`.") from exc


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
  request_schema: Optional[str] = None
  response_schemas: Tuple[Tuple[str, str, str], ...] = field(default_factory=tuple)


@dataclass
class OpenApiSnapshot:
  operations: Dict[Tuple[str, str], Operation] = field(default_factory=dict)
  schemas: Set[str] = field(default_factory=set)
  schema_required: Dict[str, Set[str]] = field(default_factory=dict)
  schema_properties: Dict[str, Dict[str, str]] = field(default_factory=dict)


def load_text(source: str) -> str:
  if source.startswith("http://") or source.startswith("https://"):
    with urlopen(source, timeout=30) as response:
      return response.read().decode("utf-8")
  return Path(source).read_text(encoding="utf-8")


def schema_signature(schema: Any) -> Optional[str]:
  if not isinstance(schema, dict):
    return None

  def normalize(value: Any) -> Any:
    if isinstance(value, dict):
      return {key: normalize(value[key]) for key in sorted(value)}
    if isinstance(value, list):
      return [normalize(item) for item in value]
    return value

  keys = (
      "$ref",
      "type",
      "format",
      "nullable",
      "enum",
      "items",
      "additionalProperties",
      "allOf",
      "anyOf",
      "oneOf",
  )
  signature = {key: normalize(schema[key]) for key in keys if key in schema}
  if not signature:
    return None
  return json.dumps(signature, sort_keys=True, separators=(",", ":"))


def extract_request_schema(operation: Dict[str, Any]) -> Optional[str]:
  request_body = operation.get("requestBody")
  if not isinstance(request_body, dict):
    return None
  content = request_body.get("content")
  if not isinstance(content, dict):
    return None
  schemas = []
  for media_type, media_type_config in sorted(content.items()):
    if not isinstance(media_type_config, dict):
      continue
    signature = schema_signature(media_type_config.get("schema"))
    if signature:
      schemas.append((media_type, signature))
  if not schemas:
    return None
  return json.dumps(schemas, sort_keys=True, separators=(",", ":"))


def extract_response_schemas(operation: Dict[str, Any]) -> Tuple[Tuple[str, str, str], ...]:
  responses = operation.get("responses")
  if not isinstance(responses, dict):
    return ()

  schemas = []
  for status_code, response in sorted(responses.items(), key=lambda item: str(item[0])):
    if not isinstance(response, dict):
      continue
    content = response.get("content")
    if not isinstance(content, dict):
      continue
    for media_type, media_type_config in sorted(content.items()):
      if not isinstance(media_type_config, dict):
        continue
      signature = schema_signature(media_type_config.get("schema"))
      if signature:
        schemas.append((str(status_code), media_type, signature))
  return tuple(schemas)


def parse_spec(text: str) -> OpenApiSnapshot:
  snapshot = OpenApiSnapshot()
  spec = yaml.safe_load(text) or {}
  if not isinstance(spec, dict):
    return snapshot

  paths = spec.get("paths") or {}
  if isinstance(paths, dict):
    for path, path_item in paths.items():
      if not isinstance(path_item, dict):
        continue
      for method, operation in path_item.items():
        method = str(method).lower()
        if method not in HTTP_METHODS or not isinstance(operation, dict):
          continue
        snapshot.operations[(str(path), method)] = Operation(
            operation_id=operation.get("operationId"),
            request_schema=extract_request_schema(operation),
            response_schemas=extract_response_schemas(operation),
        )

  schemas = ((spec.get("components") or {}).get("schemas") or {})
  if isinstance(schemas, dict):
    for schema_name, schema in schemas.items():
      schema_name = str(schema_name)
      snapshot.schemas.add(schema_name)
      snapshot.schema_required.setdefault(schema_name, set())
      snapshot.schema_properties.setdefault(schema_name, {})
      if not isinstance(schema, dict):
        continue

      required = schema.get("required") or []
      if isinstance(required, list):
        snapshot.schema_required[schema_name].update(str(field_name) for field_name in required)

      properties = schema.get("properties") or {}
      if isinstance(properties, dict):
        for property_name, property_schema in properties.items():
          signature = schema_signature(property_schema)
          if signature:
            snapshot.schema_properties[schema_name][str(property_name)] = signature

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
        and base_operation_id != head_operation_id
        and key not in allowed_operation_id_change_set
    ):
      if head_operation_id:
        issues.append(
            f"Changed operationId for {key}: {base_operation_id} -> {head_operation_id}"
        )
      else:
        issues.append(f"Removed operationId for {key}: {base_operation_id}")

    base_request_schema = base.operations[(path, method)].request_schema
    head_request_schema = head.operations[(path, method)].request_schema
    if base_request_schema and base_request_schema != head_request_schema:
      issues.append(
          f"Changed request schema for {key}: {base_request_schema} -> "
          f"{head_request_schema}"
      )

    base_response_schemas = base.operations[(path, method)].response_schemas
    head_response_schemas = head.operations[(path, method)].response_schemas
    if base_response_schemas and base_response_schemas != head_response_schemas:
      issues.append(
          f"Changed response schemas for {key}: {base_response_schemas} -> "
          f"{head_response_schemas}"
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

    base_properties = base.schema_properties.get(schema, {})
    head_properties = head.schema_properties.get(schema, {})
    for property_name, base_property_signature in sorted(base_properties.items()):
      key = f"{schema}.{property_name}"
      if property_name not in head_properties:
        issues.append(f"Removed property from existing schema: {key}")
        continue
      head_property_signature = head_properties[property_name]
      if base_property_signature != head_property_signature:
        issues.append(
            f"Changed property schema for existing schema: {key}: "
            f"{base_property_signature} -> {head_property_signature}"
        )

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
  if not base.operations:
    print(f"No OpenAPI operations found in base spec: {args.base}", file=sys.stderr)
    return 1
  if not head.operations:
    print(f"No OpenAPI operations found in head spec: {args.head}", file=sys.stderr)
    return 1

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
