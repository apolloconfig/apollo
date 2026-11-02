--
-- Copyright 2026 Apollo Authors
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

INSERT INTO "Consumer" (Id, AppId, Name, OrgId, OrgName, OwnerName, OwnerEmail,
  DataChange_CreatedBy, DataChange_LastModifiedBy) VALUES
  (1000, 'role-test-consumer', 'Role test consumer', 'TEST', 'Test', 'apollo', 'apollo@example.com', 'apollo', 'apollo'),
  (1001, 'role-test-other-consumer', 'Other consumer', 'TEST', 'Test', 'apollo', 'apollo@example.com', 'apollo', 'apollo');

INSERT INTO "Role" (Id, RoleName, DataChange_CreatedBy, DataChange_LastModifiedBy) VALUES
  (1000, 'Master+role-test-app', 'apollo', 'apollo'),
  (1001, 'ModifyNamespace+role-test-app+application', 'apollo', 'apollo'),
  (1002, 'ReleaseNamespace+role-test-app+application', 'apollo', 'apollo'),
  (1003, 'ModifyNamespace+role-test-app+application+DEV', 'apollo', 'apollo'),
  (1004, 'ReleaseNamespace+role-test-app+application+DEV', 'apollo', 'apollo'),
  (1005, 'ModifyNamespace+role-test-app+application+PRO', 'apollo', 'apollo'),
  (1006, 'ReleaseNamespace+role-test-app+application+PRO', 'apollo', 'apollo'),
  (1007, 'CreateApplication+SystemRole', 'apollo', 'apollo'),
  (1008, 'ManageUsers+SystemRole', 'apollo', 'apollo');

INSERT INTO "ConsumerRole" (Id, ConsumerId, RoleId, DataChange_CreatedBy, DataChange_LastModifiedBy) VALUES
  (1000, 1000, 1000, 'apollo', 'apollo'),
  (1001, 1000, 1001, 'apollo', 'apollo'),
  (1002, 1000, 1002, 'apollo', 'apollo'),
  (1003, 1000, 1003, 'apollo', 'apollo'),
  (1004, 1000, 1004, 'apollo', 'apollo'),
  (1005, 1000, 1005, 'apollo', 'apollo'),
  (1006, 1000, 1006, 'apollo', 'apollo'),
  (1007, 1000, 1007, 'apollo', 'apollo'),
  (1008, 1000, 1008, 'apollo', 'apollo'),
  (1010, 1001, 1003, 'apollo', 'apollo'),
  (1011, 1001, 1004, 'apollo', 'apollo');

INSERT INTO "Permission" (Id, PermissionType, TargetId, DataChange_CreatedBy, DataChange_LastModifiedBy)
  VALUES (1000, 'ModifyNamespace', 'role-test-app+application+DEV', 'apollo', 'apollo');
INSERT INTO "RolePermission" (Id, RoleId, PermissionId, DataChange_CreatedBy, DataChange_LastModifiedBy)
  VALUES (1000, 1003, 1000, 'apollo', 'apollo');
INSERT INTO "UserRole" (Id, UserId, RoleId, DataChange_CreatedBy, DataChange_LastModifiedBy)
  VALUES (1000, 'apollo', 1003, 'apollo', 'apollo');
