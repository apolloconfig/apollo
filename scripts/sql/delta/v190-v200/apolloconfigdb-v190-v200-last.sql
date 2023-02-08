--
-- Copyright 2022 Apollo Authors
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
# delta schema to upgrade apollo config db from v1.9.0 to v2.0.0

Use ApolloConfigDB;

ALTER TABLE `Namespace`
    ADD UNIQUE INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt` (`AppId`,`ClusterName`(191),`NamespaceName`(191),`DeletedAt`),
    DROP INDEX `UK_AppId_ClusterName_NamespaceName_DeletedAt`;