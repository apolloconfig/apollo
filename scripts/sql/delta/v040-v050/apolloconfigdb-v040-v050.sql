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
# delta schema to upgrade apollo config db from v0.4.0 to v0.5.0

Use ApolloConfigDB;

DROP TABLE `Privilege`;
ALTER TABLE `Release` DROP `Status`;
ALTER TABLE `Namespace` ADD KEY `IX_NamespaceName` (`NamespaceName`(191));
ALTER TABLE `Cluster` ADD KEY `IX_ParentClusterId` (`ParentClusterId`);
ALTER TABLE `AppNamespace` ADD KEY `IX_AppId` (`AppId`);
ALTER TABLE `App` DROP INDEX `Name`;
ALTER TABLE `App` ADD KEY `Name` (`Name`);