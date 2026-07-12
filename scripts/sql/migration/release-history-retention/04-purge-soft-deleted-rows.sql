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
-- been restored. It deletes at most 1000 rows from each table per execution
-- and is safe to rerun until both reported counts are 0.

SET AUTOCOMMIT = FALSE;

DELETE FROM `ReleaseHistory`
WHERE `Id` IN (
  SELECT `Id`
  FROM (
    SELECT `Id`
    FROM `ReleaseHistory`
    WHERE `IsDeleted` = TRUE
    ORDER BY `Id`
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
      )
    ORDER BY r.`Id`
    LIMIT 1000
  ) release_batch
);

COMMIT;

SET AUTOCOMMIT = TRUE;

SELECT (SELECT COUNT(*)
        FROM `ReleaseHistory`
        WHERE `IsDeleted` = TRUE) AS `RemainingSoftDeletedReleaseHistoryRows`,
       (SELECT COUNT(*)
        FROM `Release`
        WHERE `IsDeleted` = TRUE) AS `RemainingSoftDeletedReleaseRows`;
