-- Insert role associations for user 6 (roles 200 and 201)
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES
    (1001, 6, 200, 'test-operator', 'test-operator'),
    (1002, 6, 201, 'test-operator', 'test-operator');

-- Insert role-permission associations (role 200 associated with permission 300, role 201 associated with permissions 301 and 302)
INSERT INTO "RolePermission" ("RoleId", "PermissionId") -- Assuming RolePermission table structure is (RoleId, PermissionId)
VALUES
    (200, 300),
    (201, 301),
    (201, 302);

-- Insert permission details (permission table)
INSERT INTO "Permission" ("Id", "PermissionType", "TargetId") -- Assuming Permission table structure is (Id, PermissionType, TargetId)
VALUES
    (300, 'app', 'app1'),
    (301, 'namespace', 'ns1'),
    (302, 'cluster', 'cluster1');