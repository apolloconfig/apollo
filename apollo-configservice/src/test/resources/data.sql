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
INSERT INTO "App" (AppId, Name, OwnerName, OwnerEmail) VALUES ('100003171','apollo-config-service','刘一鸣','liuym@ctrip.com');
INSERT INTO "App" (AppId, Name, OwnerName, OwnerEmail) VALUES ('100003172','apollo-admin-service','宋顺','song_s@ctrip.com');
INSERT INTO "App" (AppId, Name, OwnerName, OwnerEmail) VALUES ('100003173','apollo-portal','张乐','zhanglea@ctrip.com');
INSERT INTO "App" (AppId, Name, OwnerName, OwnerEmail) VALUES ('fxhermesproducer','fx-hermes-producer','梁锦华','jhliang@ctrip.com');

INSERT INTO "Cluster" (AppId, Name) VALUES ('100003171', 'default');
INSERT INTO "Cluster" (AppId, Name) VALUES ('100003171', 'cluster1');
INSERT INTO "Cluster" (AppId, Name) VALUES ('100003172', 'default');
INSERT INTO "Cluster" (AppId, Name) VALUES ('100003172', 'cluster2');
INSERT INTO "Cluster" (AppId, Name) VALUES ('100003173', 'default');
INSERT INTO "Cluster" (AppId, Name) VALUES ('100003173', 'cluster3');
INSERT INTO "Cluster" (AppId, Name) VALUES ('fxhermesproducer', 'default');

INSERT INTO "AppNamespace" (AppId, Name) VALUES ('100003171', 'application');
INSERT INTO "AppNamespace" (AppId, Name) VALUES ('100003171', 'fx.apollo.config');
INSERT INTO "AppNamespace" (AppId, Name) VALUES ('100003172', 'application');
INSERT INTO "AppNamespace" (AppId, Name) VALUES ('100003172', 'fx.apollo.admin');
INSERT INTO "AppNamespace" (AppId, Name) VALUES ('100003173', 'application');
INSERT INTO "AppNamespace" (AppId, Name) VALUES ('100003173', 'fx.apollo.portal');
INSERT INTO "AppNamespace" (AppId, Name) VALUES ('fxhermesproducer', 'fx.hermes.producer');

INSERT INTO "Namespace" (Id, AppId, ClusterName, NamespaceName) VALUES (1, '100003171', 'default', 'application');
INSERT INTO "Namespace" (Id, AppId, ClusterName, NamespaceName) VALUES (2, 'fxhermesproducer', 'default', 'fx.hermes.producer');
INSERT INTO "Namespace" (Id, AppId, ClusterName, NamespaceName) VALUES (3, '100003172', 'default', 'application');
INSERT INTO "Namespace" (Id, AppId, ClusterName, NamespaceName) VALUES (4, '100003173', 'default', 'application');
INSERT INTO "Namespace" (Id, AppId, ClusterName, NamespaceName) VALUES (5, '100003171', 'default', 'application');

INSERT INTO "Item" (NamespaceId, "Key", "Value", Comment) VALUES (1, 'k1', 'v1', 'comment1');
INSERT INTO "Item" (NamespaceId, "Key", "Value", Comment) VALUES (1, 'k2', 'v2', 'comment2');
INSERT INTO "Item" (NamespaceId, "Key", "Value", Comment) VALUES (2, 'k3', 'v3', 'comment3');
INSERT INTO "Item" (NamespaceId, "Key", "Value", Comment) VALUES (5, 'k3', 'v4', 'comment4');

INSERT INTO "Release" (ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations) VALUES ('TEST-RELEASE-KEY', 'REV1','First Release','100003171', 'default', 'application', '{"k1":"v1"}');
