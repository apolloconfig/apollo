-- User 7's permissions: app:app2
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES (1003, 7, 400, 'test-operator', 'test-operator');

INSERT INTO "RolePermission" ("RoleId", "PermissionId")
VALUES (400, 401);

INSERT INTO "Permission" ("Id", "PermissionType", "TargetId")
VALUES (401, 'app', 'app2');

-- User 8's permissions: namespace:ns2, cluster:cluster2
INSERT INTO "ConsumerRole" ("Id", "ConsumerId", "RoleId", "DataChange_CreatedBy", "DataChange_LastModifiedBy")
VALUES
    (1004, 8, 500, 'test-operator', 'test-operator'),
    (1005, 8, 501, 'test-operator', 'test-operator');

INSERT INTO "RolePermission" ("RoleId", "PermissionId")
VALUES
    (500, 502),
    (501, 503);

INSERT INTO "Permission" ("Id", "PermissionType", "TargetId")
VALUES
    (502, 'namespace', 'ns2'),
    (503, 'cluster', 'cluster2');