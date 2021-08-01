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
# delta schema to upgrade apollo config db from v1.8.0 to v1.9.0

Use ApolloConfigDB;

ALTER TABLE `App`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `AppNamespace`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Audit`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Cluster`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Commit`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `GrayReleaseRule`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Item`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Namespace`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `NamespaceLock`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Release`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `ReleaseHistory`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `ServerConfig`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `AccessKey`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on seconds' AFTER `IsDeleted`,
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';
