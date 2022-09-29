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

-- add other Config
INSERT INTO `ServerConfig` (`Key`, `Cluster`, `Value`, `Comment`)
VALUES
    ('DEFAULT_EXPIRED_AFTER_ACCESS_IN_MINUTES', 'default', '60', ''),
	('TRACER_EVENT_CACHE_INVALIDATE', 'default', 'ConfigCache.Invalidate', ''),
	('TRACER_EVENT_CACHE_LOAD', 'default', 'ConfigCache.LoadFromDB', ''),
	('TRACER_EVENT_CACHE_LOAD_ID', 'default', 'ConfigCache.LoadFromDBById', ''),
	('TRACER_EVENT_CACHE_GET', 'default', 'ConfigCache.Get', ''),
	('TRACER_EVENT_CACHE_GET_ID', 'default', 'ConfigCache.GetById', '');