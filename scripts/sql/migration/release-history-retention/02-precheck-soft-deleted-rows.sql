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
-- must belong to the current active Namespace incarnation. The millisecond-resolution
-- DeletedAt boundary excludes normal namespace deletion rows and rows left by an
-- older, subsequently recreated Namespace, including same-second recreation.

SELECT COUNT(*) AS `SoftDeletedReleaseHistoryRows`,
       COALESCE(SUM(EXISTS (
         SELECT 1
         FROM `Namespace` active_namespace
         WHERE active_namespace.`AppId` = h.`AppId`
           AND active_namespace.`ClusterName` = h.`ClusterName`
           AND active_namespace.`NamespaceName` = h.`NamespaceName`
           AND active_namespace.`IsDeleted` = FALSE
       ) AND NOT EXISTS (
         SELECT 1
         FROM `Namespace` deleted_namespace
         WHERE deleted_namespace.`AppId` = h.`AppId`
           AND deleted_namespace.`ClusterName` = h.`ClusterName`
           AND deleted_namespace.`NamespaceName` = h.`NamespaceName`
           AND deleted_namespace.`IsDeleted` = TRUE
           AND deleted_namespace.`DeletedAt` >= h.`DeletedAt`
       )), 0) AS `RetentionReleaseHistoryPurgeCandidates`,
       COALESCE(SUM(NOT EXISTS (
         SELECT 1
         FROM `Namespace` active_namespace
         WHERE active_namespace.`AppId` = h.`AppId`
           AND active_namespace.`ClusterName` = h.`ClusterName`
           AND active_namespace.`NamespaceName` = h.`NamespaceName`
           AND active_namespace.`IsDeleted` = FALSE
       ) OR EXISTS (
         SELECT 1
         FROM `Namespace` deleted_namespace
         WHERE deleted_namespace.`AppId` = h.`AppId`
           AND deleted_namespace.`ClusterName` = h.`ClusterName`
           AND deleted_namespace.`NamespaceName` = h.`NamespaceName`
           AND deleted_namespace.`IsDeleted` = TRUE
           AND deleted_namespace.`DeletedAt` >= h.`DeletedAt`
       )), 0) AS `ExcludedSoftDeletedReleaseHistoryRows`
FROM `ReleaseHistory` h
WHERE h.`IsDeleted` = TRUE;

SELECT COUNT(*) AS `SoftDeletedReleaseRows`,
       COALESCE(SUM(EXISTS (
         SELECT 1
         FROM `Namespace` active_namespace
         WHERE active_namespace.`AppId` = r.`AppId`
           AND active_namespace.`ClusterName` = r.`ClusterName`
           AND active_namespace.`NamespaceName` = r.`NamespaceName`
           AND active_namespace.`IsDeleted` = FALSE
       ) AND NOT EXISTS (
         SELECT 1
         FROM `Namespace` deleted_namespace
         WHERE deleted_namespace.`AppId` = r.`AppId`
           AND deleted_namespace.`ClusterName` = r.`ClusterName`
           AND deleted_namespace.`NamespaceName` = r.`NamespaceName`
           AND deleted_namespace.`IsDeleted` = TRUE
           AND deleted_namespace.`DeletedAt` >= r.`DeletedAt`
       )), 0) AS `RetentionScopedSoftDeletedReleaseRows`,
       COALESCE(SUM(NOT EXISTS (
         SELECT 1
         FROM `Namespace` active_namespace
         WHERE active_namespace.`AppId` = r.`AppId`
           AND active_namespace.`ClusterName` = r.`ClusterName`
           AND active_namespace.`NamespaceName` = r.`NamespaceName`
           AND active_namespace.`IsDeleted` = FALSE
       ) OR EXISTS (
         SELECT 1
         FROM `Namespace` deleted_namespace
         WHERE deleted_namespace.`AppId` = r.`AppId`
           AND deleted_namespace.`ClusterName` = r.`ClusterName`
           AND deleted_namespace.`NamespaceName` = r.`NamespaceName`
           AND deleted_namespace.`IsDeleted` = TRUE
           AND deleted_namespace.`DeletedAt` >= r.`DeletedAt`
       )), 0) AS `ExcludedSoftDeletedReleaseRows`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE;

-- These retention-scoped release rows can be physically deleted now. More rows
-- may become eligible after retention-generated release histories are purged.
-- Separate index-forced checks avoid the MySQL 5.7 full scan caused by an OR predicate.
SELECT COUNT(*) AS `RetentionReleaseRowsEligibleForDeletion`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE
  AND EXISTS (
    SELECT 1
    FROM `Namespace` active_namespace
    WHERE active_namespace.`AppId` = r.`AppId`
      AND active_namespace.`ClusterName` = r.`ClusterName`
      AND active_namespace.`NamespaceName` = r.`NamespaceName`
      AND active_namespace.`IsDeleted` = FALSE
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `Namespace` deleted_namespace
    WHERE deleted_namespace.`AppId` = r.`AppId`
      AND deleted_namespace.`ClusterName` = r.`ClusterName`
      AND deleted_namespace.`NamespaceName` = r.`NamespaceName`
      AND deleted_namespace.`IsDeleted` = TRUE
      AND deleted_namespace.`DeletedAt` >= r.`DeletedAt`
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `ReleaseHistory` h FORCE INDEX (`IX_ReleaseId`)
    WHERE h.`ReleaseId` = r.`Id`
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `ReleaseHistory` h FORCE INDEX (`IX_PreviousReleaseId`)
    WHERE h.`PreviousReleaseId` = r.`Id`
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
    FROM `Namespace` active_namespace
    WHERE active_namespace.`AppId` = h.`AppId`
      AND active_namespace.`ClusterName` = h.`ClusterName`
      AND active_namespace.`NamespaceName` = h.`NamespaceName`
      AND active_namespace.`IsDeleted` = FALSE
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `Namespace` deleted_namespace
    WHERE deleted_namespace.`AppId` = h.`AppId`
      AND deleted_namespace.`ClusterName` = h.`ClusterName`
      AND deleted_namespace.`NamespaceName` = h.`NamespaceName`
      AND deleted_namespace.`IsDeleted` = TRUE
      AND deleted_namespace.`DeletedAt` >= h.`DeletedAt`
  )
GROUP BY h.`AppId`, h.`ClusterName`, h.`NamespaceName`, h.`BranchName`
ORDER BY `SoftDeletedRows` DESC
LIMIT 100;
