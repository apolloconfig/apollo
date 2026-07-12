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

-- Run this script against ApolloConfigDB after the restore precheck. It only
-- restores releases from the current active Namespace incarnation, using the
-- millisecond-resolution DeletedAt boundary. It restores at most 1000 releases
-- per execution and is safe to rerun.
-- ReleaseId and PreviousReleaseId checks are split so MySQL 5.7 can use both indexes.

SET AUTOCOMMIT = FALSE;

UPDATE `Release` r
SET `IsDeleted` = FALSE,
    `DeletedAt` = 0,
    `DataChange_LastModifiedBy` = 'release-history-retention-migration'
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
  AND (
    EXISTS (
      SELECT 1
      FROM `ReleaseHistory` h FORCE INDEX (`IX_ReleaseId`)
      WHERE h.`IsDeleted` = FALSE
        AND h.`ReleaseId` = r.`Id`
    )
    OR EXISTS (
      SELECT 1
      FROM `ReleaseHistory` h FORCE INDEX (`IX_PreviousReleaseId`)
      WHERE h.`IsDeleted` = FALSE
        AND h.`PreviousReleaseId` = r.`Id`
    )
    OR EXISTS (
      SELECT 1
      FROM `GrayReleaseRule` g
      WHERE g.`IsDeleted` = FALSE
        AND g.`BranchStatus` = 1
        AND g.`ReleaseId` = r.`Id`
    )
  )
ORDER BY r.`Id`
LIMIT 1000;

COMMIT;

SET AUTOCOMMIT = TRUE;

SELECT COUNT(*) AS `RemainingReleasesNeedingRestore`
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
  AND (
    EXISTS (
      SELECT 1
      FROM `ReleaseHistory` h FORCE INDEX (`IX_ReleaseId`)
      WHERE h.`IsDeleted` = FALSE
        AND h.`ReleaseId` = r.`Id`
    )
    OR EXISTS (
      SELECT 1
      FROM `ReleaseHistory` h FORCE INDEX (`IX_PreviousReleaseId`)
      WHERE h.`IsDeleted` = FALSE
        AND h.`PreviousReleaseId` = r.`Id`
    )
    OR EXISTS (
      SELECT 1
      FROM `GrayReleaseRule` g
      WHERE g.`IsDeleted` = FALSE
        AND g.`BranchStatus` = 1
        AND g.`ReleaseId` = r.`Id`
    )
  );
