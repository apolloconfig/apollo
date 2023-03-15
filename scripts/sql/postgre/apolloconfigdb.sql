--
-- Copyright 2023 Apollo Authors
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
\c "ApolloConfigDB"
/* # Dump of table app */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."App";

CREATE TABLE apolloconfig."App"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '主键'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "Name"                      varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT '应用名'
    "OrgId"                     varchar(32)  NOT NULL DEFAULT 'default',         -- COMMENT '部门Id'
    "OrgName"                   varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '部门名字'
    "OwnerName"                 varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ownerName'
    "OwnerEmail"                varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ownerEmail'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "App_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "App_UK_AppId_DeletedAt" UNIQUE ("AppId", "DeletedAt")
);

CREATE INDEX "App_IX_Name" ON apolloconfig."App" ("Name");
CREATE INDEX "App_DataChange_LastTime" ON apolloconfig."App" ("DataChange_LastTime");

/* # Dump of table appnamespace */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."AppNamespace";

CREATE TABLE apolloconfig."AppNamespace"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增主键'
    "Name"                      varchar(32) NOT NULL DEFAULT '',                -- COMMENT 'namespace名字，注意，需要全局唯一'
    "AppId"                     varchar(64) NOT NULL DEFAULT '',                -- COMMENT 'app id'
    "Format"                    varchar(32) NOT NULL DEFAULT 'properties',      -- COMMENT 'namespace的format类型'
    "IsPublic"                  BOOL        NOT NULL DEFAULT false,             -- COMMENT 'namespace是否为公共'
    "Comment"                   varchar(64) NOT NULL DEFAULT '',                -- COMMENT '注释'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "AppNamespace_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "AppNamespace_UK_AppId_Name_DeletedAt" UNIQUE ("AppId", "Name", "DeletedAt")
);

CREATE INDEX "AppNamespace_Name_AppId" ON apolloconfig."AppNamespace" ("Name", "AppId");
CREATE INDEX "AppNamespace_DataChange_LastTime" ON apolloconfig."AppNamespace" ("DataChange_LastTime");

/* # Dump of table audit */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Audit";

CREATE TABLE apolloconfig."Audit"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '主键'
    "EntityName"                varchar(50) NOT NULL DEFAULT 'default',         -- COMMENT '表名'
    "EntityId"                  int                  DEFAULT NULL,              -- COMMENT '记录ID'
    "OpName"                    varchar(50) NOT NULL DEFAULT 'default',         -- COMMENT '操作类型'
    "Comment"                   varchar(500)         DEFAULT NULL,              -- COMMENT '备注'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "Audit_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "Audit_DataChange_LastTime" ON apolloconfig."Audit" ("DataChange_LastTime");

/* # Dump of table cluster */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Cluster";

CREATE TABLE apolloconfig."Cluster"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增主键'
    "Name"                      varchar(32) NOT NULL DEFAULT '',                -- COMMENT '集群名字'
    "AppId"                     varchar(64) NOT NULL DEFAULT '',                -- COMMENT 'App id'
    "ParentClusterId"           int         NOT NULL DEFAULT '0',               -- COMMENT '父cluster'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "Cluster_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Cluster_UK_AppId_Name_DeletedAt" UNIQUE ("AppId", "Name", "DeletedAt")
);

CREATE INDEX "Cluster_IX_ParentClusterId" ON apolloconfig."Cluster" ("ParentClusterId");
CREATE INDEX "Cluster_DataChange_LastTime" ON apolloconfig."Cluster" ("DataChange_LastTime");

/* # Dump of table commit */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Commit";

CREATE TABLE apolloconfig."Commit"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '主键'
    "ChangeSets"                TEXT         NOT NULL,                           -- COMMENT '修改变更集'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "ClusterName"               varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ClusterName'
    "NamespaceName"             varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'namespaceName'
    "Comment"                   varchar(500)          DEFAULT NULL,              -- COMMENT '备注'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Commit_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "Commit_DataChange_LastTime" ON apolloconfig."Commit" ("DataChange_LastTime");
CREATE INDEX "Commit_AppId" ON apolloconfig."Commit" ("AppId");
CREATE INDEX "Commit_ClusterName" ON apolloconfig."Commit" ("ClusterName");
CREATE INDEX "Commit_NamespaceName" ON apolloconfig."Commit" ("NamespaceName");

/* # Dump of table grayreleaserule */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."GrayReleaseRule";

CREATE TABLE apolloconfig."GrayReleaseRule"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '主键'
    "AppId"                     varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "ClusterName"               varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'Cluster Name'
    "NamespaceName"             varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'Namespace Name'
    "BranchName"                varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'branch name'
    "Rules"                     varchar(16000)       DEFAULT '[]',              -- COMMENT '灰度规则'
    "ReleaseId"                 int         NOT NULL DEFAULT '0',               -- COMMENT '灰度对应的release'
    "BranchStatus"              SMALLINT             DEFAULT '1',               -- COMMENT '灰度分支状态: 0:删除分支,1:正在使用的规则 2：全量发布'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "GrayReleaseRule_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "GrayReleaseRule_DataChange_LastTime" ON apolloconfig."GrayReleaseRule" ("DataChange_LastTime");
CREATE INDEX "GrayReleaseRule_IX_Namespace" ON apolloconfig."GrayReleaseRule" ("AppId", "ClusterName", "NamespaceName");

/* # Dump of table instance */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Instance";

CREATE TABLE apolloconfig."Instance"
(
    "Id"                     BIGSERIAL,                                      -- COMMENT '自增Id'
    "AppId"                  varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "ClusterName"            varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'ClusterName'
    "DataCenter"             varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT 'Data Center Name'
    "Ip"                     varchar(32) NOT NULL DEFAULT '',                -- COMMENT 'instance ip'
    "DataChange_CreatedTime" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '最后修改时间'
    CONSTRAINT "Instance_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Instance_IX_UNIQUE_KEY" UNIQUE ("AppId", "ClusterName", "Ip", "DataCenter")
);

CREATE INDEX "Instance_IX_DataChange_LastTime" ON apolloconfig."Instance" ("DataChange_LastTime");
CREATE INDEX "Instance_IX_IP" ON apolloconfig."Instance" ("Ip");

/* # Dump of table instanceconfig */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."InstanceConfig";

CREATE TABLE apolloconfig."InstanceConfig"
(
    "Id"                     BIGSERIAL,                                      -- COMMENT '自增Id'
    "InstanceId"             int                  DEFAULT NULL,              -- COMMENT 'Instance Id'
    "ConfigAppId"            varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT 'Config App Id'
    "ConfigClusterName"      varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'Config Cluster Name'
    "ConfigNamespaceName"    varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'Config Namespace Name'
    "ReleaseKey"             varchar(64) NOT NULL DEFAULT '',                -- COMMENT '发布的Key'
    "ReleaseDeliveryTime"    timestamp NULL DEFAULT NULL,                    -- COMMENT '配置获取时间'
    "DataChange_CreatedTime" timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '最后修改时间'
    CONSTRAINT "InstanceConfig_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "InstanceConfig_IX_UNIQUE_KEY" UNIQUE ("InstanceId", "ConfigAppId", "ConfigNamespaceName")
);

CREATE INDEX "InstanceConfig_IX_ReleaseKey" ON apolloconfig."InstanceConfig" ("ReleaseKey");
CREATE INDEX "InstanceConfig_IX_DataChange_LastTime" ON apolloconfig."InstanceConfig" ("DataChange_LastTime");
CREATE INDEX "InstanceConfig_IX_Valid_Namespace" ON apolloconfig."InstanceConfig" ("ConfigAppId", "ConfigClusterName",
                                                                                   "ConfigNamespaceName",
                                                                                   "DataChange_LastTime");

/* # Dump of table item */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Item";

CREATE TABLE apolloconfig."Item"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增Id'
    "NamespaceId"               int          NOT NULL DEFAULT '0',               -- COMMENT '集群NamespaceId'
    "Key"                       varchar(128) NOT NULL DEFAULT 'default',         -- COMMENT '配置项Key'
    "Type"                      SMALLINT     NOT NULL DEFAULT '0',               -- COMMENT '配置项类型，0: String，1: Number，2: Boolean，3: JSON'
    "Value"                     TEXT         NOT NULL,                           -- COMMENT '配置项值'
    "Comment"                   varchar(1024)         DEFAULT '',                -- COMMENT '注释'
    "LineNum"                   int                   DEFAULT '0',               -- COMMENT '行号'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Item_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "Item_IX_GroupId" ON apolloconfig."Item" ("NamespaceId");
CREATE INDEX "Item_DataChange_LastTime" ON apolloconfig."Item" ("DataChange_LastTime");

/* # Dump of table namespace */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Namespace";

CREATE TABLE apolloconfig."Namespace"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增主键'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "ClusterName"               varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'Cluster Name'
    "NamespaceName"             varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'Namespace Name'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Namespace_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Namespace_UK_AppId_ClusterName_NamespaceName_DeletedAt" UNIQUE ("AppId", "ClusterName", "NamespaceName", "DeletedAt")
);

CREATE INDEX "Namespace_IX_NamespaceName" ON apolloconfig."Namespace" ("NamespaceName");
CREATE INDEX "Namespace_DataChange_LastTime" ON apolloconfig."Namespace" ("DataChange_LastTime");

/* # Dump of table namespacelock */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."NamespaceLock";

CREATE TABLE apolloconfig."NamespaceLock"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增id'
    "NamespaceId"               int         NOT NULL DEFAULT '0',               -- COMMENT '集群NamespaceId'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    "IsDeleted"                 BOOL                 DEFAULT false,             -- COMMENT '软删除'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    CONSTRAINT "NamespaceLock_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "NamespaceLock_UK_NamespaceId_DeletedAt" UNIQUE ("NamespaceId", "DeletedAt")
);

CREATE INDEX "NamespaceLock_DataChange_LastTime" ON apolloconfig."NamespaceLock" ("DataChange_LastTime");

/* # Dump of table release */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."Release";

CREATE TABLE apolloconfig."Release"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增主键'
    "ReleaseKey"                varchar(64)  NOT NULL DEFAULT '',                -- COMMENT '发布的Key'
    "Name"                      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '发布名字'
    "Comment"                   varchar(256)          DEFAULT NULL,              -- COMMENT '发布说明'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "ClusterName"               varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ClusterName'
    "NamespaceName"             varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'namespaceName'
    "Configurations"            TEXT         NOT NULL,                           -- COMMENT '发布配置'
    "IsAbandoned"               BOOL         NOT NULL DEFAULT false,             -- COMMENT '是否废弃'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Release_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Release_UK_ReleaseKey_DeletedAt" UNIQUE ("ReleaseKey", "DeletedAt")
);

CREATE INDEX "Release_AppId_ClusterName_GroupName" ON apolloconfig."Release" ("AppId", "ClusterName", "NamespaceName");
CREATE INDEX "Release_DataChange_LastTime" ON apolloconfig."Release" ("DataChange_LastTime");



/* # Dump of table releasehistory */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."ReleaseHistory";

CREATE TABLE apolloconfig."ReleaseHistory"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增Id'
    "AppId"                     varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "ClusterName"               varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'ClusterName'
    "NamespaceName"             varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT 'namespaceName'
    "BranchName"                varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT '发布分支名'
    "ReleaseId"                 int         NOT NULL DEFAULT '0',               -- COMMENT '关联的Release Id'
    "PreviousReleaseId"         int         NOT NULL DEFAULT '0',               -- COMMENT '前一次发布的ReleaseId'
    "Operation"                 SMALLINT    NOT NULL DEFAULT '0',               -- COMMENT '发布类型，0: 普通发布，1: 回滚，2: 灰度发布，3: 灰度规则更新，4: 灰度合并回主分支发布，5: 主分支发布灰度自动发布，6: 主分支回滚灰度自动发布，7: 放弃灰度'
    "OperationContext"          TEXT        NOT NULL,                           -- COMMENT '发布上下文信息'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "ReleaseHistory_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "ReleaseHistory_IX_Namespace" ON apolloconfig."ReleaseHistory" ("AppId", "ClusterName", "NamespaceName", "BranchName");
CREATE INDEX "ReleaseHistory_IX_ReleaseId" ON apolloconfig."ReleaseHistory" ("ReleaseId");
CREATE INDEX "ReleaseHistory_IX_PreviousReleaseId" ON apolloconfig."ReleaseHistory" ("PreviousReleaseId");
CREATE INDEX "ReleaseHistory_IX_DataChange_LastTime" ON apolloconfig."ReleaseHistory" ("DataChange_LastTime");

/* # Dump of table releasemessage */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."ReleaseMessage";

CREATE TABLE apolloconfig."ReleaseMessage"
(
    "Id"                  BIGSERIAL,                                        -- COMMENT '自增主键'
    "Message"             varchar(1024) NOT NULL DEFAULT '',                -- COMMENT '发布的消息内容'
    "DataChange_LastTime" timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '最后修改时间'
    CONSTRAINT "ReleaseMessage_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "ReleaseMessage_IX_Message" ON apolloconfig."ReleaseMessage" ("Message");
CREATE INDEX "ReleaseMessage_DataChange_LastTime" ON apolloconfig."ReleaseMessage" ("DataChange_LastTime");

/* # Dump of table serverconfig */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."ServerConfig";

CREATE TABLE apolloconfig."ServerConfig"
(
    "Id"                        BIGSERIAL,                                        -- COMMENT '自增Id'
    "Key"                       varchar(64)   NOT NULL DEFAULT 'default',         -- COMMENT '配置项Key'
    "Cluster"                   varchar(32)   NOT NULL DEFAULT 'default',         -- COMMENT '配置对应的集群，default为不针对特定的集群'
    "Value"                     varchar(2048) NOT NULL DEFAULT 'default',         -- COMMENT '配置项值'
    "Comment"                   varchar(1024)          DEFAULT '',                -- COMMENT '注释'
    "IsDeleted"                 BOOL          NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT        NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)   NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)            DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,         -- COMMENT '最后修改时间'
    CONSTRAINT "ServerConfig_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "ServerConfig_UK_Key_Cluster_DeletedAt" UNIQUE ("Key", "Cluster", "DeletedAt")
);

CREATE INDEX "ServerConfig_DataChange_LastTime" ON apolloconfig."ServerConfig" ("DataChange_LastTime");

/* # Dump of table accesskey */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."AccessKey";

CREATE TABLE apolloconfig."AccessKey"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增主键'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "Secret"                    varchar(128) NOT NULL DEFAULT '',                -- COMMENT 'Secret'
    "IsEnabled"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: enabled, 0: disabled'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '最后修改时间'
    CONSTRAINT "AccessKey_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "AccessKey_UK_AppId_Secret_DeletedAt" UNIQUE ("AppId", "Secret", "DeletedAt")
);

CREATE INDEX "AccessKey_DataChange_LastTime" ON apolloconfig."AccessKey" ("DataChange_LastTime");

/* # Dump of table serviceregistry */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloconfig."ServiceRegistry";

CREATE TABLE apolloconfig."ServiceRegistry"
(
    "Id"                     BIGSERIAL,                           -- COMMENT '自增Id'
    "ServiceName"            VARCHAR(64)   NOT NULL,              -- COMMENT '服务名'
    "Uri"                    VARCHAR(64)   NOT NULL,              -- COMMENT '服务地址'
    "Cluster"                VARCHAR(64)   NOT NULL,              -- COMMENT '集群，可以用来标识apollo.cluster或者网络分区'
    "Metadata"               VARCHAR(1024) NOT NULL DEFAULT '{}', -- COMMENT '元-数据，key value结构的json object，为了方面后面扩展功能而不需要修改表结构'
    "DataChange_CreatedTime" TIMESTAMP     NOT NULL,              -- COMMENT '创建时间'
    "DataChange_LastTime"    TIMESTAMP     NOT NULL,              -- COMMENT '最后修改时间'
    CONSTRAINT "ServiceRegistry_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "ServiceRegistry_IX_UNIQUE_KEY" UNIQUE ("ServiceName", "Uri")
);

CREATE INDEX "ServiceRegistry_IX_DataChange_LastTime" ON apolloconfig."ServiceRegistry" ("DataChange_LastTime");


/* # Config */
/* # ------------------------------------------------------------ */
INSERT INTO apolloconfig."ServerConfig" ("Key", "Cluster", "Value", "Comment")
VALUES ('eureka.service.url', 'default', 'http://localhost:8080/eureka/', 'Eureka服务Url，多个service以英文逗号分隔'),
       ('namespace.lock.switch', 'default', 'false', '一次发布只能有一个人修改开关'),
       ('item.key.length.limit', 'default', '128', 'item key 最大长度限制'),
       ('item.value.length.limit', 'default', '20000', 'item value最大长度限制'),
       ('config-service.cache.enabled', 'default', 'false',
        'ConfigService是否开启缓存，开启后能提高性能，但是会增大内存消耗！');
