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

ALTER TABLE `App`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `AppNamespace`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Consumer`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `ConsumerRole`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `ConsumerToken`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Favorite`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Permission`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `Role`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `RolePermission`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `ServerConfig`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;

ALTER TABLE `UserRole`
    ADD COLUMN `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds' AFTER `IsDeleted`;