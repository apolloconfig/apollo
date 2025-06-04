INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
  (1500, 'ModifyNamespacesInCluster', 'clusterApp+DEV+default', 'someOperator', 'someOperator'),
  (1501, 'ReleaseNamespacesInCluster', 'clusterApp+DEV+default', 'someOperator', 'someOperator');

INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
  (1500, 'ModifyNamespacesInCluster+clusterApp+DEV+default', 'someOperator', 'someOperator'),
  (1501, 'ReleaseNamespacesInCluster+clusterApp+DEV+default', 'someOperator', 'someOperator');

INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`) VALUES
  (1500, 1500, 1500),
  (1501, 1501, 1501);
