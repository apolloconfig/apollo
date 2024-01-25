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
INSERT INTO "GrayReleaseRule" (`Id`, `AppId`, `ClusterName`, `NamespaceName`, `BranchName`, `Rules`, `ReleaseId`, `BranchStatus`)
VALUES
	(1, 'someAppId', 'default', 'application', 'gray-branch-1', '[{"clientAppId":"someAppId","clientIpList":["1.1.1.1"],"clientLabelList":["myLabel"]}]', 986, 1);
INSERT INTO "GrayReleaseRule" (`Id`, `AppId`, `ClusterName`, `NamespaceName`, `BranchName`, `Rules`, `ReleaseId`, `BranchStatus`)
VALUES
	(2, 'somePublicAppId', 'default', 'somePublicNamespace', 'gray-branch-2', '[{"clientAppId":"someAppId","clientIpList":["1.1.1.1"],"clientLabelList":["myLabel"]}]', 985, 1);
