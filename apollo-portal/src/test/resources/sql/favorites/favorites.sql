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
INSERT INTO "Favorite" (`Id`, `UserId`, `AppId`, `Position`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`)
VALUES
	(18, 'apollo', 'test0621-03', 10000, 0, 'apollo', '2016-10-10 17:45:30', 'apollo', '2016-10-10 17:45:30'),
	(19, 'apollo', '100003173', 9999, 0, 'apollo', '2016-10-10 17:45:42', 'apollo', '2016-10-10 17:51:12'),
	(20, 'apollo', 'test0621-01', 10000, 00000000, 'apollo', '2016-10-10 17:50:57', 'apollo', '2016-10-10 17:50:57'),
	(21, 'apollo', 'test0621-04', 10000, 00000000, 'apollo', '2016-10-10 17:55:03', 'apollo', '2016-10-10 17:55:03'),
	(22, 'apollo2', 'test0621-04', 10000, 00000000, 'apollo', '2016-10-10 17:55:21', 'apollo', '2016-10-10 17:55:21'),
	(23, 'apollo3', 'test0621-04', 10000, 00000000, 'apollo', '2016-10-10 17:55:21', 'apollo', '2016-10-10 17:55:21');
