-- Insert role association for user 5 (role 100), but role 100 has no permissions
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES
    (1000, 5, 100, 'test-operator', 'test-operator'); -- Insert role association record for user 5

-- Role 100 has no associated permissions in RolePermission table (no need to insert RolePermission records)