--
-- Copyright 2022 Apollo Authors
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
# delta schema to upgrade apollo config db from v2.1.0 to v2.2.0

Use ApolloConfigDB;

ALTER TABLE `App`
    MODIFY COLUMN `AppId` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'AppID';

ALTER TABLE `Commit`
    MODIFY COLUMN `AppId` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'AppID';

ALTER TABLE `Namespace`
    MODIFY COLUMN `AppId` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'AppID';

ALTER TABLE `Release`
    MODIFY COLUMN `AppId` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'AppID';

ALTER TABLE `AccessKey`
    MODIFY COLUMN `AppId` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'AppID';

ALTER TABLE `Commit`
    DROP INDEX `AppId`,
    ADD INDEX `AppId` (`AppId`);

ALTER TABLE `Namespace`
    DROP INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt`,
    ADD UNIQUE INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt` (`AppId`,`ClusterName`(191),`NamespaceName`(191),`DeletedAt`);

ALTER TABLE `Release`
    DROP INDEX `AppId_ClusterName_GroupName`,
    ADD  INDEX `AppId_ClusterName_GroupName` (`AppId`,`ClusterName`(191),`NamespaceName`(191),`DeletedAt`);