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
INSERT INTO "App" (AppId, Name, OwnerName, OwnerEmail) VALUES ('someAppId','someAppName','someOwnerName','someOwnerName@ctrip.com');

INSERT INTO "Cluster" (AppId, Name) VALUES ('someAppId', 'default');

INSERT INTO "AppNamespace" (AppId, Name) VALUES ('someAppId', 'application');

INSERT INTO "AppNamespace" (AppId, Name) VALUES ('someAppId', 'someNamespace');

INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'default', 'application');

INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'default', 'someNamespace');
