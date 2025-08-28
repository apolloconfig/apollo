--
-- Copyright 2025 Apollo Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- User 7's permissions: app:app2
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES (1003, 7, 400, 'test-operator', 'test-operator');

INSERT INTO "RolePermission" ("RoleId", "PermissionId")
VALUES (400, 401);

INSERT INTO "Permission" ("Id", "PermissionType", "TargetId")
VALUES (401, 'app', 'app2');

-- User 8's permissions: namespace:ns2, cluster:cluster2
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES
    (1004, 8, 500, 'test-operator', 'test-operator'),
    (1005, 8, 501, 'test-operator', 'test-operator');

INSERT INTO "RolePermission" ("RoleId", "PermissionId")
VALUES
    (500, 502),
    (501, 503);

INSERT INTO "Permission" ("Id", "PermissionType", "TargetId")
VALUES
    (502, 'namespace', 'ns2'),
    (503, 'cluster', 'cluster2');