INSERT INTO `app` ( `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('testApp', 'test', 'default', 'default', 'default', 'default', 0, 'default', 'default');

INSERT INTO `cluster` (`ID`, `Name`, `AppId`, `ParentClusterId`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES (1, 'default', 'testApp', 0, 0, 'default', 'default');
INSERT INTO `cluster` (`Name`, `AppId`, `ParentClusterId`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('child-cluster', 'testApp', 1, 0, 'default', 'default');

INSERT INTO `appnamespace` (`Name`, `AppId`, `Format`, `IsPublic`) VALUES ( 'application', 'testApp', 'properties', 0);

INSERT INTO `namespace` (`ID`, `AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES(1,'testApp', 'default', 'application', 0, 'apollo', 'apollo');
INSERT INTO `namespace` (`AppId`, `ClusterName`, `NamespaceName`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`)VALUES('testApp', 'child-cluster', 'application', 0, 'apollo', 'apollo');

INSERT INTO `commit` (`ChangeSets`, `AppId`, `ClusterName`, `NamespaceName`)VALUES('{}', 'testApp', 'default', 'application');
INSERT INTO `commit` (`ChangeSets`, `AppId`, `ClusterName`, `NamespaceName`, `DataChange_LastTime`)VALUES('{}', 'commitTestApp', 'default', 'application', '2020-08-22 10:00:00');

INSERT INTO `item` (`NamespaceId`, `Key`, `Value`, `Comment`, `LineNum`)VALUES(1, 'k1', 'v1', '', 1);

INSERT INTO `namespacelock` (`NamespaceId`)VALUES(1);

INSERT INTO `release` (`AppId`, `ClusterName`, `NamespaceName`, `Configurations`, `IsAbandoned`)VALUES('branch-test', 'default', 'application', '{}', 0);
INSERT INTO `release` (`AppId`, `ClusterName`, `NamespaceName`, `Configurations`, `IsAbandoned`)VALUES('branch-test', 'child-cluster', 'application', '{}', 0);

INSERT INTO `releasehistory` (`AppId`, `ClusterName`, `NamespaceName`, `BranchName`, `ReleaseId`, `PreviousReleaseId`, `Operation`, `OperationContext`)VALUES('branch-test', 'default', 'application', 'default', 0, 0, 7, '{}');

INSERT INTO `instanceconfig` (`ID`, `InstanceId`, `ConfigAppId`, `ConfigClusterName`, `ConfigNamespaceName`, `ReleaseKey`, `ReleaseDeliveryTime`, `DataChange_CreatedTime`, `DataChange_LastTime`)
VALUES
	(1, 90, 'testApp', 'default', 'application', '20160829134524-dee271ddf9fced58', '2016-08-29 13:45:24', '2016-08-30 17:03:32', '2016-10-19 11:13:47');








