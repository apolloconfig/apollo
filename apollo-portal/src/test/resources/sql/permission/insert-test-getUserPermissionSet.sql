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

-- Clean up possible legacy data (optional, will be done in cleanup.sql, written again here for safety)
DELETE FROM `RolePermission` WHERE `RoleId` IN (2001, 2002);
DELETE FROM `UserRole`      WHERE `RoleId` IN (2001, 2002);
DELETE FROM `Permission`    WHERE `Id` IN (1001, 1002);
DELETE FROM `Role`          WHERE `Id` IN (2001, 2002);

-- Permissions
INSERT INTO `Permission` (`Id`, `PermissionType`, `TargetId`, `IsDeleted`, `DeletedAt`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
    (1001, 'ModifyNamespace', 'someApp+someNamespace', 0, 0, 'test', NOW(), 'test', NOW()),
    (1002, 'ReleaseNamespace', 'someApp+someNamespace', 0, 0, 'test', NOW(), 'test', NOW());

-- Roles
INSERT INTO `Role` (`Id`, `RoleName`, `IsDeleted`, `DeletedAt`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
    (2001, 'role-with-modify', 0, 0, 'test', NOW(), 'test', NOW()),
    (2002, 'role-with-release', 0, 0, 'test', NOW(), 'test', NOW());

-- Role-Permission associations
INSERT INTO `RolePermission` (`RoleId`, `PermissionId`, `IsDeleted`, `DeletedAt`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
    (2001, 1001, 0, 0, 'test', NOW(), 'test', NOW()),
    (2002, 1002, 0, 0, 'test', NOW(), 'test', NOW());

-- User-Role associations (apollo has two permissions, nobody has no roles)
INSERT INTO `UserRole` (`UserId`, `RoleId`, `IsDeleted`, `DeletedAt`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
    ('apollo', 2001, 0, 0, 'test', NOW(), 'test', NOW()),
    ('apollo', 2002, 0, 0, 'test', NOW(), 'test', NOW());