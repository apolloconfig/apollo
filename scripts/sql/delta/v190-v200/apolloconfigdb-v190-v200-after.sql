--
-- Copyright 2021 Apollo Authors
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
# delta schema to upgrade apollo config db from v1.9.0 to v2.0.0

Use ApolloConfigDB;

-- the follow DML won't change the `DataChange_LastTime` field
UPDATE `AccessKey` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `App` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `AppNamespace` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Audit` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Cluster` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Commit` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `GrayReleaseRule` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Item` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Namespace` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `NamespaceLock` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Release` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ReleaseHistory` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ServerConfig` SET `DeletedAt` = -Id, `DataChange_LastTime` = `DataChange_LastTime` WHERE `IsDeleted` = 1 and `DeletedAt` = 0;

-- add UNIQUE CONSTRAINT INDEX for each table
ALTER TABLE `AccessKey`
    ADD UNIQUE INDEX `UK_AppId_Secret_DeletedAt` (`AppId`,`Secret`,`DeletedAt`),
    DROP INDEX `AppId`;

ALTER TABLE `App`
    ADD UNIQUE INDEX `UK_AppId_DeletedAt` (`AppId`,`DeletedAt`),
    DROP INDEX `AppId`;

ALTER TABLE `AppNamespace`
    ADD UNIQUE INDEX `UK_AppId_Name_DeletedAt` (`AppId`,`Name`,`DeletedAt`),
    DROP INDEX `IX_AppId`;

-- Ignore TABLE `Audit`

ALTER TABLE `Cluster`
    ADD UNIQUE INDEX `UK_AppId_Name_DeletedAt` (`AppId`,`Name`,`DeletedAt`),
    DROP INDEX `IX_AppId_Name`;

-- Ignore TABLE `Commit`

ALTER TABLE `GrayReleaseRule`
    ADD UNIQUE INDEX `UK_AppId_ClusterName_NamespaceName_BranchName_DeletedAt` (`AppId`,`ClusterName`,`NamespaceName`,`BranchName`,`DeletedAt`),
    DROP INDEX `IX_Namespace`;

ALTER TABLE `Item`
    ADD UNIQUE INDEX `UK_NamespaceId_LineNum_Key_DeletedAt` (`NamespaceId`,`Key`,`LineNum`,`DeletedAt`),
    DROP INDEX `IX_GroupId`;

ALTER TABLE `Namespace`
    ADD UNIQUE INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt` (`AppId`(191),`ClusterName`(191),`NamespaceName`(191),`DeletedAt`),
    DROP INDEX `AppId_ClusterName_NamespaceName`;

ALTER TABLE `NamespaceLock`
    ADD UNIQUE INDEX `UK_NamespaceId_DeletedAt` (`NamespaceId`,`DeletedAt`),
    DROP INDEX `IX_NamespaceId`;

ALTER TABLE `Release`
    ADD UNIQUE INDEX `UK_ReleaseKey_DeletedAt` (`ReleaseKey`,`DeletedAt`),
    DROP INDEX `IX_ReleaseKey`;

-- Ignore TABLE `ReleaseHistory`

ALTER TABLE `ServerConfig`
    ADD UNIQUE INDEX `UK_Key_Cluster_DeletedAt` (`Key`,`Cluster`,`DeletedAt`),
    DROP INDEX `IX_Key`;
