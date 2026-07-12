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

-- Run this read-only precheck against ApolloConfigDB.

SELECT COUNT(*) AS `SoftDeletedReleaseHistoryRows`
FROM `ReleaseHistory`
WHERE `IsDeleted` = TRUE;

SELECT COUNT(*) AS `SoftDeletedReleaseRows`
FROM `Release`
WHERE `IsDeleted` = TRUE;

-- These release rows can be physically deleted now. More rows may become
-- eligible after soft-deleted release histories are purged.
SELECT COUNT(*) AS `SoftDeletedReleaseRowsEligibleForDeletion`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE
  AND NOT EXISTS (
    SELECT 1
    FROM `ReleaseHistory` h
    WHERE h.`ReleaseId` = r.`Id`
       OR h.`PreviousReleaseId` = r.`Id`
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `GrayReleaseRule` g
    WHERE g.`IsDeleted` = FALSE
      AND g.`ReleaseId` = r.`Id`
  );

-- Show the oldest affected namespaces to help estimate cleanup scope.
SELECT `AppId`,
       `ClusterName`,
       `NamespaceName`,
       `BranchName`,
       COUNT(*) AS `SoftDeletedRows`,
       MIN(`Id`) AS `OldestId`,
       MAX(`Id`) AS `NewestId`
FROM `ReleaseHistory`
WHERE `IsDeleted` = TRUE
GROUP BY `AppId`, `ClusterName`, `NamespaceName`, `BranchName`
ORDER BY `SoftDeletedRows` DESC
LIMIT 100;
