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

INSERT INTO "Namespace" (`Id`, `AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES(1,'testApp', 'default', 'application', 0, 'apollo', 'apollo');

INSERT INTO "Item" (`Id`, `NamespaceId`, "Key", "Type", "Value", `Comment`, `LineNum`)
    VALUES
        (9901, 1, 'k1', 0, 'v1', '', 1),
        (9902, 1, 'k2', 2, 'v2', '', 2),
        (9903, 1, 'k3', 0, 'v3', '', 3),
        (9904, 1, 'k4', 0, 'v4', '', 4),
        (9905, 1, 'k5', 0, 'v5', '', 5);
