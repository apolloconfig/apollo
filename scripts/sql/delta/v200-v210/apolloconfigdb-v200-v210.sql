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
# delta schema to upgrade apollo config db from v2.0.0 to v2.1.0

Use ApolloConfigDB;

-- add INDEX for ReleaseHistory table
CREATE INDEX IX_PreviousReleaseId ON ReleaseHistory(PreviousReleaseId);

ALTER TABLE `Item`
    ADD COLUMN `Type` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '配置项类型，0: String，1: Number，2: Boolean，3: JSON' AFTER `Key`;

CREATE TABLE `ServiceRegistry` (
    `Id`                     INT(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增Id',
    `ServiceName`            VARCHAR(64)   NOT NULL COMMENT '服务名',
    `Uri`                    VARCHAR(64)   NOT NULL COMMENT '服务地址',
    `Cluster`                VARCHAR(64)   NOT NULL COMMENT '集群，可以用来标识apollo.cluster或者网络分区',
    `Metadata`               VARCHAR(1024) NOT NULL DEFAULT '{}' COMMENT '元数据，key value结构的json object，为了方面后面扩展功能而不需要修改表结构',
    `DataChange_CreatedTime` TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `DataChange_LastTime`    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (`Id`),
    UNIQUE INDEX `IX_UNIQUE_KEY` (`ServiceName`, `Uri`),
    INDEX                    `IX_DataChange_LastTime` (`DataChange_LastTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册中心';