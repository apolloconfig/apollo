--
-- Copyright 2024 Apollo Authors
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
# delta schema to upgrade apollo portal db from v1.9.0 to v2.0.0

Use ApolloPortalDB;

-- the follow DML won't change the `DataChange_LastTime` field
UPDATE `App` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `AppNamespace` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Consumer` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ConsumerRole` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ConsumerToken` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Favorite` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Permission` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Role` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `RolePermission` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ServerConfig` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `UserRole` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;

-- add UNIQUE CONSTRAINT INDEX for each table
ALTER TABLE `App`
    ADD UNIQUE INDEX `UK_AppId_DeletedAt` (`AppId`,`DeletedAt`),
    DROP INDEX `AppId`;

ALTER TABLE `AppNamespace`
    ADD UNIQUE INDEX `UK_AppId_Name_DeletedAt` (`AppId`,`Name`,`DeletedAt`),
    DROP INDEX `IX_AppId`;

ALTER TABLE `Consumer`
    ADD UNIQUE INDEX `UK_AppId_DeletedAt` (`AppId`,`DeletedAt`),
    DROP INDEX `AppId`;

ALTER TABLE `ConsumerRole`
    ADD UNIQUE INDEX `UK_ConsumerId_RoleId_DeletedAt` (`ConsumerId`,`RoleId`,`DeletedAt`),
    DROP INDEX `IX_ConsumerId_RoleId`;

ALTER TABLE `ConsumerToken`
    ADD UNIQUE INDEX `UK_Token_DeletedAt` (`Token`,`DeletedAt`),
    DROP INDEX `IX_Token`;

ALTER TABLE `Favorite`
    ADD UNIQUE INDEX `UK_UserId_AppId_DeletedAt` (`UserId`,`AppId`,`DeletedAt`),
    DROP INDEX `IX_UserId`;

ALTER TABLE `Permission`
    ADD UNIQUE INDEX `UK_TargetId_PermissionType_DeletedAt` (`TargetId`,`PermissionType`,`DeletedAt`),
    DROP INDEX `IX_TargetId_PermissionType`;

ALTER TABLE `Role`
    ADD UNIQUE INDEX `UK_RoleName_DeletedAt` (`RoleName`,`DeletedAt`),
    DROP INDEX `IX_RoleName`;

ALTER TABLE `RolePermission`
    ADD UNIQUE INDEX `UK_RoleId_PermissionId_DeletedAt` (`RoleId`,`PermissionId`,`DeletedAt`),
    DROP INDEX `IX_RoleId`;

ALTER TABLE `ServerConfig`
    ADD UNIQUE INDEX `UK_Key_DeletedAt` (`Key`,`DeletedAt`),
    DROP INDEX `IX_Key`;

ALTER TABLE `UserRole`
    ADD UNIQUE INDEX `UK_UserId_RoleId_DeletedAt` (`UserId`,`RoleId`,`DeletedAt`),
    DROP INDEX `IX_UserId_RoleId`;

ALTER TABLE `Users`
    ADD UNIQUE INDEX `UK_Username` (`Username`);
