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
# delta schema to upgrade apollo portal db from v1.9.0 to v1.10.0

Use ApolloPortalDB;

UPDATE `App` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `AppNamespace` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Consumer` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ConsumerRole` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ConsumerToken` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Favorite` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Permission` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `Role` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `RolePermission` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `ServerConfig` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;
UPDATE `UserRole` SET `DeletedAt` = -Id WHERE `IsDeleted` = 1 and `DeletedAt` = 0;

-- TODO: add UNIQUE CONSTRAINT INDEX for each table