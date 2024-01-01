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
# delta schema to upgrade apollo config db from v1.9.0 to v2.0.0

Use ApolloConfigDB;

ALTER TABLE `App`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `AppNamespace`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Audit`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Cluster`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Commit`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `GrayReleaseRule`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Item`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`,
    ADD INDEX IX_key (`Key`);

ALTER TABLE `Namespace`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `NamespaceLock`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Release`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `ReleaseHistory`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `ServerConfig`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `AccessKey`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;
