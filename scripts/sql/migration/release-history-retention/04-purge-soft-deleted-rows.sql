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

-- Run this script against ApolloConfigDB only after referenced releases have
-- been restored. It only purges rows belonging to the current active Namespace
-- incarnation. The millisecond-resolution DeletedAt boundary remains unambiguous
-- for same-second namespace recreation. It deletes at most 1000 rows from each
-- table per execution and is safe to rerun.
-- ReleaseId and PreviousReleaseId checks are split so MySQL 5.7 can use both indexes.

SET AUTOCOMMIT = FALSE;

DELETE FROM `ReleaseHistory`
WHERE `Id` IN (
  SELECT `Id`
  FROM (
    SELECT h.`Id`
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
    ORDER BY h.`Id`
    LIMIT 1000
  ) release_history_batch
);

DELETE FROM `Release`
WHERE `Id` IN (
  SELECT `Id`
  FROM (
    SELECT r.`Id`
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
      )
    ORDER BY r.`Id`
    LIMIT 1000
  ) release_batch
);

COMMIT;

SET AUTOCOMMIT = TRUE;

SELECT (SELECT COUNT(*)
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
          )) AS `RemainingRetentionReleaseHistoryRows`,
       (SELECT COUNT(*)
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
          )) AS `RemainingRetentionReleaseRows`,
       (SELECT COUNT(*)
        FROM `ReleaseHistory` h
        WHERE h.`IsDeleted` = TRUE
          AND (NOT EXISTS (
                 SELECT 1
                 FROM `Namespace` active_namespace
                 WHERE active_namespace.`AppId` = h.`AppId`
                   AND active_namespace.`ClusterName` = h.`ClusterName`
                   AND active_namespace.`NamespaceName` = h.`NamespaceName`
                   AND active_namespace.`IsDeleted` = FALSE
               )
               OR EXISTS (
                 SELECT 1
                 FROM `Namespace` deleted_namespace
                 WHERE deleted_namespace.`AppId` = h.`AppId`
                   AND deleted_namespace.`ClusterName` = h.`ClusterName`
                   AND deleted_namespace.`NamespaceName` = h.`NamespaceName`
                   AND deleted_namespace.`IsDeleted` = TRUE
                   AND deleted_namespace.`DeletedAt` >= h.`DeletedAt`
               ))) AS `ExcludedSoftDeletedReleaseHistoryRows`,
       (SELECT COUNT(*)
        FROM `Release` r
        WHERE r.`IsDeleted` = TRUE
          AND (NOT EXISTS (
                 SELECT 1
                 FROM `Namespace` active_namespace
                 WHERE active_namespace.`AppId` = r.`AppId`
                   AND active_namespace.`ClusterName` = r.`ClusterName`
                   AND active_namespace.`NamespaceName` = r.`NamespaceName`
                   AND active_namespace.`IsDeleted` = FALSE
               )
               OR EXISTS (
                 SELECT 1
                 FROM `Namespace` deleted_namespace
                 WHERE deleted_namespace.`AppId` = r.`AppId`
                   AND deleted_namespace.`ClusterName` = r.`ClusterName`
                   AND deleted_namespace.`NamespaceName` = r.`NamespaceName`
                   AND deleted_namespace.`IsDeleted` = TRUE
                   AND deleted_namespace.`DeletedAt` >= r.`DeletedAt`
               ))) AS `ExcludedSoftDeletedReleaseRows`;
