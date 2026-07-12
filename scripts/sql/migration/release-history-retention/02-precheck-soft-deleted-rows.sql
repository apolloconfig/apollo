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

-- Run this read-only precheck against ApolloConfigDB. Retention cleanup candidates
-- must belong to the current active Namespace incarnation and must have been
-- soft-deleted after that Namespace was created. This excludes normal namespace
-- deletion rows and rows left by an older, subsequently recreated Namespace.

SELECT COUNT(*) AS `SoftDeletedReleaseHistoryRows`,
       COALESCE(SUM(EXISTS (
         SELECT 1
         FROM `Namespace` n
         WHERE n.`AppId` = h.`AppId`
           AND n.`ClusterName` = h.`ClusterName`
           AND n.`NamespaceName` = h.`NamespaceName`
           AND n.`IsDeleted` = FALSE
           AND h.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
       )), 0) AS `RetentionReleaseHistoryPurgeCandidates`,
       COALESCE(SUM(NOT EXISTS (
         SELECT 1
         FROM `Namespace` n
         WHERE n.`AppId` = h.`AppId`
           AND n.`ClusterName` = h.`ClusterName`
           AND n.`NamespaceName` = h.`NamespaceName`
           AND n.`IsDeleted` = FALSE
           AND h.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
       )), 0) AS `ExcludedSoftDeletedReleaseHistoryRows`
FROM `ReleaseHistory` h
WHERE h.`IsDeleted` = TRUE;

SELECT COUNT(*) AS `SoftDeletedReleaseRows`,
       COALESCE(SUM(EXISTS (
         SELECT 1
         FROM `Namespace` n
         WHERE n.`AppId` = r.`AppId`
           AND n.`ClusterName` = r.`ClusterName`
           AND n.`NamespaceName` = r.`NamespaceName`
           AND n.`IsDeleted` = FALSE
           AND r.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
       )), 0) AS `RetentionScopedSoftDeletedReleaseRows`,
       COALESCE(SUM(NOT EXISTS (
         SELECT 1
         FROM `Namespace` n
         WHERE n.`AppId` = r.`AppId`
           AND n.`ClusterName` = r.`ClusterName`
           AND n.`NamespaceName` = r.`NamespaceName`
           AND n.`IsDeleted` = FALSE
           AND r.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
       )), 0) AS `ExcludedSoftDeletedReleaseRows`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE;

-- These retention-scoped release rows can be physically deleted now. More rows
-- may become eligible after retention-generated release histories are purged.
SELECT COUNT(*) AS `RetentionReleaseRowsEligibleForDeletion`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE
  AND EXISTS (
    SELECT 1
    FROM `Namespace` n
    WHERE n.`AppId` = r.`AppId`
      AND n.`ClusterName` = r.`ClusterName`
      AND n.`NamespaceName` = r.`NamespaceName`
      AND n.`IsDeleted` = FALSE
      AND r.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
  )
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
      AND g.`BranchStatus` = 1
      AND g.`ReleaseId` = r.`Id`
  );

-- Show the oldest retention-scoped namespace branches to help estimate cleanup scope.
SELECT h.`AppId`,
       h.`ClusterName`,
       h.`NamespaceName`,
       h.`BranchName`,
       COUNT(*) AS `SoftDeletedRows`,
       MIN(h.`Id`) AS `OldestId`,
       MAX(h.`Id`) AS `NewestId`
FROM `ReleaseHistory` h
WHERE h.`IsDeleted` = TRUE
  AND EXISTS (
    SELECT 1
    FROM `Namespace` n
    WHERE n.`AppId` = h.`AppId`
      AND n.`ClusterName` = h.`ClusterName`
      AND n.`NamespaceName` = h.`NamespaceName`
      AND n.`IsDeleted` = FALSE
      AND h.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
  )
GROUP BY h.`AppId`, h.`ClusterName`, h.`NamespaceName`, h.`BranchName`
ORDER BY `SoftDeletedRows` DESC
LIMIT 100;
