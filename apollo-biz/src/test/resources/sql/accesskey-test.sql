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
INSERT INTO "AccessKey" (`Id`, `AppId`, `Secret`, `Mode`, `IsEnabled`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
	(1, 'someAppId', 'someSecret', 0, 0, 0, 'apollo', '2019-12-19 10:28:40', 'apollo', '2019-12-19 10:28:40'),
	(2, '100004458', 'c715cbc80fc44171b43732c3119c9456', 0, 0, 0, 'apollo', '2019-12-19 10:39:54', 'apollo', '2019-12-19 14:46:35'),
	(3, '100004458', '25a0e68d2a3941edb1ed3ab6dd0646cd', 0, 0, 1, 'apollo', '2019-12-19 13:44:13', 'apollo', '2019-12-19 13:44:19'),
	(4, '100004458', '4003c4d7783443dc9870932bebf3b7fe', 0, 0, 0, 'apollo', '2019-12-19 13:43:52', 'apollo', '2019-12-19 13:44:21');
