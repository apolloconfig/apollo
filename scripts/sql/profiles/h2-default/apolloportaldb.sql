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
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `AppId` varchar(64) NOT NULL DEFAULT 'default' ,
  `Name` varchar(500) NOT NULL DEFAULT 'default' ,
  `OrgId` varchar(32) NOT NULL DEFAULT 'default' ,
  `OrgName` varchar(64) NOT NULL DEFAULT 'default' ,
  `OwnerName` varchar(500) NOT NULL DEFAULT 'default' ,
  `OwnerEmail` varchar(500) NOT NULL DEFAULT 'default' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `App_UK_AppId_DeletedAt` (`AppId`,`DeletedAt`),
  KEY `App_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `App_IX_Name` (`Name`)
)   ;



-- Dump of table appnamespace
-- ------------------------------------------------------------


CREATE TABLE `AppNamespace` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `Name` varchar(32) NOT NULL DEFAULT '' ,
  `AppId` varchar(64) NOT NULL DEFAULT '' ,
  `Format` varchar(32) NOT NULL DEFAULT 'properties' ,
  `IsPublic` boolean NOT NULL DEFAULT FALSE ,
  `Comment` varchar(64) NOT NULL DEFAULT '' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `AppNamespace_UK_AppId_Name_DeletedAt` (`AppId`,`Name`,`DeletedAt`),
  KEY `AppNamespace_Name_AppId` (`Name`,`AppId`),
  KEY `AppNamespace_DataChange_LastTime` (`DataChange_LastTime`)
)   ;



-- Dump of table consumer
-- ------------------------------------------------------------


CREATE TABLE `Consumer` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `AppId` varchar(64) NOT NULL DEFAULT 'default' ,
  `Name` varchar(500) NOT NULL DEFAULT 'default' ,
  `OrgId` varchar(32) NOT NULL DEFAULT 'default' ,
  `OrgName` varchar(64) NOT NULL DEFAULT 'default' ,
  `OwnerName` varchar(500) NOT NULL DEFAULT 'default' ,
  `OwnerEmail` varchar(500) NOT NULL DEFAULT 'default' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Consumer_UK_AppId_DeletedAt` (`AppId`,`DeletedAt`),
  KEY `Consumer_DataChange_LastTime` (`DataChange_LastTime`)
)   ;



-- Dump of table consumeraudit
-- ------------------------------------------------------------


CREATE TABLE `ConsumerAudit` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `ConsumerId` int(11) unsigned DEFAULT NULL ,
  `Uri` varchar(1024) NOT NULL DEFAULT '' ,
  `Method` varchar(16) NOT NULL DEFAULT '' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  KEY `ConsumerAudit_IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `ConsumerAudit_IX_ConsumerId` (`ConsumerId`)
)   ;



-- Dump of table consumerrole
-- ------------------------------------------------------------


CREATE TABLE `ConsumerRole` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `ConsumerId` int(11) unsigned DEFAULT NULL ,
  `RoleId` int(10) unsigned DEFAULT NULL ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `ConsumerRole_UK_ConsumerId_RoleId_DeletedAt` (`ConsumerId`,`RoleId`,`DeletedAt`),
  KEY `ConsumerRole_IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `ConsumerRole_IX_RoleId` (`RoleId`)
)   ;



-- Dump of table consumertoken
-- ------------------------------------------------------------


CREATE TABLE `ConsumerToken` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `ConsumerId` int(11) unsigned DEFAULT NULL ,
  `Token` varchar(128) NOT NULL DEFAULT '' ,
  `Expires` datetime NOT NULL DEFAULT '2099-01-01 00:00:00' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `ConsumerToken_UK_Token_DeletedAt` (`Token`,`DeletedAt`),
  KEY `ConsumerToken_DataChange_LastTime` (`DataChange_LastTime`)
)   ;

-- Dump of table favorite
-- ------------------------------------------------------------


CREATE TABLE `Favorite` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `UserId` varchar(32) NOT NULL DEFAULT 'default' ,
  `AppId` varchar(64) NOT NULL DEFAULT 'default' ,
  `Position` int(32) NOT NULL DEFAULT '10000' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Favorite_UK_UserId_AppId_DeletedAt` (`UserId`,`AppId`,`DeletedAt`),
  KEY `Favorite_AppId` (`AppId`),
  KEY `Favorite_DataChange_LastTime` (`DataChange_LastTime`)
)  AUTO_INCREMENT=23  ;

-- Dump of table permission
-- ------------------------------------------------------------


CREATE TABLE `Permission` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `PermissionType` varchar(32) NOT NULL DEFAULT '' ,
  `TargetId` varchar(256) NOT NULL DEFAULT '' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Permission_UK_TargetId_PermissionType_DeletedAt` (`TargetId`,`PermissionType`,`DeletedAt`),
  KEY `Permission_IX_DataChange_LastTime` (`DataChange_LastTime`)
)   ;



-- Dump of table role
-- ------------------------------------------------------------


CREATE TABLE `Role` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `RoleName` varchar(256) NOT NULL DEFAULT '' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Role_UK_RoleName_DeletedAt` (`RoleName`,`DeletedAt`),
  KEY `Role_IX_DataChange_LastTime` (`DataChange_LastTime`)
)   ;



-- Dump of table rolepermission
-- ------------------------------------------------------------


CREATE TABLE `RolePermission` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `RoleId` int(10) unsigned DEFAULT NULL ,
  `PermissionId` int(10) unsigned DEFAULT NULL ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `RolePermission_UK_RoleId_PermissionId_DeletedAt` (`RoleId`,`PermissionId`,`DeletedAt`),
  KEY `RolePermission_IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `RolePermission_IX_PermissionId` (`PermissionId`)
)   ;



-- Dump of table serverconfig
-- ------------------------------------------------------------


CREATE TABLE `ServerConfig` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `Key` varchar(64) NOT NULL DEFAULT 'default' ,
  `Value` varchar(2048) NOT NULL DEFAULT 'default' ,
  `Comment` varchar(1024) DEFAULT '' ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `ServerConfig_UK_Key_DeletedAt` (`Key`,`DeletedAt`),
  KEY `ServerConfig_DataChange_LastTime` (`DataChange_LastTime`)
)   ;



-- Dump of table userrole
-- ------------------------------------------------------------


CREATE TABLE `UserRole` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `UserId` varchar(128) DEFAULT '' ,
  `RoleId` int(10) unsigned DEFAULT NULL ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `UserRole_UK_UserId_RoleId_DeletedAt` (`UserId`,`RoleId`,`DeletedAt`),
  KEY `UserRole_IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `UserRole_IX_RoleId` (`RoleId`)
)   ;

-- Dump of table Users
-- ------------------------------------------------------------


CREATE TABLE `Users` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `Username` varchar(64) NOT NULL DEFAULT 'default' ,
  `Password` varchar(512) NOT NULL DEFAULT 'default' ,
  `UserDisplayName` varchar(512) NOT NULL DEFAULT 'default' ,
  `Email` varchar(64) NOT NULL DEFAULT 'default' ,
  `Enabled` tinyint(4) DEFAULT NULL ,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `Users_UK_Username` (`Username`)
)   ;


-- Dump of table Authorities
-- ------------------------------------------------------------


CREATE TABLE `Authorities` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT ,
  `Username` varchar(64) NOT NULL,
  `Authority` varchar(50) NOT NULL,
  PRIMARY KEY (`Id`)
)  ;

-- spring session (https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-mysql.sql)
-- Dump of table SPRING_SESSION
-- ------------------------------------------------------------


CREATE TABLE `SPRING_SESSION` (
  `PRIMARY_ID` char(36) NOT NULL,
  `SESSION_ID` char(36) NOT NULL,
  `CREATION_TIME` bigint NOT NULL,
  `LAST_ACCESS_TIME` bigint NOT NULL,
  `MAX_INACTIVE_INTERVAL` int NOT NULL,
  `EXPIRY_TIME` bigint NOT NULL,
  `PRINCIPAL_NAME` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`PRIMARY_ID`),
  UNIQUE KEY `SPRING_SESSION_SPRING_SESSION_IX1` (`SESSION_ID`),
  KEY `SPRING_SESSION_SPRING_SESSION_IX2` (`EXPIRY_TIME`),
  KEY `SPRING_SESSION_SPRING_SESSION_IX3` (`PRINCIPAL_NAME`)
)   ;

-- Dump of table SPRING_SESSION_ATTRIBUTES
-- ------------------------------------------------------------


CREATE TABLE `SPRING_SESSION_ATTRIBUTES` (
  `SESSION_PRIMARY_ID` char(36) NOT NULL,
  `ATTRIBUTE_NAME` varchar(200) NOT NULL,
  `ATTRIBUTE_BYTES` blob NOT NULL,
  PRIMARY KEY (`SESSION_PRIMARY_ID`,`ATTRIBUTE_NAME`),
  CONSTRAINT `SPRING_SESSION_ATTRIBUTES_FK` FOREIGN KEY (`SESSION_PRIMARY_ID`) REFERENCES `SPRING_SESSION` (`PRIMARY_ID`) ON DELETE CASCADE
)   ;

-- Dump of table AuditLog
-- ------------------------------------------------------------


CREATE TABLE `AuditLog` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `TraceId` varchar(32) NOT NULL DEFAULT '' ,
  `SpanId` varchar(32) NOT NULL DEFAULT '' ,
  `ParentSpanId` varchar(32) DEFAULT NULL ,
  `FollowsFromSpanId` varchar(32) DEFAULT NULL ,
  `Operator` varchar(64) NOT NULL DEFAULT 'anonymous' ,
  `OpType` varchar(50) NOT NULL DEFAULT 'default' ,
  `OpName` varchar(150) NOT NULL DEFAULT 'default' ,
  `Description` varchar(200) DEFAULT NULL ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) DEFAULT NULL ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  KEY `AuditLog_IX_TraceId` (`TraceId`),
  KEY `AuditLog_IX_OpName` (`OpName`),
  KEY `AuditLog_IX_DataChange_CreatedTime` (`DataChange_CreatedTime`),
  KEY `AuditLog_IX_Operator` (`Operator`)
)   ;

-- Dump of table AuditLogDataInfluence
-- ------------------------------------------------------------


CREATE TABLE `AuditLogDataInfluence` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT ,
  `SpanId` char(32) NOT NULL DEFAULT '' ,
  `InfluenceEntityId` varchar(50) NOT NULL DEFAULT '0' ,
  `InfluenceEntityName` varchar(50) NOT NULL DEFAULT 'default' ,
  `FieldName` varchar(50) DEFAULT NULL ,
  `FieldOldValue` varchar(500) DEFAULT NULL ,
  `FieldNewValue` varchar(500) DEFAULT NULL ,
  `IsDeleted` boolean NOT NULL DEFAULT FALSE ,
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' ,
  `DataChange_CreatedBy` varchar(64) DEFAULT NULL ,
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' ,
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ,
  PRIMARY KEY (`Id`),
  KEY `AuditLogDataInfluence_IX_SpanId` (`SpanId`),
  KEY `AuditLogDataInfluence_IX_DataChange_CreatedTime` (`DataChange_CreatedTime`),
  KEY `AuditLogDataInfluence_IX_EntityId` (`InfluenceEntityId`)
)   ;

-- Config
-- ------------------------------------------------------------
INSERT INTO `ServerConfig` (`Key`, `Value`, `Comment`)
VALUES
    ('apollo.portal.envs', 'dev', '可支持的环境列表'),
    ('organizations', '[{"orgId":"TEST1","orgName":"样例部门1"},{"orgId":"TEST2","orgName":"样例部门2"}]', '部门列表'),
    ('superAdmin', 'apollo', 'Portal超级管理员'),
    ('api.readTimeout', '10000', 'http接口read timeout'),
    ('consumer.token.salt', 'someSalt', 'consumer token salt'),
    ('admin.createPrivateNamespace.switch', 'true', '是否允许项目管理员创建私有namespace'),
    ('configView.memberOnly.envs', 'pro', '只对项目成员显示配置信息的环境列表，多个env以英文逗号分隔'),
    ('apollo.portal.meta.servers', '{}', '各环境Meta Service列表');


INSERT INTO `Users` (`Username`, `Password`, `UserDisplayName`, `Email`, `Enabled`)
VALUES
	('apollo', '$2a$10$7r20uS.BQ9uBpf3Baj3uQOZvMVvB1RN3PYoKE94gtz2.WAOuiiwXS', 'apollo', 'apollo@acme.com', 1);

INSERT INTO `Authorities` (`Username`, `Authority`) VALUES ('apollo', 'ROLE_user');

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