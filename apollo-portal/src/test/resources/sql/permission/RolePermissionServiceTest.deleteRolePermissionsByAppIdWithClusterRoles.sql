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
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`,
                          `DataChange_LastModifiedBy`)
VALUES (1500, 'ModifyNamespacesInCluster', 'clusterApp+DEV+default', 'someOperator',
        'someOperator'),
       (1501, 'ReleaseNamespacesInCluster', 'clusterApp+DEV+default', 'someOperator',
        'someOperator');

INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)
VALUES (1500, 'ModifyNamespacesInCluster+clusterApp+DEV+default', 'someOperator', 'someOperator'),
       (1501, 'ReleaseNamespacesInCluster+clusterApp+DEV+default', 'someOperator', 'someOperator');

INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`)
VALUES (1500, 1500, 1500),
       (1501, 1501, 1501);