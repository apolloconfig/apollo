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
INSERT INTO "ServerConfig" ("Key", "Cluster", "Value", "Comment", "DataChange_CreatedBy", "DataChange_CreatedTime")
VALUES
    ('eureka.service.url', 'default', 'http://localhost:8080/eureka/', 'Eureka服务Url，多个service以英文逗号分隔', 'default', '1970-01-01 00:00:00'),
    ('namespace.lock.switch', 'default', 'false', '一次发布只能有一个人修改开关', 'default', '1970-01-01 00:00:00'),
    ('item.key.length.limit', 'default', '128', 'item key 最大长度限制', 'default', '1970-01-01 00:00:00'),
    ('item.value.length.limit', 'default', '20000', 'item value最大长度限制', 'default', '1970-01-01 00:00:00'),
    ('config-service.cache.enabled', 'default', 'false', 'ConfigService是否开启缓存，开启后能提高性能，但是会增大内存消耗！', 'default', '1970-01-01 00:00:00');
CREATE ALIAS IF NOT EXISTS UNIX_TIMESTAMP FOR "com.ctrip.framework.apollo.common.jpa.H2Function.unixTimestamp";
