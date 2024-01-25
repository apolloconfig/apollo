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
INSERT INTO "App" (AppId, Name, OwnerName, OwnerEmail) VALUES ('somePublicAppId','somePublicAppName','someOwnerName','someOwnerName@ctrip.com');

INSERT INTO "Cluster" (AppId, Name) VALUES ('someAppId', 'default');
INSERT INTO "Cluster" (AppId, Name) VALUES ('someAppId', 'someCluster');
INSERT INTO "Cluster" (AppId, Name) VALUES ('somePublicAppId', 'default');
INSERT INTO "Cluster" (AppId, Name) VALUES ('somePublicAppId', 'someDC');

INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('someAppId', 'application', false);
INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('someAppId', 'someNamespace', true);
INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('someAppId', 'someNamespace.xml', false);
INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('someAppId', 'anotherNamespace', false);
INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('somePublicAppId', 'application', false);
INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('somePublicAppId', 'somePublicNamespace', true);
INSERT INTO "AppNamespace" (AppId, Name, IsPublic) VALUES ('somePublicAppId', 'anotherNamespace', true);

INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'default', 'application');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'default', 'someNamespace.xml');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'default', 'anotherNamespace');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'someCluster', 'someNamespace');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('somePublicAppId', 'default', 'application');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('somePublicAppId', 'someDC', 'somePublicNamespace');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('someAppId', 'default', 'somePublicNamespace');
INSERT INTO "Namespace" (AppId, ClusterName, NamespaceName) VALUES ('somePublicAppId', 'default', 'anotherNamespace');

INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
  VALUES (990, 'TEST-RELEASE-KEY1', 'INTEGRATION-TEST-DEFAULT','First Release','someAppId', 'default', 'application', '{"k1":"v1"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
  VALUES (991, 'TEST-RELEASE-KEY2', 'INTEGRATION-TEST-NAMESPACE','First Release','someAppId', 'someCluster', 'someNamespace', '{"k2":"v2"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
  VALUES (992, 'TEST-RELEASE-KEY3', 'INTEGRATION-TEST-PUBLIC-DEFAULT','First Release','somePublicAppId', 'default', 'somePublicNamespace', '{"k1":"default-v1", "k2":"default-v2"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
 VALUES (993, 'TEST-RELEASE-KEY4', 'INTEGRATION-TEST-PUBLIC-NAMESPACE','First Release','somePublicAppId', 'someDC', 'somePublicNamespace', '{"k1":"someDC-v1", "k2":"someDC-v2"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
 VALUES (989, 'TEST-RELEASE-KEY5', 'INTEGRATION-TEST-PRIVATE-CONFIG-FILE','First Release','someAppId', 'default', 'someNamespace.xml', '{"k1":"v1-file", "k2":"v2-file"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
 VALUES (988, 'TEST-RELEASE-KEY6', 'INTEGRATION-TEST-PRIVATE-CONFIG-FILE','First Release','someAppId', 'default', 'anotherNamespace', '{"k1":"v1-file"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
 VALUES (987, 'TEST-RELEASE-KEY7', 'INTEGRATION-TEST-PUBLIC-CONFIG-FILE','First Release','somePublicAppId', 'default', 'anotherNamespace', '{"k2":"v2-file"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
  VALUES (986, 'TEST-GRAY-RELEASE-KEY1', 'INTEGRATION-TEST-DEFAULT','Gray Release','someAppId', 'gray-branch-1', 'application', '{"k1":"v1-gray"}');
INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
  VALUES (985, 'TEST-GRAY-RELEASE-KEY2', 'INTEGRATION-TEST-NAMESPACE','Gray Release','somePublicAppId', 'gray-branch-2', 'somePublicNamespace', '{"k1":"gray-v1", "k2":"gray-v2"}');