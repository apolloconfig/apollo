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

  def test_rejects_removed_operation_id(self):
    head_spec = BASE_SPEC.replace("      operationId: findApps\n", "")
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertEqual(
        ["Removed operationId for GET /openapi/v1/apps: findApps"], issues
    )

  def test_parses_quoted_paths_with_non_standard_indentation(self):
    spec = """
openapi: 3.0.1
paths:
    "/openapi/v1/apps":
        get:
            operationId: findApps
            responses:
                "200":
                    description: ok
components:
    schemas:
        OpenAppDTO:
            type: object
"""
    snapshot = parse_spec(spec)
    self.assertIn(("/openapi/v1/apps", "get"), snapshot.operations)
    self.assertEqual(
        "findApps", snapshot.operations[("/openapi/v1/apps", "get")].operation_id
    )
    self.assertIn("OpenAppDTO", snapshot.schemas)

  def test_rejects_response_schema_changes(self):
    base_spec = """
openapi: 3.0.1
paths:
  /openapi/v1/apps:
    get:
      operationId: findApps
      responses:
        "200":
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/OpenAppDTO"
components:
  schemas:
    OpenAppDTO:
      type: object
"""
    head_spec = base_spec.replace("OpenAppDTO", "OpenAppSummaryDTO", 1)
    issues = compare_specs(parse_spec(base_spec), parse_spec(head_spec))
    self.assertEqual(1, len(issues))
    self.assertTrue(issues[0].startswith("Changed response schemas for GET /openapi/v1/apps:"))

  def test_rejects_request_schema_changes(self):
    base_spec = """
openapi: 3.0.1
paths:
  /openapi/v1/apps:
    post:
      operationId: createApp
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/OpenAppDTO"
      responses:
        "200":
          description: ok
components:
  schemas:
    OpenAppDTO:
      type: object
"""
    head_spec = base_spec.replace("OpenAppDTO", "OpenAppSummaryDTO", 1)
    issues = compare_specs(parse_spec(base_spec), parse_spec(head_spec))
    self.assertEqual(1, len(issues))
    self.assertTrue(issues[0].startswith("Changed request schema for POST /openapi/v1/apps:"))

  def test_rejects_optional_property_removal(self):
    head_spec = BASE_SPEC.replace(
        """        name:
          type: string
""",
        "",
        1,
    )
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertEqual(["Removed property from existing schema: OpenAppDTO.name"], issues)

  def test_rejects_property_schema_changes(self):
    head_spec = BASE_SPEC.replace(
        """        name:
          type: string""",
        """        name:
          type: integer""",
        1,
    )
    issues = compare_specs(parse_spec(BASE_SPEC), parse_spec(head_spec))
    self.assertEqual(1, len(issues))
    self.assertTrue(
        issues[0].startswith(
            "Changed property schema for existing schema: OpenAppDTO.name:"
        )
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
