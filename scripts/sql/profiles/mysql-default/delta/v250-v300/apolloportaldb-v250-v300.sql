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

-- delta schema to upgrade apollo portal db from v2.5.0 to v3.0.0

CREATE TABLE IF NOT EXISTS `UserToken` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `UserId` varchar(64) NOT NULL DEFAULT '' COMMENT '用户ID',
  `Name` varchar(128) NOT NULL DEFAULT '' COMMENT 'token名称',
  `TokenPrefix` varchar(32) NOT NULL DEFAULT '' COMMENT 'token前缀',
  `TokenHash` varchar(128) NOT NULL DEFAULT '' COMMENT 'token哈希',
  `Scopes` varchar(4096) DEFAULT NULL COMMENT 'token权限范围JSON，字段: operations/appIds/envs/namespaces',
  `RateLimit` int NOT NULL DEFAULT '0' COMMENT '限流值',
  `Expires` datetime NOT NULL COMMENT 'token失效时间',
  `LastUsedTime` datetime DEFAULT NULL COMMENT '最后使用时间',
  `LastUsedIp` varchar(64) DEFAULT NULL COMMENT '最后使用IP',
  `LastUsedUserAgent` varchar(512) DEFAULT NULL COMMENT '最后使用UserAgent',
  `RevokedAt` datetime DEFAULT NULL COMMENT '撤销时间',
  `RevokedBy` varchar(64) DEFAULT NULL COMMENT '撤销人',
  `IsDeleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '1: deleted, 0: normal',
  `DeletedAt` BIGINT(20) NOT NULL DEFAULT '0' COMMENT 'Delete timestamp based on milliseconds',
  `DataChange_CreatedBy` varchar(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(64) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `UK_TokenPrefix_DeletedAt` (`TokenPrefix`,`DeletedAt`),
  KEY `IX_UserId` (`UserId`),
  KEY `IX_TokenHash` (`TokenHash`),
  KEY `IX_DataChange_LastTime` (`DataChange_LastTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户访问token表';

CREATE TABLE IF NOT EXISTS `UserTokenAudit` (
  `Id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增Id',
  `TokenId` int(11) unsigned DEFAULT NULL COMMENT 'UserToken Id',
  `UserId` varchar(64) NOT NULL DEFAULT '' COMMENT '用户ID',
  `Uri` varchar(1024) NOT NULL DEFAULT '' COMMENT '访问的Uri',
  `Method` varchar(16) NOT NULL DEFAULT '' COMMENT '访问的Method',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  KEY `IX_DataChange_LastTime` (`DataChange_LastTime`),
  KEY `IX_TokenId` (`TokenId`),
  KEY `IX_UserId` (`UserId`),
  CONSTRAINT `FK_UserTokenAudit_TokenId` FOREIGN KEY (`TokenId`) REFERENCES `UserToken` (`Id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户访问token审计表';
