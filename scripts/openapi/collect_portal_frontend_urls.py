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

"""Collect Apollo Portal frontend URLs for OpenAPI migration tracking."""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
import re
import sys
from typing import Dict, Iterable, List, Optional, Sequence


PATH_START_RE = re.compile(
    r"^/?(?:"
    r"apollo|appnamespaces|apps|clusters|configs|consumer-tokens|consumers|envs|favorites|"
    r"global-search|import|namespaces|openapi|page-settings|permissions|server|system|"
    r"system-info|user|users"
    r")\b"
)
STRING_RE = re.compile(r"""(['"])(.*?)\1""")
VARIABLE_ASSIGNMENT_RE = re.compile(r"\b(?:var|let|const)\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*=")


@dataclass(frozen=True)
class FrontendUrl:
  service: str
  line: int
  action: str
  method: str
  surface: str
  prefix_path: bool
  path: str


def is_path_literal(value: str) -> bool:
  return bool(value and PATH_START_RE.search(value))


def normalize_path(path: str) -> str:
  if path.startswith("/"):
    return path
  return "/" + path


def extract_path(expression: str) -> Optional[str]:
  matches = list(STRING_RE.finditer(expression))
  first_path_index = None
  for index, match in enumerate(matches):
    if is_path_literal(match.group(2)):
      first_path_index = index
      break
  if first_path_index is None:
    return None

  path = matches[first_path_index].group(2)
  previous_match = matches[first_path_index]
  for match in matches[first_path_index + 1:]:
    value = match.group(2)
    if not is_path_continuation(value, path):
      previous_match = match
      continue
    gap = expression[previous_match.end():match.start()]
    if has_dynamic_gap(gap):
      path = append_dynamic_placeholder(path, value)
    path += value
    previous_match = match

  trailing_gap = expression[previous_match.end():]
  if has_dynamic_gap(trailing_gap):
    path = append_trailing_dynamic_placeholder(path)

  return normalize_path(path)


def is_path_continuation(value: str, path: str) -> bool:
  return (
      value.startswith("/")
      or value.startswith("?")
      or value.startswith("&")
      or path.endswith("/")
      or "?" in path
  )


def has_dynamic_gap(gap: str) -> bool:
  normalized = re.sub(r"[\s+().;'\",{}\[\]]", "", gap)
  return bool(normalized)


def append_dynamic_placeholder(path: str, next_value: str) -> str:
  if path.endswith("/") and next_value.startswith("/"):
    return path + ":param"
  if path.endswith(("=", "/", "?", "&")):
    return path + ":param"
  if next_value.startswith("/") and not path.endswith("/"):
    return path + "/:param"
  return path


def append_trailing_dynamic_placeholder(path: str) -> str:
  if path.endswith(("=", "/", "?", "&")):
    return path + ":param"
  return path


def find_action(lines: Sequence[str], line_index: int) -> str:
  for index in range(line_index, max(-1, line_index - 12), -1):
    match = re.search(r"^\s*([A-Za-z0-9_$]+):\s*\{\s*$", lines[index])
    if match:
      return match.group(1)
  return "-"


def find_method(lines: Sequence[str], line_index: int, default: str = "GET") -> str:
  for index in range(line_index, max(-1, line_index - 12), -1):
    match = re.search(r"method:\s*['\"]([A-Z]+)['\"]", lines[index])
    if match:
      return match.group(1)
  return default


def collect_continued_expression(lines: Sequence[str], line_index: int, expression: str) -> str:
  expression_lines = [expression]
  index = line_index
  while index + 1 < len(lines):
    next_line = lines[index + 1]
    current_expression = "\n".join(expression_lines)
    current_line = expression_lines[-1].rstrip()
    if not current_expression.strip() or current_line.endswith("+") or next_line.lstrip().startswith("+"):
      index += 1
      expression_lines.append(lines[index])
      continue
    if extract_path(current_expression):
      break
    break
  return "\n".join(expression_lines)


def collect_url_expression(lines: Sequence[str], line_index: int) -> str:
  return collect_continued_expression(lines, line_index, lines[line_index].split("url:", 1)[1])


def collect_resource_expression(line: str) -> Optional[str]:
  marker = "$resource("
  if marker not in line:
    return None
  expression = line.split(marker, 1)[1]
  if "," in expression:
    expression = expression.split(",", 1)[0]
  return expression


def collect_variable_assignments(lines: Sequence[str]) -> Dict[str, str]:
  assignments: Dict[str, str] = {}
  for line_index, line in enumerate(lines):
    match = VARIABLE_ASSIGNMENT_RE.search(line)
    if not match:
      continue
    expression = collect_continued_expression(lines, line_index, line.split("=", 1)[1])
    if extract_path(expression):
      assignments[match.group(1)] = expression
  return assignments


def resolve_expression(expression: str, variables: Dict[str, str]) -> str:
  candidate = expression.strip().rstrip(",;")
  return variables.get(candidate, expression)


def collect_file_urls(
    source_file: Path,
    source_name: Optional[str] = None,
    collect_resource_base: bool = True,
) -> List[FrontendUrl]:
  lines = source_file.read_text(encoding="utf-8").splitlines()
  variables = collect_variable_assignments(lines)
  urls: List[FrontendUrl] = []
  service = source_name or source_file.name
  seen = set()

  for line_index, line in enumerate(lines):
    expression = None
    action = "-"
    method = "GET"
    if "url:" in line:
      expression = collect_url_expression(lines, line_index)
      expression = resolve_expression(expression, variables)
      action = find_action(lines, line_index)
      method = find_method(lines, line_index)
    elif "$window.location.href" in line and "=" in line:
      expression = collect_continued_expression(lines, line_index, line.split("=", 1)[1])
      expression = resolve_expression(expression, variables)
      action = "$window.location.href"
      method = "GET"
    elif collect_resource_base and "$resource(" in line:
      expression = collect_resource_expression(line)
      method = "RESOURCE_BASE"

    if not expression:
      continue

    path = extract_path(expression)
    if not path:
      continue

    key = (service, action, method, path, line_index + 1)
    if key in seen:
      continue
    seen.add(key)
    urls.append(
        FrontendUrl(
            service=service,
            line=line_index + 1,
            action=action,
            method=method,
            surface="OpenAPI" if path.startswith("/openapi/") else "WebAPI",
            prefix_path="AppUtil.prefixPath()" in expression,
            path=path,
        )
    )

  return urls


def collect_service_urls(service_file: Path) -> List[FrontendUrl]:
  return collect_file_urls(service_file)


def collect_urls(services_dir: Path, scripts_dir: Optional[Path] = None) -> List[FrontendUrl]:
  urls: List[FrontendUrl] = []
  seen_files = set()
  for service_file in sorted(services_dir.glob("*.js")):
    urls.extend(collect_service_urls(service_file))
    seen_files.add(service_file.resolve())

  if scripts_dir is not None:
    for script_file in sorted(scripts_dir.rglob("*.js")):
      if script_file.resolve() in seen_files:
        continue
      try:
        source_name = str(script_file.relative_to(scripts_dir))
      except ValueError:
        source_name = script_file.name
      urls.extend(
          collect_file_urls(
              script_file,
              source_name=source_name,
              collect_resource_base=False,
          )
      )
  return urls


def markdown_bool(value: bool) -> str:
  return "yes" if value else "no"


def render_summary(urls: Sequence[FrontendUrl]) -> List[str]:
  by_service: Dict[str, Counter] = defaultdict(Counter)
  for url in urls:
    by_service[url.service][url.surface] += 1
    if not url.prefix_path:
      by_service[url.service]["No prefix"] += 1
    by_service[url.service]["Total"] += 1

  lines = [
      "| Source | OpenAPI | WebAPI | No prefix | Total |",
      "| --- | ---: | ---: | ---: | ---: |",
  ]
  for service in sorted(by_service):
    counter = by_service[service]
    lines.append(
        f"| `{service}` | {counter['OpenAPI']} | {counter['WebAPI']} | "
        f"{counter['No prefix']} | {counter['Total']} |"
    )
  return lines


def render_inventory(urls: Sequence[FrontendUrl]) -> List[str]:
  lines = [
      "| Source | Line | Action | Method | Surface | Prefix path | Path |",
      "| --- | ---: | --- | --- | --- | --- | --- |",
  ]
  for url in urls:
    lines.append(
        f"| `{url.service}` | {url.line} | `{url.action}` | `{url.method}` | "
        f"{url.surface} | {markdown_bool(url.prefix_path)} | `{url.path}` |"
    )
  return lines


def render_markdown(urls: Sequence[FrontendUrl], language: str) -> str:
  surface_count = Counter(url.surface for url in urls)
  no_prefix_count = sum(1 for url in urls if not url.prefix_path)
  service_count = len({url.service for url in urls})

  if language == "zh":
    title = "Apollo Portal 前端 URL 迁移清单（临时）"
    intro = (
        "本文档由 `scripts/openapi/collect_portal_frontend_urls.py` 生成，用于跟踪 "
        "Portal 前端 API 调用到 OpenAPI 的迁移进度。迁移完成后应删除。"
    )
    summary_title = "## 汇总"
    service_title = "## 按来源汇总"
    inventory_title = "## URL 清单"
    summary = [
        f"- 前端文件数：{service_count}",
        f"- URL 条目数：{len(urls)}",
        f"- OpenAPI 条目数：{surface_count['OpenAPI']}",
        f"- WebAPI 条目数：{surface_count['WebAPI']}",
        f"- 未使用 `AppUtil.prefixPath()` 的条目数：{no_prefix_count}",
    ]
  else:
    title = "Apollo Portal Frontend URL Migration Inventory (Temporary)"
    intro = (
        "This document is generated by `scripts/openapi/collect_portal_frontend_urls.py` "
        "to track Portal frontend API call migration toward OpenAPI. Delete it after the "
        "migration is complete."
    )
    summary_title = "## Summary"
    service_title = "## By Source"
    inventory_title = "## URL Inventory"
    summary = [
        f"- Frontend files: {service_count}",
        f"- URL entries: {len(urls)}",
        f"- OpenAPI entries: {surface_count['OpenAPI']}",
        f"- WebAPI entries: {surface_count['WebAPI']}",
        f"- Entries without `AppUtil.prefixPath()`: {no_prefix_count}",
    ]

  sections = [
      f"# {title}",
      "",
      intro,
      "",
      summary_title,
      "",
      *summary,
      "",
      service_title,
      "",
      *render_summary(urls),
      "",
      inventory_title,
      "",
      *render_inventory(urls),
      "",
  ]
  return "\n".join(sections)


def parse_args(argv: Optional[Iterable[str]] = None) -> argparse.Namespace:
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument(
      "--services-dir",
      default="apollo-portal/src/main/resources/static/scripts/services",
      help="Path to apollo-portal static service JavaScript directory.",
  )
  parser.add_argument(
      "--scripts-dir",
      default="apollo-portal/src/main/resources/static/scripts",
      help="Path to apollo-portal static JavaScript directory for direct API calls.",
  )
  parser.add_argument(
      "--language",
      choices=("en", "zh"),
      default="zh",
      help="Markdown language to generate.",
  )
  parser.add_argument("--output", help="Output markdown file. Defaults to stdout.")
  return parser.parse_args(argv)


def main(argv: Optional[Iterable[str]] = None) -> int:
  args = parse_args(argv)
  services_dir = Path(args.services_dir)
  if not services_dir.is_dir():
    print(f"--services-dir not found or not a directory: {services_dir}", file=sys.stderr)
    return 1
  scripts_dir = Path(args.scripts_dir)
  if not scripts_dir.is_dir():
    print(f"--scripts-dir not found or not a directory: {scripts_dir}", file=sys.stderr)
    return 1

  urls = collect_urls(services_dir, scripts_dir)
  markdown = render_markdown(urls, args.language)
  if args.output:
    Path(args.output).write_text(markdown, encoding="utf-8")
  else:
    print(markdown, end="")
  return 0


if __name__ == "__main__":
  raise SystemExit(main())
