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
CREATE TABLE SPRING_SESSION
(
    PRIMARY_ID            VARCHAR(255) NOT NULL,
    SESSION_ID            VARCHAR(255) NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID VARCHAR(255) NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BLOB         NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);

INSERT INTO "ServerConfig" ("Key", "Value", "Comment", "DataChange_CreatedBy", "DataChange_CreatedTime")
VALUES
    ('apollo.portal.envs', 'dev', '可支持的环境列表', 'default', '1970-01-01 00:00:00'),
    ('organizations', '[{"orgId":"TEST1","orgName":"样例部门1"},{"orgId":"TEST2","orgName":"样例部门2"}]', '部门列表', 'default', '1970-01-01 00:00:00'),
    ('superAdmin', 'apollo', 'Portal超级管理员', 'default', '1970-01-01 00:00:00'),
    ('api.readTimeout', '10000', 'http接口read timeout', 'default', '1970-01-01 00:00:00'),
    ('consumer.token.salt', 'someSalt', 'consumer token salt', 'default', '1970-01-01 00:00:00'),
    ('admin.createPrivateNamespace.switch', 'true', '是否允许项目管理员创建私有namespace', 'default', '1970-01-01 00:00:00'),
    ('configView.memberOnly.envs', 'pro', '只对项目成员显示配置信息的环境列表，多个env以英文逗号分隔', 'default', '1970-01-01 00:00:00'),
    ('apollo.portal.meta.servers', '{}', '各环境Meta Service列表', 'default', '1970-01-01 00:00:00');

INSERT INTO "Users" ("Username", "Password", "UserDisplayName", "Email", "Enabled")
VALUES
    ('apollo', '$2a$10$7r20uS.BQ9uBpf3Baj3uQOZvMVvB1RN3PYoKE94gtz2.WAOuiiwXS', 'apollo', 'apollo@acme.com', 1);

INSERT INTO "Authorities" ("Username", "Authority")
VALUES
    ('apollo', 'ROLE_user');
CREATE ALIAS IF NOT EXISTS UNIX_TIMESTAMP FOR "com.ctrip.framework.apollo.common.jpa.H2Function.unixTimestamp";
