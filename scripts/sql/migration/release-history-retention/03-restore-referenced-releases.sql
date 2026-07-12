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

-- Run this script against ApolloConfigDB after the restore precheck.
-- It restores at most 1000 releases per execution and is safe to rerun.

SET AUTOCOMMIT = FALSE;

UPDATE `Release` r
SET `IsDeleted` = FALSE,
    `DeletedAt` = 0,
    `DataChange_LastModifiedBy` = 'release-history-retention-migration'
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
        AND g.`BranchStatus` = 1
        AND g.`ReleaseId` = r.`Id`
    )
  );
