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

\c "ApolloPortalDB"
/* # Dump of table app */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."App";

CREATE TABLE apolloportal."App"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '主键'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "Name"                      varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT '应用名'
    "OrgId"                     varchar(32)  NOT NULL DEFAULT 'default',         -- COMMENT '部门Id'
    "OrgName"                   varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '部门名字'
    "OwnerName"                 varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ownerName'
    "OwnerEmail"                varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ownerEmail'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "App_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "App_UK_AppId_DeletedAt" UNIQUE ("AppId", "DeletedAt")
);

CREATE INDEX "App_Name_AppId" ON apolloportal."App" ("Name");
CREATE INDEX "App_DataChange_LastTime" ON apolloportal."App" ("DataChange_LastTime");

/* # Dump of table appnamespace */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."AppNamespace";

CREATE TABLE apolloportal."AppNamespace"
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

CREATE INDEX "AppNamespace_Name_AppId" ON apolloportal."AppNamespace" ("Name", "AppId");
CREATE INDEX "AppNamespace_DataChange_LastTime" ON apolloportal."AppNamespace" ("DataChange_LastTime");

/* # Dump of table consumer */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."Consumer";

CREATE TABLE apolloportal."Consumer"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增Id'
    "AppId"                     varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "Name"                      varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT '应用名'
    "OrgId"                     varchar(32)  NOT NULL DEFAULT 'default',         -- COMMENT '部门Id'
    "OrgName"                   varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '部门名字'
    "OwnerName"                 varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ownerName'
    "OwnerEmail"                varchar(500) NOT NULL DEFAULT 'default',         -- COMMENT 'ownerEmail'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Consumer_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Consumer_UK_AppId_DeletedAt" UNIQUE ("AppId", "DeletedAt")
);

CREATE INDEX "Consumer_IX_DataChange_LastTime" ON apolloportal."Consumer" ("DataChange_LastTime");

/* # Dump of table consumeraudit */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."ConsumerAudit";

CREATE TABLE apolloportal."ConsumerAudit"
(
    "Id"                     BIGSERIAL,                                        -- COMMENT '自增Id'
    "ConsumerId"             int                    DEFAULT NULL,              -- COMMENT 'Consumer Id'
    "Uri"                    varchar(1024) NOT NULL DEFAULT '',                -- COMMENT '访问的Uri'
    "Method"                 varchar(16)   NOT NULL DEFAULT '',                -- COMMENT '访问的Method'
    "DataChange_CreatedTime" timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastTime"    timestamp NULL DEFAULT CURRENT_TIMESTAMP,         -- COMMENT '最后修改时间'
    CONSTRAINT "ConsumerAudit_Id" PRIMARY KEY ("Id")
);

CREATE INDEX "ConsumerAudit_IX_ConsumerId" ON apolloportal."ConsumerAudit" ("ConsumerId");
CREATE INDEX "ConsumerAudit_IX_DataChange_LastTime" ON apolloportal."ConsumerAudit" ("DataChange_LastTime");


/* # Dump of table consumerrole */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."ConsumerRole";

CREATE TABLE apolloportal."ConsumerRole"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增Id'
    "ConsumerId"                int                  DEFAULT NULL,              -- COMMENT 'Consumer Id'
    "RoleId"                    int                  DEFAULT NULL,              -- COMMENT 'Role Id'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "ConsumerRole_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "ConsumerRole_UK_ConsumerId_RoleId_DeletedAt" UNIQUE ("ConsumerId", "RoleId", "DeletedAt")
);

CREATE INDEX "ConsumerRole_IX_RoleId" ON apolloportal."ConsumerRole" ("RoleId");
CREATE INDEX "ConsumerRole_IX_DataChange_LastTime" ON apolloportal."ConsumerRole" ("DataChange_LastTime");

/* # Dump of table consumertoken */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."ConsumerToken";

CREATE TABLE apolloportal."ConsumerToken"
(
    "Id"                        BIGSERIAL,                                           -- COMMENT '自增Id'
    "ConsumerId"                int                   DEFAULT NULL,                  -- COMMENT 'ConsumerId'
    "Token"                     varchar(128) NOT NULL DEFAULT '',                    -- COMMENT 'token'
    "Expires"                   timestamp    NOT NULL DEFAULT '2099-01-01 00:00:00', --  COMMENT 'token失效时间'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,                 -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',                   -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',             -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,     -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                    -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,            -- COMMENT '最后修改时间'
    CONSTRAINT "ConsumerToken_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "ConsumerToken_UK_Token_DeletedAt" UNIQUE ("Token", "DeletedAt")
);

CREATE INDEX "ConsumerToken_DataChange_LastTime" ON apolloportal."ConsumerToken" ("DataChange_LastTime");

/* # Dump of table favorite */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."Favorite";

CREATE TABLE apolloportal."Favorite"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '主键'
    "UserId"                    varchar(32) NOT NULL DEFAULT 'default',         -- COMMENT '收藏的用户'
    "AppId"                     varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT 'AppID'
    "Position"                  int         NOT NULL DEFAULT '10000',           -- COMMENT '收藏顺序'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "Favorite_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Favorite_UK_UserId_AppId_DeletedAt" UNIQUE ("UserId", "AppId", "DeletedAt")
);

CREATE INDEX "Favorite_AppId" ON apolloportal."Favorite" ("AppId");
CREATE INDEX "Favorite_DataChange_LastTime" ON apolloportal."Favorite" ("DataChange_LastTime");

/* # Dump of table permission */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."Permission";

CREATE TABLE apolloportal."Permission"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增Id'
    "PermissionType"            varchar(32)  NOT NULL DEFAULT '',                -- COMMENT '权限类型'
    "TargetId"                  varchar(256) NOT NULL DEFAULT '',                -- COMMENT '权限对象类型'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Permission_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Permission_UK_TargetId_PermissionType_DeletedAt" UNIQUE ("TargetId", "PermissionType", "DeletedAt")
);

CREATE INDEX "Permission_IX_DataChange_LastTime" ON apolloportal."Permission" ("DataChange_LastTime");

/* # Dump of table role */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."Role";

CREATE TABLE apolloportal."Role"
(
    "Id"                        BIGSERIAL,                                       -- COMMENT '自增Id'
    "RoleName"                  varchar(256) NOT NULL DEFAULT '',                -- COMMENT 'Role name'
    "IsDeleted"                 BOOL         NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT       NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)  NOT NULL DEFAULT 'default',         --  COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)           DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,        -- COMMENT '最后修改时间'
    CONSTRAINT "Role_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "Role_UK_RoleName_DeletedAt" UNIQUE ("RoleName", "DeletedAt")
);

CREATE INDEX "Role_IX_DataChange_LastTime" ON apolloportal."Role" ("DataChange_LastTime");

/* # Dump of table rolepermission */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."RolePermission";

CREATE TABLE apolloportal."RolePermission"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增Id'
    "RoleId"                    int                  DEFAULT NULL,              -- COMMENT 'Role Id'
    "PermissionId"              int                  DEFAULT NULL,              -- COMMENT 'Permission Id'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "RolePermission_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "RolePermission_UK_RoleId_PermissionId_DeletedAt" UNIQUE ("RoleId", "PermissionId", "DeletedAt")
);

CREATE INDEX "RolePermission_IX_DataChange_LastTime" ON apolloportal."RolePermission" ("DataChange_LastTime");
CREATE INDEX "RolePermission_IX_PermissionId" ON apolloportal."RolePermission" ("PermissionId");


/* # Dump of table serverconfig */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."ServerConfig";

CREATE TABLE apolloportal."ServerConfig"
(
    "Id"                        BIGSERIAL,
    "Key"                       varchar(64)   NOT NULL DEFAULT 'default',         -- COMMENT '配置项Key'
    "Value"                     varchar(2048) NOT NULL DEFAULT 'default',         -- COMMENT '配置项值'
    "Comment"                   varchar(1024)          DEFAULT '',                -- COMMENT '注释'
    "IsDeleted"                 BOOL          NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT        NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64)   NOT NULL DEFAULT 'default',         -- COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)            DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,         -- COMMENT '最后修改时间'
    CONSTRAINT "ServerConfig_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "ServerConfig_UK_Key_DeletedAt" UNIQUE ("Key", "DeletedAt")
);

CREATE INDEX "ServerConfig_IX_DataChange_LastTime" ON apolloportal."ServerConfig" ("DataChange_LastTime");

/* # Dump of table userrole */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."UserRole";

CREATE TABLE apolloportal."UserRole"
(
    "Id"                        BIGSERIAL,                                      -- COMMENT '自增Id'
    "UserId"                    varchar(128)         DEFAULT '',                -- COMMENT '用户身份标识'
    "RoleId"                    int                  DEFAULT NULL,              -- COMMENT 'Role Id'
    "IsDeleted"                 BOOL        NOT NULL DEFAULT false,             -- COMMENT '1: deleted, 0: normal'
    "DeletedAt"                 BIGINT      NOT NULL DEFAULT '0',               -- COMMENT 'Delete timestamp based on milliseconds'
    "DataChange_CreatedBy"      varchar(64) NOT NULL DEFAULT 'default',         --  COMMENT '创建人邮箱前缀'
    "DataChange_CreatedTime"    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP, -- COMMENT '创建时间'
    "DataChange_LastModifiedBy" varchar(64)          DEFAULT '',                -- COMMENT '最后修改人邮箱前缀'
    "DataChange_LastTime"       timestamp NULL DEFAULT CURRENT_TIMESTAMP,       -- COMMENT '最后修改时间'
    CONSTRAINT "UserRole_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "UK_UserId_RoleId_DeletedAt" UNIQUE ("UserId", "RoleId", "DeletedAt")
);

CREATE INDEX "UserRole_IX_DataChange_LastTime" ON apolloportal."UserRole" ("DataChange_LastTime");
CREATE INDEX "UserRole_IX_RoleId" ON apolloportal."UserRole" ("RoleId");

/* # Dump of table Users */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."Users";

CREATE TABLE apolloportal."Users"
(
    "Id"              BIGSERIAL,                               --  COMMENT '自增Id'
    "Username"        varchar(64)  NOT NULL DEFAULT 'default', -- COMMENT '用户登录账户'
    "Password"        varchar(512) NOT NULL DEFAULT 'default', -- COMMENT '密码'
    "UserDisplayName" varchar(512) NOT NULL DEFAULT 'default', -- COMMENT '用户名称'
    "Email"           varchar(64)  NOT NULL DEFAULT 'default', -- COMMENT '邮箱地址'
    "Enabled"         SMALLINT              DEFAULT NULL,      -- COMMENT '是否有效'
    CONSTRAINT "Users_Id" PRIMARY KEY ("Id"),
    CONSTRAINT "UK_Username" UNIQUE ("Username")
);


/* # Dump of table Authorities */
/* # ------------------------------------------------------------ */

DROP TABLE IF EXISTS apolloportal."Authorities";

CREATE TABLE apolloportal."Authorities"
(
    "Id"        BIGSERIAL,
    "Username"  varchar(64) NOT NULL,
    "Authority" varchar(50) NOT NULL,
    CONSTRAINT "Authorities_Id" PRIMARY KEY ("Id")
);


/* # Config */
/* # ------------------------------------------------------------ */
INSERT INTO apolloportal."ServerConfig" ("Key", "Value", "Comment")
VALUES ('apollo.portal.envs', 'dev', '可支持的环境列表'),
       ('organizations',
        '[{"orgId":"TEST1","orgName":"样例部门1"},{"orgId":"TEST2","orgName":"样例部门2"}]',
        '部门列表'),
       ('superAdmin', 'apollo', 'Portal超级管理员'),
       ('api.readTimeout', '10000', 'http接口read timeout'),
       ('consumer.token.salt', 'someSalt', 'consumer token salt'),
       ('admin.createPrivateNamespace.switch', 'true', '是否允许项目管理员创建私有namespace'),
       ('configView.memberOnly.envs', 'pro', '只对项目成员显示配置信息的环境列表，多个env以英文逗号分隔'),
       ('apollo.portal.meta.servers', '{}', '各环境Meta Service列表');


INSERT INTO apolloportal."Users" ("Username", "Password", "UserDisplayName", "Email", "Enabled")
VALUES ('apollo', '$2a$10$7r20uS.BQ9uBpf3Baj3uQOZvMVvB1RN3PYoKE94gtz2.WAOuiiwXS', 'apollo', 'apollo@acme.com', 1);

INSERT INTO apolloportal."Authorities" ("Username", "Authority")
VALUES ('apollo', 'ROLE_user');

-- spring session (https://github.com/spring-projects/spring-session/blob/main/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-postgresql.sql)
CREATE TABLE apolloportal.SPRING_SESSION
(
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT   NOT NULL,
    LAST_ACCESS_TIME      BIGINT   NOT NULL,
    MAX_INACTIVE_INTERVAL INT      NOT NULL,
    EXPIRY_TIME           BIGINT   NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON apolloportal.SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON apolloportal.SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON apolloportal.SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE apolloportal.SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
);

ALTER TABLE apolloportal.SPRING_SESSION_ATTRIBUTES
    ADD CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES apolloportal.SPRING_SESSION (PRIMARY_ID);
