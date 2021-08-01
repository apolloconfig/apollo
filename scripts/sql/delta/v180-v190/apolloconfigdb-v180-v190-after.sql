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
# delta schema to upgrade apollo config db from v1.8.0 to v1.9.0

Use ApolloConfigDB;

UPDATE `App` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `AppNamespace` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Audit` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Cluster` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Commit` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `GrayReleaseRule` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Item` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Namespace` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `NamespaceLock` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Release` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ReleaseHistory` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ServerConfig` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `AccessKey` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;

-- TODO: add UNIQUE CONSTRAINT INDEX for each table