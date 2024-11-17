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
-- delta schema to upgrade apollo portal db from v2.3.0 to v2.4.0

-- 
-- ===============================================================================
-- ==                                                                           ==
-- ==                     Generated from 'scripts/sql/src/'                     ==
-- == by running 'mvn compile -pl apollo-build-sql-converter -Psql-converter'.  ==
-- ==                              DO NOT EDIT !!!                              ==
-- ==                                                                           ==
-- ===============================================================================
-- 
-- 
-- Use Database
Use ApolloPortalDB;

ALTER TABLE `ConsumerToken`
    ADD COLUMN `RateLimit` int NOT NULL DEFAULT '0' COMMENT '限流值' AFTER `Token`;

-- 
-- ===============================================================================
-- ==                                                                           ==
-- ==                     Generated from 'scripts/sql/src/'                     ==
-- == by running 'mvn compile -pl apollo-build-sql-converter -Psql-converter'.  ==
-- ==                              DO NOT EDIT !!!                              ==
-- ==                                                                           ==
-- ===============================================================================
