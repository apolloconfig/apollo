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
-- delta schema to upgrade apollo config db from v2.3.0 to v2.4.0

-- 
-- ===============================================================================
-- ==                                                                           ==
-- ==                     Generated from 'scripts/sql/src/'                     ==
-- == by running 'mvn compile -pl apollo-build-sql-converter -Psql-converter'.  ==
-- ==                              DO NOT EDIT !!!                              ==
-- ==                                                                           ==
-- ===============================================================================
-- 
-- 

ALTER TABLE `AccessKey`
    ADD COLUMN `Mode` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '密钥模式，0: filter，1: observer' AFTER `Secret`;

ALTER TABLE `Commit`
    MODIFY COLUMN `ClusterName` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'Cluster Name',
    MODIFY COLUMN `NamespaceName` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'Namespace Name';
ALTER TABLE `Namespace`
    MODIFY COLUMN `ClusterName` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'Cluster Name',
    MODIFY COLUMN `NamespaceName` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'Namespace Name';
ALTER TABLE `Release`
    MODIFY COLUMN `ClusterName` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'Cluster Name',
    MODIFY COLUMN `NamespaceName` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT 'Namespace Name';

ALTER TABLE `Commit`
    DROP INDEX `ClusterName`,
    DROP INDEX `NamespaceName`,
    ADD INDEX `ClusterName` (`ClusterName`),
    ADD INDEX `NamespaceName` (`NamespaceName`);
ALTER TABLE `Namespace`
    DROP INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt`,
    DROP INDEX `IX_NamespaceName`,
    ADD UNIQUE INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt` (`AppId`,`ClusterName`,`NamespaceName`,`DeletedAt`),
    ADD INDEX `IX_NamespaceName` (`NamespaceName`);
ALTER TABLE `Release`
    DROP INDEX `AppId_ClusterName_GroupName`,
    ADD INDEX `AppId_ClusterName_GroupName` (`AppId`,`ClusterName`,`NamespaceName`);

-- 
-- ===============================================================================
-- ==                                                                           ==
-- ==                     Generated from 'scripts/sql/src/'                     ==
-- == by running 'mvn compile -pl apollo-build-sql-converter -Psql-converter'.  ==
-- ==                              DO NOT EDIT !!!                              ==
-- ==                                                                           ==
-- ===============================================================================
