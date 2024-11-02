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
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- 
-- ===============================================================================
-- ==                                                                           ==
-- ==                     Generated from 'scripts/sql/src/'                     ==
-- == by running 'mvn compile -pl apollo-build-sql-converter -Psql-converter'.  ==
-- ==                              DO NOT EDIT !!!                              ==
-- ==                                                                           ==
-- ===============================================================================
-- 

-- H2 Function
-- ------------------------------------------------------------
CREATE ALIAS IF NOT EXISTS UNIX_TIMESTAMP FOR "com.ctrip.framework.apollo.common.jpa.H2Function.unixTimestamp";

-- 

-- Dump of table app
-- ------------------------------------------------------------


CREATE TABLE `App` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `Name` varchar(500) NOT NULL DEFAULT 'default' COMMENT '应用名',
  `OrgId` varchar(32) NOT NULL DEFAULT 'default' COMMENT '部门Id',
  `OrgName` varchar(64) NOT NULL DEFAULT 'default' COMMENT '部门名字',
  `OwnerName` varchar(500) NOT NULL DEFAULT 'default' COMMENT 'ownerName',
  `OwnerEmail` varchar(500) NOT NULL DEFAULT 'default' COMMENT 'ownerEmail',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `App_UK_AppId_DeletedAt` (`AppId`,`DeletedAt`),
  KEY `App_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `App_IX_Name` (`Name`)
)   COMMENT='应用表';



-- Dump of table appnamespace
-- ------------------------------------------------------------


CREATE TABLE `AppNamespace` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `Name` varchar(32) NOT NULL DEFAULT '' COMMENT 'namespace名字，注意，需要全局唯一',
  `AppId` varchar(64) NOT NULL DEFAULT '' COMMENT 'app id',
  `Format` varchar(32) NOT NULL DEFAULT 'properties' COMMENT 'namespace的format类型',
  `IsPublic` boolean NOT NULL DEFAULT FALSE COMMENT 'namespace是否为公共',
  `Comment` varchar(64) NOT NULL DEFAULT '' COMMENT '注释',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `AppNamespace_UK_AppId_Name_DeletedAt` (`AppId`,`Name`,`DeletedAt`),
  KEY `AppNamespace_Name_AppId` (`Name`,`AppId`),
  KEY `AppNamespace_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='应用namespace定义';



-- Dump of table audit
-- ------------------------------------------------------------


CREATE TABLE `Audit` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `EntityName` varchar(50) NOT NULL DEFAULT 'default' COMMENT '表名',
  `EntityId` int(10) unsigned DEFAULT NULL COMMENT '记录ID',
  `OpName` varchar(50) NOT NULL DEFAULT 'default' COMMENT '操作类型',
  `Comment` varchar(500) DEFAULT NULL COMMENT '备注',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `Audit_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='日志审计表';



-- Dump of table cluster
-- ------------------------------------------------------------


CREATE TABLE `Cluster` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `Name` varchar(32) NOT NULL DEFAULT '' COMMENT '集群名字',
  `AppId` varchar(64) NOT NULL DEFAULT '' COMMENT 'App id',
  `ParentClusterId` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '父cluster',
  `Comment` varchar(64) DEFAULT NULL COMMENT '备注',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Cluster_UK_AppId_Name_DeletedAt` (`AppId`,`Name`,`DeletedAt`),
  KEY `Cluster_IX_ParentClusterId` (`ParentClusterId`),
  KEY `Cluster_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='集群';



-- Dump of table commit
-- ------------------------------------------------------------


CREATE TABLE `Commit` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ChangeSets` longtext NOT NULL COMMENT '修改变更集',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `ClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'ClusterName',
  `NamespaceName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'namespaceName',
  `Comment` varchar(500) DEFAULT NULL COMMENT '备注',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `Commit_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `Commit_AppId` (`AppId`),
  KEY `Commit_ClusterName` (`ClusterName`),
  KEY `Commit_NamespaceName` (`NamespaceName`)
)   COMMENT='commit 历史表';

-- Dump of table grayreleaserule
-- ------------------------------------------------------------


CREATE TABLE `GrayReleaseRule` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `ClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'Cluster Name',
  `NamespaceName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'Namespace Name',
  `BranchName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'branch name',
  `Rules` varchar(16000) DEFAULT '[]' COMMENT '灰度规则',
  `ReleaseId` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '灰度对应的release',
  `BranchStatus` tinyint(2) DEFAULT '1' COMMENT '灰度分支状态: 0:删除分支,1:正在使用的规则 2：全量发布',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `GrayReleaseRule_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `GrayReleaseRule_IX_Namespace` (`AppId`,`ClusterName`,`NamespaceName`)
)   COMMENT='灰度规则表';


-- Dump of table instance
-- ------------------------------------------------------------


CREATE TABLE `Instance` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `ClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'ClusterName',
  `DataCenter` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'Data Center Name',
  `Ip` varchar(32) NOT NULL DEFAULT '' COMMENT 'instance ip',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Instance_IX_UNIQUE_KEY` (`AppId`,`ClusterName`,`Ip`,`DataCenter`),
  KEY `Instance_IX_IP` (`Ip`),
  KEY `Instance_IX_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='使用配置的应用实例';



-- Dump of table instanceconfig
-- ------------------------------------------------------------


CREATE TABLE `InstanceConfig` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `InstanceId` int(11) unsigned DEFAULT NULL COMMENT 'Instance Id',
  `ConfigAppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'Config App Id',
  `ConfigClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'Config Cluster Name',
  `ConfigNamespaceName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'Config Namespace Name',
  `ReleaseKey` varchar(64) NOT NULL DEFAULT '' COMMENT '发布的Key',
  `ReleaseDeliveryTime` timestamp NULL DEFAULT NULL COMMENT '配置获取时间',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `InstanceConfig_IX_UNIQUE_KEY` (`InstanceId`,`ConfigAppId`,`ConfigNamespaceName`),
  KEY `InstanceConfig_IX_ReleaseKey` (`ReleaseKey`),
  KEY `InstanceConfig_IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `InstanceConfig_IX_Valid_Namespace` (`ConfigAppId`,`ConfigClusterName`,`ConfigNamespaceName`,`DataChange_LastTime`)
)   COMMENT='应用实例的配置信息';



-- Dump of table item
-- ------------------------------------------------------------


CREATE TABLE `Item` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `NamespaceId` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '集群NamespaceId',
  `Key` varchar(128) NOT NULL DEFAULT 'default' COMMENT '配置项Key',
  `Type` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '配置项类型，0: String，1: Number，2: Boolean，3: JSON',
  `Value` longtext NOT NULL COMMENT '配置项值',
  `Comment` varchar(1024) DEFAULT '' COMMENT '注释',
  `LineNum` int(10) unsigned DEFAULT '0' COMMENT '行号',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `Item_IX_GroupId` (`NamespaceId`),
  KEY `Item_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='配置项目';



-- Dump of table namespace
-- ------------------------------------------------------------


CREATE TABLE `Namespace` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `ClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'Cluster Name',
  `NamespaceName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'Namespace Name',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Namespace_UK_AppId_ClusterName_NamespaceName_DeletedAt` (`AppId`,`ClusterName`,`NamespaceName`,`DeletedAt`),
  KEY `Namespace_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `Namespace_IX_NamespaceName` (`NamespaceName`)
)   COMMENT='命名空间';



-- Dump of table namespacelock
-- ------------------------------------------------------------


CREATE TABLE `NamespaceLock` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `NamespaceId` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '集群NamespaceId',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `IsDeleted` boolean DEFAULT FALSE COMMENT '软删除',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `NamespaceLock_UK_NamespaceId_DeletedAt` (`NamespaceId`,`DeletedAt`),
  KEY `NamespaceLock_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='namespace的编辑锁';



-- Dump of table release
-- ------------------------------------------------------------


CREATE TABLE `Release` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `ReleaseKey` varchar(64) NOT NULL DEFAULT '' COMMENT '发布的Key',
  `Name` varchar(64) NOT NULL DEFAULT 'default' COMMENT '发布名字',
  `Comment` varchar(256) DEFAULT NULL COMMENT '发布说明',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `ClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'ClusterName',
  `NamespaceName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'namespaceName',
  `Configurations` longtext NOT NULL COMMENT '发布配置',
  `IsAbandoned` boolean NOT NULL DEFAULT FALSE COMMENT '是否废弃',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Release_UK_ReleaseKey_DeletedAt` (`ReleaseKey`,`DeletedAt`),
  KEY `Release_AppId_ClusterName_GroupName` (`AppId`,`ClusterName`,`NamespaceName`),
  KEY `Release_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='发布';


-- Dump of table releasehistory
-- ------------------------------------------------------------


CREATE TABLE `ReleaseHistory` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `ClusterName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'ClusterName',
  `NamespaceName` varchar(32) NOT NULL DEFAULT 'default' COMMENT 'namespaceName',
  `BranchName` varchar(32) NOT NULL DEFAULT 'default' COMMENT '发布分支名',
  `ReleaseId` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '关联的Release Id',
  `PreviousReleaseId` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '前一次发布的ReleaseId',
  `Operation` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '发布类型，0: 普通发布，1: 回滚，2: 灰度发布，3: 灰度规则更新，4: 灰度合并回主分支发布，5: 主分支发布灰度自动发布，6: 主分支回滚灰度自动发布，7: 放弃灰度',
  `OperationContext` longtext NOT NULL COMMENT '发布上下文信息',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `ReleaseHistory_IX_Namespace` (`AppId`,`ClusterName`,`NamespaceName`,`BranchName`),
  KEY `ReleaseHistory_IX_ReleaseId` (`ReleaseId`),
  KEY `ReleaseHistory_IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `ReleaseHistory_IX_PreviousReleaseId` (`PreviousReleaseId`)
)   COMMENT='发布历史';


-- Dump of table releasemessage
-- ------------------------------------------------------------


CREATE TABLE `ReleaseMessage` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `Message` varchar(1024) NOT NULL DEFAULT '' COMMENT '发布的消息内容',
  `DataChange_LastTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `ReleaseMessage_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `ReleaseMessage_IX_Message` (`Message`)
)   COMMENT='发布消息';



-- Dump of table serverconfig
-- ------------------------------------------------------------


CREATE TABLE `ServerConfig` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `Key` varchar(64) NOT NULL DEFAULT 'default' COMMENT '配置项Key',
  `Cluster` varchar(32) NOT NULL DEFAULT 'default' COMMENT '配置对应的集群，default为不针对特定的集群',
  `Value` varchar(2048) NOT NULL DEFAULT 'default' COMMENT '配置项值',
  `Comment` varchar(1024) DEFAULT '' COMMENT '注释',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `ServerConfig_UK_Key_Cluster_DeletedAt` (`Key`,`Cluster`,`DeletedAt`),
  KEY `ServerConfig_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='配置服务自身配置';

-- Dump of table accesskey
-- ------------------------------------------------------------


CREATE TABLE `AccessKey` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `AppId` varchar(64) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `Secret` varchar(128) NOT NULL DEFAULT '' COMMENT 'Secret',
  `Mode` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '密钥模式，0: filter，1: observer',
  `IsEnabled` boolean NOT NULL DEFAULT FALSE COMMENT '1: enabled, 0: disabled',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `AccessKey_UK_AppId_Secret_DeletedAt` (`AppId`,`Secret`,`DeletedAt`),
  KEY `AccessKey_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='访问密钥';


-- Dump of table serviceregistry
-- ------------------------------------------------------------


CREATE TABLE `ServiceRegistry` (
  `Id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `ServiceName` VARCHAR(64) NOT NULL COMMENT '服务名',
  `Uri` VARCHAR(64) NOT NULL COMMENT '服务地址',
  `Cluster` VARCHAR(64) NOT NULL COMMENT '集群，可以用来标识apollo.cluster或者网络分区',
  `Metadata` VARCHAR(1024) NOT NULL DEFAULT '{}' COMMENT '元数据，key value结构的json object，为了方面后面扩展功能而不需要修改表结构',
  `DataChange_CreatedTime` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastTime` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE INDEX `IX_UNIQUE_KEY` (`ServiceName`, `Uri`),
  INDEX `IX_DataChange_LastTime` (`DataChange_LastTime`)
)   COMMENT='注册中心';

-- Dump of table AuditLog
-- ------------------------------------------------------------


CREATE TABLE `AuditLog` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `TraceId` varchar(32) NOT NULL DEFAULT '' COMMENT '链路全局唯一ID',
  `SpanId` varchar(32) NOT NULL DEFAULT '' COMMENT '跨度ID',
  `ParentSpanId` varchar(32) DEFAULT NULL COMMENT '父跨度ID',
  `FollowsFromSpanId` varchar(32) DEFAULT NULL COMMENT '上一个兄弟跨度ID',
  `Operator` varchar(64) NOT NULL DEFAULT 'anonymous' COMMENT '操作人',
  `OpType` varchar(50) NOT NULL DEFAULT 'default' COMMENT '操作类型',
  `OpName` varchar(150) NOT NULL DEFAULT 'default' COMMENT '操作名称',
  `Description` varchar(200) DEFAULT NULL COMMENT '备注',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) DEFAULT NULL COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `AuditLog_IX_TraceId` (`TraceId`),
  KEY `AuditLog_IX_OpName` (`OpName`),
  KEY `AuditLog_IX_DataChange_CreatedTime` (`DataChange_CreatedTime`),
  KEY `AuditLog_IX_Operator` (`Operator`)
)   COMMENT='审计日志表';

-- Dump of table AuditLogDataInfluence
-- ------------------------------------------------------------


CREATE TABLE `AuditLogDataInfluence` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `SpanId` char(32) NOT NULL DEFAULT '' COMMENT '跨度ID',
  `InfluenceEntityId` varchar(50) NOT NULL DEFAULT '0' COMMENT '记录ID',
  `InfluenceEntityName` varchar(50) NOT NULL DEFAULT 'default' COMMENT '表名',
  `FieldName` varchar(50) DEFAULT NULL COMMENT '字段名称',
  `FieldOldValue` varchar(500) DEFAULT NULL COMMENT '字段旧值',
  `FieldNewValue` varchar(500) DEFAULT NULL COMMENT '字段新值',
  `IsDeleted` boolean NOT NULL DEFAULT FALSE COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) DEFAULT NULL COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `AuditLogDataInfluence_IX_SpanId` (`SpanId`),
  KEY `AuditLogDataInfluence_IX_DataChange_CreatedTime` (`DataChange_CreatedTime`),
  KEY `AuditLogDataInfluence_IX_EntityId` (`InfluenceEntityId`)
)   COMMENT='审计日志数据变动表';

-- Config
-- ------------------------------------------------------------
INSERT INTO `ServerConfig` (`Key`, `Cluster`, `Value`, `Comment`)
VALUES
    ('eureka.service.url', 'default', 'http://localhost:8080/eureka/', 'Eureka服务Url，多个service以英文逗号分隔'),
    ('namespace.lock.switch', 'default', 'false', '一次发布只能有一个人修改开关'),
    ('item.key.length.limit', 'default', '128', 'item key 最大长度限制'),
    ('item.value.length.limit', 'default', '20000', 'item value最大长度限制'),
    ('config-service.cache.enabled', 'default', 'false', 'ConfigService是否开启缓存，开启后能提高性能，但是会增大内存消耗！');

-- 
-- ===============================================================================
-- ==                                                                           ==
-- ==                     Generated from 'scripts/sql/src/'                     ==
-- == by running 'mvn compile -pl apollo-build-sql-converter -Psql-converter'.  ==
-- ==                              DO NOT EDIT !!!                              ==
-- ==                                                                           ==
-- ===============================================================================

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
