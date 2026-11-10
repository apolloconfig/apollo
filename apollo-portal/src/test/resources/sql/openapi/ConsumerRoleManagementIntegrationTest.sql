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
  (1008, 'ManageUsers+SystemRole', 'apollo', 'apollo'),
  -- FAT 与 FWS 是两个不同的环境，角色按原始字符串创建，两者可以同时存在
  (1009, 'ModifyNamespace+role-test-app+application+FAT', 'apollo', 'apollo'),
  (1010, 'ReleaseNamespace+role-test-app+application+FAT', 'apollo', 'apollo'),
  (1011, 'ModifyNamespace+role-test-app+application+FWS', 'apollo', 'apollo'),
  (1012, 'ReleaseNamespace+role-test-app+application+FWS', 'apollo', 'apollo'),
  -- namespace 名带前导空格：RoleName 拼接不 trim，前导空格被原样保留
  (1013, 'ModifyNamespace+role-test-app+ application', 'apollo', 'apollo'),
  (1014, 'ReleaseNamespace+role-test-app+ application', 'apollo', 'apollo'),
  -- namespace 名本身含 env 片段，用于覆盖“指定 env”与“所有环境”不能混淆的场景
  (1015, 'ModifyNamespace+role-test-app+application+DEV+extra', 'apollo', 'apollo');

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
  (1011, 1001, 1004, 'apollo', 'apollo'),
  (1012, 1000, 1009, 'apollo', 'apollo'),
  (1013, 1000, 1010, 'apollo', 'apollo'),
  (1014, 1000, 1011, 'apollo', 'apollo'),
  (1015, 1000, 1012, 'apollo', 'apollo'),
  (1016, 1000, 1013, 'apollo', 'apollo'),
  (1017, 1000, 1014, 'apollo', 'apollo'),
  (1018, 1000, 1015, 'apollo', 'apollo');

INSERT INTO "Permission" (Id, PermissionType, TargetId, DataChange_CreatedBy, DataChange_LastModifiedBy)
  VALUES (1000, 'ModifyNamespace', 'role-test-app+application+DEV', 'apollo', 'apollo');
INSERT INTO "RolePermission" (Id, RoleId, PermissionId, DataChange_CreatedBy, DataChange_LastModifiedBy)
  VALUES (1000, 1003, 1000, 'apollo', 'apollo');
INSERT INTO "UserRole" (Id, UserId, RoleId, DataChange_CreatedBy, DataChange_LastModifiedBy)
  VALUES (1000, 'apollo', 1003, 'apollo', 'apollo');
