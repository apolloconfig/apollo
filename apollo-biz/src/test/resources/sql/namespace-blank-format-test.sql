--
-- Copyright 2025 Apollo Authors
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
-- Deliberately omits `Format` on the AppNamespace insert - some existing fixtures/rows do this,
-- and it resolves to a blank value rather than a recognizable format in the test DB.
INSERT INTO "App" ( `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('blankFormatTestApp', 'test', 'default', 'default', 'default', 'default', 0, 'default', 'default');

INSERT INTO "Cluster" (`Id`, `Name`, `AppId`, `ParentClusterId`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES (900002, 'default', 'blankFormatTestApp', 0, 0, 'default', 'default');

INSERT INTO "AppNamespace" (`Name`, `AppId`) VALUES ( 'blank.format', 'blankFormatTestApp');

INSERT INTO "Namespace" (`Id`, `AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES(900002,'blankFormatTestApp', 'default', 'blank.format', 0, 'apollo', 'apollo');
