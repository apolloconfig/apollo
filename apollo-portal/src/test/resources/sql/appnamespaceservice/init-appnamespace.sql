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
INSERT INTO "AppNamespace" (`Id`, `Name`, `AppId`, `Format`, `IsPublic`, `Comment`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
	(139, 'FX.old', '100003173', 'properties', 1, '', 0, 'zhanglea', '2016-07-11 10:00:58', 'zhanglea', '2016-07-11 10:00:58'),
	(140, 'SCC.song0711-03', 'song0711-01', 'properties', 1, '', 0, 'song_s', '2016-07-11 10:04:09', 'song_s', '2016-07-11 10:04:09'),
	(141, 'SCC.song0711-04', 'song0711-01', 'properties', 1, '', 0, 'song_s', '2016-07-11 10:06:29', 'song_s', '2016-07-11 10:06:29'),
	(142, 'application', 'song0711-02', 'properties', 1, 'default app namespace', 0, 'song_s', '2016-07-11 11:18:24', 'song_s', '2016-07-11 11:18:24'),
	(143, 'TFF.song0711-02', 'song0711-02', 'properties', 0, '', 0, 'song_s', '2016-07-11 11:15:11', 'song_s', '2016-07-11 11:15:11'),
	(144, 'datasourcexml', '100003173', 'properties', 1, '', 0, 'apollo', '2016-07-11 12:08:29', 'apollo', '2016-07-11 12:08:29'),
	(145, 'datasource.xml', '100003173', 'xml', 0, '', 0, 'apollo', '2016-07-11 12:09:30', 'apollo', '2016-07-11 12:09:30'),
	(146, 'FX.private-01', '100003173', 'properties', 0, '', 0, 'apollo', '2016-07-11 12:09:30', 'apollo', '2016-07-11 12:09:30'),
	(147, 'datasource', '100003173', 'properties', 0, '', 0, 'apollo', '2016-07-11 12:09:30', 'apollo', '2016-07-11 12:09:30');

INSERT INTO "App" (`AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)
VALUES
	('1000', 'apollo-test', 'FX', '框架', 'song_s', 'song_s@ctrip.com', 0, 'song_s', 'song_s'),
	('song0711-01', 'song0711-01', 'SCC', '框架', 'song_s', 'song_s@ctrip.com', 0, 'song_s', 'song_s'),
	('song0711-02', 'song0711-02', 'SCC', '框架', 'song_s', 'song_s@ctrip.com', 0, 'song_s', 'song_s'),
	('100003173', 'apollo-portal', 'FX', '框架', 'song_s', 'song_s@ctrip.com', 0, 'song_s', 'song_s');
