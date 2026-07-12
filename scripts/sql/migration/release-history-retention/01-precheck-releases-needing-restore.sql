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
-- A soft-deleted release must be restored when an active release history or
-- active gray release rule still references it.

SELECT COUNT(*) AS `ReleasesNeedingRestore`,
       COALESCE(SUM(EXISTS (
         SELECT 1
         FROM `Release` active_release
         WHERE active_release.`ReleaseKey` = r.`ReleaseKey`
           AND active_release.`DeletedAt` = 0
           AND active_release.`Id` <> r.`Id`
       )), 0) AS `ActiveReleaseKeyConflicts`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE
  AND (
    EXISTS (
      SELECT 1
      FROM `ReleaseHistory` h
      WHERE h.`IsDeleted` = FALSE
        AND (h.`ReleaseId` = r.`Id` OR h.`PreviousReleaseId` = r.`Id`)
    )
    OR EXISTS (
      SELECT 1
      FROM `GrayReleaseRule` g
      WHERE g.`IsDeleted` = FALSE
        AND g.`ReleaseId` = r.`Id`
    )
  );

-- Inspect at most 100 affected releases. ActiveReleaseKeyConflict must be 0
-- before running the restore script because DeletedAt is reset to 0.
SELECT r.`Id`,
       r.`ReleaseKey`,
       r.`AppId`,
       r.`ClusterName`,
       r.`NamespaceName`,
       EXISTS (
         SELECT 1
         FROM `ReleaseHistory` h
         WHERE h.`IsDeleted` = FALSE
           AND h.`ReleaseId` = r.`Id`
       ) AS `ReferencedByReleaseHistory`,
       EXISTS (
         SELECT 1
         FROM `ReleaseHistory` h
         WHERE h.`IsDeleted` = FALSE
           AND h.`PreviousReleaseId` = r.`Id`
       ) AS `ReferencedAsPreviousRelease`,
       EXISTS (
         SELECT 1
         FROM `GrayReleaseRule` g
         WHERE g.`IsDeleted` = FALSE
           AND g.`ReleaseId` = r.`Id`
       ) AS `ReferencedByGrayReleaseRule`,
       EXISTS (
         SELECT 1
         FROM `Release` active_release
         WHERE active_release.`ReleaseKey` = r.`ReleaseKey`
           AND active_release.`DeletedAt` = 0
           AND active_release.`Id` <> r.`Id`
       ) AS `ActiveReleaseKeyConflict`
FROM `Release` r
WHERE r.`IsDeleted` = TRUE
  AND (
    EXISTS (
      SELECT 1
      FROM `ReleaseHistory` h
      WHERE h.`IsDeleted` = FALSE
        AND (h.`ReleaseId` = r.`Id` OR h.`PreviousReleaseId` = r.`Id`)
    )
    OR EXISTS (
      SELECT 1
      FROM `GrayReleaseRule` g
      WHERE g.`IsDeleted` = FALSE
        AND g.`ReleaseId` = r.`Id`
    )
  )
ORDER BY r.`Id`
LIMIT 100;
