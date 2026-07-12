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
-- incarnation and soft-deleted after that Namespace was created. It deletes at
-- most 1000 rows from each table per execution and is safe to rerun.

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
        FROM `Namespace` n
        WHERE n.`AppId` = h.`AppId`
          AND n.`ClusterName` = h.`ClusterName`
          AND n.`NamespaceName` = h.`NamespaceName`
          AND n.`IsDeleted` = FALSE
          AND h.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
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
            FROM `Namespace` n
            WHERE n.`AppId` = h.`AppId`
              AND n.`ClusterName` = h.`ClusterName`
              AND n.`NamespaceName` = h.`NamespaceName`
              AND n.`IsDeleted` = FALSE
              AND h.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
          )) AS `RemainingRetentionReleaseHistoryRows`,
       (SELECT COUNT(*)
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
          )) AS `RemainingRetentionReleaseRows`,
       (SELECT COUNT(*)
        FROM `ReleaseHistory` h
        WHERE h.`IsDeleted` = TRUE
          AND NOT EXISTS (
            SELECT 1
            FROM `Namespace` n
            WHERE n.`AppId` = h.`AppId`
              AND n.`ClusterName` = h.`ClusterName`
              AND n.`NamespaceName` = h.`NamespaceName`
              AND n.`IsDeleted` = FALSE
              AND h.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
          )) AS `ExcludedSoftDeletedReleaseHistoryRows`,
       (SELECT COUNT(*)
        FROM `Release` r
        WHERE r.`IsDeleted` = TRUE
          AND NOT EXISTS (
            SELECT 1
            FROM `Namespace` n
            WHERE n.`AppId` = r.`AppId`
              AND n.`ClusterName` = r.`ClusterName`
              AND n.`NamespaceName` = r.`NamespaceName`
              AND n.`IsDeleted` = FALSE
              AND r.`DataChange_LastTime` >= n.`DataChange_CreatedTime`
          )) AS `ExcludedSoftDeletedReleaseRows`;
