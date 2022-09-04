--
-- Copyright 2022 Apollo Authors
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

ALTER TABLE `Item`
    ADD COLUMN `Type` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '配置项类型，0: String，1: Number，2: Boolean，3: Json' AFTER `Key`;
