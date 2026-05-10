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

"""Collect Apollo Portal frontend service URLs for OpenAPI migration tracking."""

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
    r"apollo|appnamespaces|apps|consumer-tokens|consumers|envs|favorites|global-search|"
    r"import|namespaces|openapi|page-settings|permissions|server|system|system-info|user|users"
    r")\b"
)
STRING_RE = re.compile(r"""(['"])(.*?)\1""")


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
  parts = [value for _, value in STRING_RE.findall(expression) if is_path_literal(value)]
  if not parts:
    return None
  return normalize_path("".join(parts))


def find_action(lines: Sequence[str], line_index: int) -> str:
  for index in range(line_index, max(-1, line_index - 12), -1):
    match = re.search(r"^\s*([A-Za-z0-9_$]+):\s*\{\s*$", lines[index])
    if match:
      return match.group(1)
  return "-"


def find_method(lines: Sequence[str], line_index: int) -> str:
  for index in range(line_index, max(-1, line_index - 12), -1):
    match = re.search(r"method:\s*['\"]([A-Z]+)['\"]", lines[index])
    if match:
      return match.group(1)
  return "RESOURCE_BASE"


def collect_url_expression(lines: Sequence[str], line_index: int) -> str:
  expression_lines = [lines[line_index].split("url:", 1)[1]]
  index = line_index
  while index + 1 < len(lines):
    next_line = lines[index + 1]
    expression = "\n".join(expression_lines)
    if extract_path(expression) and not next_line.lstrip().startswith("+"):
      break
    if not next_line.lstrip().startswith("+"):
      break
    index += 1
    expression_lines.append(lines[index])
  return "\n".join(expression_lines)


def collect_resource_expression(line: str) -> Optional[str]:
  marker = "$resource("
  if marker not in line:
    return None
  expression = line.split(marker, 1)[1]
  if "," in expression:
    expression = expression.split(",", 1)[0]
  return expression


def collect_service_urls(service_file: Path) -> List[FrontendUrl]:
  lines = service_file.read_text(encoding="utf-8").splitlines()
  urls: List[FrontendUrl] = []
  service = service_file.name
  seen = set()

  for line_index, line in enumerate(lines):
    expression = None
    action = "-"
    method = "RESOURCE_BASE"
    if "url:" in line:
      expression = collect_url_expression(lines, line_index)
      action = find_action(lines, line_index)
      method = find_method(lines, line_index)
    elif "$resource(" in line:
      expression = collect_resource_expression(line)

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


def collect_urls(services_dir: Path) -> List[FrontendUrl]:
  urls: List[FrontendUrl] = []
  for service_file in sorted(services_dir.glob("*.js")):
    urls.extend(collect_service_urls(service_file))
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
      "| Service | OpenAPI | WebAPI | No prefix | Total |",
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
      "| Service | Line | Action | Method | Surface | Prefix path | Path |",
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
        "Portal 前端 service 到 OpenAPI 的迁移进度。迁移完成后应删除。"
    )
    summary_title = "## 汇总"
    service_title = "## 按 Service 汇总"
    inventory_title = "## URL 清单"
    summary = [
        f"- Service 文件数：{service_count}",
        f"- URL 条目数：{len(urls)}",
        f"- OpenAPI 条目数：{surface_count['OpenAPI']}",
        f"- WebAPI 条目数：{surface_count['WebAPI']}",
        f"- 未使用 `AppUtil.prefixPath()` 的条目数：{no_prefix_count}",
    ]
  else:
    title = "Apollo Portal Frontend URL Migration Inventory (Temporary)"
    intro = (
        "This document is generated by `scripts/openapi/collect_portal_frontend_urls.py` "
        "to track Portal frontend service migration toward OpenAPI. Delete it after the "
        "migration is complete."
    )
    summary_title = "## Summary"
    service_title = "## By Service"
    inventory_title = "## URL Inventory"
    summary = [
        f"- Service files: {service_count}",
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

  urls = collect_urls(services_dir)
  markdown = render_markdown(urls, args.language)
  if args.output:
    Path(args.output).write_text(markdown, encoding="utf-8")
  else:
    print(markdown, end="")
  return 0


if __name__ == "__main__":
  raise SystemExit(main())
