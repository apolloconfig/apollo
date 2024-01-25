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
ALTER TABLE "App" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "App" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "App" ALTER COLUMN OrgName VARCHAR(255) NULL;
ALTER TABLE "App" ALTER COLUMN OrgId VARCHAR(255) NULL;
ALTER TABLE "AppNamespace" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "AppNamespace" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "AppNamespace" ALTER COLUMN Format VARCHAR(255) NULL;
ALTER TABLE "Cluster" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Cluster" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "Cluster" ALTER COLUMN ParentClusterId BIGINT DEFAULT 0;
ALTER TABLE "Namespace" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Namespace" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "Item" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Item" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "Release" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Release" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "ServerConfig" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "ServerConfig" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
ALTER TABLE "ServerConfig" ALTER COLUMN Comment VARCHAR(255) NULL;
ALTER TABLE "Audit" ALTER COLUMN DataChange_CreatedBy VARCHAR(255) NULL;
ALTER TABLE "Audit" ALTER COLUMN DataChange_CreatedTime TIMESTAMP NULL;
CREATE ALIAS IF NOT EXISTS UNIX_TIMESTAMP FOR "com.ctrip.framework.apollo.common.jpa.H2Function.unixTimestamp";
