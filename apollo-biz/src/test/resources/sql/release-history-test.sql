--
-- Copyright 2024 Apollo Authors
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
INSERT INTO ReleaseHistory (Id, AppId, ClusterName, NamespaceName, BranchName, ReleaseId,
                              PreviousReleaseId, Operation, OperationContext,
                              DataChange_CreatedBy, DataChange_LastModifiedBy)
VALUES (1, 'kl-app', 'default', 'application', 'default', 1, 0, 0, '{"isEmergencyPublish":false}', 'apollo', 'apollo'),
       (2, 'kl-app', 'default', 'application', 'default', 2, 1, 0, '{"isEmergencyPublish":false}', 'apollo', 'apollo'),
       (3, 'kl-app', 'default', 'application', 'default', 3, 2, 0, '{"isEmergencyPublish":false}', 'apollo', 'apollo'),
       (4, 'kl-app', 'default', 'application', 'default', 4, 3, 0, '{"isEmergencyPublish":false}', 'apollo', 'apollo'),
       (5, 'kl-app', 'default', 'application', 'default', 5, 4, 0, '{"isEmergencyPublish":false}', 'apollo', 'apollo'),
       (6, 'kl-app', 'default', 'application', 'default', 6, 5, 0, '{"isEmergencyPublish":false}', 'apollo', 'apollo');

INSERT INTO "Release" (Id, ReleaseKey, Name, Comment, AppId, ClusterName, NamespaceName, Configurations)
VALUES (1, 'TEST-RELEASE-KEY1', 'test','First Release','kl-app', 'default', 'application', '{"k1":"override-someDC-v1"}'),
       (2, 'TEST-RELEASE-KEY2', 'test','First Release','kl-app', 'default', 'application', '{"k1":"override-someDC-v1"}'),
       (3, 'TEST-RELEASE-KEY3', 'test','First Release','kl-app', 'default', 'application', '{"k1":"override-someDC-v1"}'),
       (4, 'TEST-RELEASE-KEY4', 'test','First Release','kl-app', 'default', 'application', '{"k1":"override-someDC-v1"}'),
       (5, 'TEST-RELEASE-KEY5', 'test','First Release','kl-app', 'default', 'application', '{"k1":"override-someDC-v1"}'),
       (6, 'TEST-RELEASE-KEY6', 'test','First Release','kl-app', 'default', 'application', '{"k1":"override-someDC-v1"}');

