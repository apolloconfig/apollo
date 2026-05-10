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

import unittest

from check_openapi_compatibility import compare_specs, parse_spec


BASE_SPEC = """
openapi: 3.0.1
paths:
  /openapi/v1/apps:
    get:
      operationId: findApps
      responses:
        "200":
          description: ok
    post:
      operationId: createApp
      responses:
        "200":
          description: ok
  /openapi/v1/apps/{appId}:
    get:
      operationId: getApp
      responses:
        "200":
          description: ok
components:
  schemas:
    OpenAppDTO:
      type: object
      required:
        - appId
      properties:
        appId:
          type: string
        name:
          type: string
    OpenClusterDTO:
      type: object
      required: [name]
      properties:
        name:
          type: string
"""


class CheckOpenApiCompatibilityTest(unittest.TestCase):

  def test_allows_additive_paths_and_optional_fields(self):
    head_spec = BASE_SPEC + """
  /openapi/v1/envs:
    get:
      operationId: getEnvs
      responses:
        "200":
          description: ok
"""
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertEqual([], issues)

  def test_rejects_removed_operations(self):
    head_spec = """
openapi: 3.0.1
paths:
  /openapi/v1/apps:
    get:
      operationId: findApps
components:
  schemas:
    OpenAppDTO:
      type: object
      required:
        - appId
    OpenClusterDTO:
      type: object
      required: [name]
"""
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertIn("Removed operation: POST /openapi/v1/apps", issues)
    self.assertIn("Removed operation: GET /openapi/v1/apps/{appId}", issues)

  def test_rejects_operation_id_changes(self):
    head_spec = BASE_SPEC.replace("operationId: findApps", "operationId: listApps")
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertEqual(
        ["Changed operationId for GET /openapi/v1/apps: findApps -> listApps"], issues
    )

  def test_rejects_required_field_additions(self):
    head_spec = BASE_SPEC.replace(
        """      required:
        - appId""",
        """      required:
        - appId
        - ownerName""",
    )
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertEqual(["Added required field to existing schema: OpenAppDTO.ownerName"], issues)

  def test_allows_explicit_compatibility_exceptions(self):
    head_spec = BASE_SPEC.replace("operationId: findApps", "operationId: listApps")
    issues = compare_specs(
        parse_spec(BASE_SPEC),
        parse_spec(head_spec),
        allowed_operation_id_changes=["GET /openapi/v1/apps"],
    )
    self.assertEqual([], issues)


if __name__ == "__main__":
  unittest.main()
