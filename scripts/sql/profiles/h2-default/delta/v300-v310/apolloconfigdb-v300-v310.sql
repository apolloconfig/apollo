--
-- Copyright 2026 Apollo Authors
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

-- Change GrayReleaseRule.Rules from varchar(16000) to TEXT.
-- utf8mb4 counts VARCHAR(16000) as 64000 bytes toward the 65535 row-size limit,
-- so widening other columns (e.g. NamespaceName) fails with error 1118.

ALTER TABLE `GrayReleaseRule` MODIFY COLUMN `Rules` TEXT COMMENT '灰度规则';
