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
INSERT INTO "App" ( `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('test', 'test0620-06', 'default', 'default', 'default', 'default', 0, 'default', 'default');
INSERT INTO "Cluster" (`Id`, `Name`, `AppId`, `ParentClusterId`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES (1, 'default', 'test', 0, 0, 'default', 'default');
INSERT INTO "Cluster" (`Name`, `AppId`, `ParentClusterId`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('child-cluster', 'test', 1, 0, 'default', 'default');

INSERT INTO "Namespace" (`AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('test', 'default', 'application', 0, 'apollo', 'apollo');
INSERT INTO "Namespace" (`AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('test', 'child-cluster', 'application', 0, 'apollo', 'apollo');


