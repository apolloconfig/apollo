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

-- delta schema to upgrade apollo config db from v2.5.0 to v3.0.0

CREATE INDEX `IX_ReleaseId_BranchStatus_IsDeleted`
ON `GrayReleaseRule` (`ReleaseId`, `BranchStatus`, `IsDeleted`);
