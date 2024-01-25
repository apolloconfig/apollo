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
ALTER TABLE "Consumer" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Consumer" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "ConsumerToken" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "ConsumerToken" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "ConsumerRole" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "ConsumerRole" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "Role" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Role" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "UserRole" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "UserRole" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "Permission" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Permission" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "RolePermission" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "RolePermission" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "AppNamespace" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "AppNamespace" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "AppNamespace" ALTER COLUMN Format VARCHAR(255) NULL;
ALTER TABLE "App" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "ServerConfig" ALTER COLUMN Comment VARCHAR(255) NULL;
CREATE ALIAS IF NOT EXISTS UNIX_TIMESTAMP FOR "com.ctrip.framework.apollo.common.jpa.H2Function.unixTimestamp";
