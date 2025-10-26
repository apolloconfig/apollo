--
-- Copyright 2025 Apollo Authors
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

-- Insert role association for user 5 (role 100), but role 100 has no permissions
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES
    (1000, 5, 100, 'test-operator', 'test-operator'); -- Insert role association record for user 5

-- Role 100 has no associated permissions in RolePermission table (no need to insert RolePermission records)