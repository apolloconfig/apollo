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

/*
	This sql is dumped from a apollo portal database.

	The logic is as follows

	create app：
		consumer-test-app-id-0
		consumer-test-app-id-1

	create consumer:
		consumer-test-app-role

	Authorization, let consumer-test-app-role manage:
		consumer-test-app-id-0:
			Authorization type: App
		consumer-test-app-id-1:
			Authorization type: Namespace
			Managed Namespace: application
*/

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40000 ALTER TABLE `App` DISABLE KEYS */;
INSERT INTO "App" (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 'consumer-test-app-id-0', 'consumer-test-app-id-0', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', 'apollo', 'apollo');
INSERT INTO "App" (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 'consumer-test-app-id-1', 'consumer-test-app-id-1', 'TEST2', '样例部门2', 'apollo', 'apollo@acme.com', 'apollo', 'apollo');
/*!40000 ALTER TABLE `App` ENABLE KEYS */;

/*!40000 ALTER TABLE `AppNamespace` DISABLE KEYS */;
INSERT INTO "AppNamespace" (`Id`, `Name`, `AppId`, `Format`, `Comment`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 'application', 'consumer-test-app-id-0', 'properties', 'default app namespace', 'apollo', 'apollo');
INSERT INTO "AppNamespace" (`Id`, `Name`, `AppId`, `Format`, `Comment`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 'application', 'consumer-test-app-id-1', 'properties', 'default app namespace', 'apollo', 'apollo');
/*!40000 ALTER TABLE `AppNamespace` ENABLE KEYS */;

/*!40000 ALTER TABLE `Consumer` DISABLE KEYS */;
INSERT INTO "Consumer" (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 'consumer-test-app-role', 'consumer-test-app-role', 'TEST2', '样例部门2', 'apollo', 'apollo@acme.com', 'apollo', 'apollo');
/*!40000 ALTER TABLE `Consumer` ENABLE KEYS */;

/*!40000 ALTER TABLE `ConsumerAudit` DISABLE KEYS */;
/*!40000 ALTER TABLE `ConsumerAudit` ENABLE KEYS */;

/*!40000 ALTER TABLE `ConsumerRole` DISABLE KEYS */;
INSERT INTO "ConsumerRole" (`Id`, `ConsumerId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 1000, 1000, 'apollo', 'apollo');
INSERT INTO "ConsumerRole" (`Id`, `ConsumerId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 1000, 11000, 'apollo', 'apollo');
/*!40000 ALTER TABLE `ConsumerRole` ENABLE KEYS */;

/*!40000 ALTER TABLE `ConsumerToken` DISABLE KEYS */;
INSERT INTO "ConsumerToken" (`Id`, `ConsumerId`, `Token`, `RateLimit`, `Expires`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 1000, '3c16bf5b1f44b465179253442460e8c0ad845289', 20, '2098-12-31 10:00:00', 'apollo', 'apollo');
/*!40000 ALTER TABLE `ConsumerToken` ENABLE KEYS */;

/*!40000 ALTER TABLE `Favorite` DISABLE KEYS */;
/*!40000 ALTER TABLE `Favorite` ENABLE KEYS */;

/*!40000 ALTER TABLE `Permission` DISABLE KEYS */;
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 'AssignRole', 'consumer-test-app-id-0', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 'CreateNamespace', 'consumer-test-app-id-0', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(3000, 'CreateCluster', 'consumer-test-app-id-0', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(4000, 'ManageAppMaster', 'consumer-test-app-id-0', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(5000, 'ModifyNamespace', 'consumer-test-app-id-0+application', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(6000, 'ReleaseNamespace', 'consumer-test-app-id-0+application', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(7000, 'ModifyNamespace', 'consumer-test-app-id-0+application+DEV', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(8000, 'ReleaseNamespace', 'consumer-test-app-id-0+application+DEV', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(9000, 'CreateNamespace', 'consumer-test-app-id-1', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(10000, 'AssignRole', 'consumer-test-app-id-1', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(11000, 'CreateCluster', 'consumer-test-app-id-1', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(12000, 'ManageAppMaster', 'consumer-test-app-id-1', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(13000, 'ModifyNamespace', 'consumer-test-app-id-1+application', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(14000, 'ReleaseNamespace', 'consumer-test-app-id-1+application', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(15000, 'ModifyNamespace', 'consumer-test-app-id-1+application+DEV', 'apollo', 'apollo');
INSERT INTO "Permission" (`Id`, `PermissionType`, `TargetId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(16000, 'ReleaseNamespace', 'consumer-test-app-id-1+application+DEV', 'apollo', 'apollo');
/*!40000 ALTER TABLE `Permission` ENABLE KEYS */;

/*!40000 ALTER TABLE `Role` DISABLE KEYS */;
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 'Master+consumer-test-app-id-0', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 'ManageAppMaster+consumer-test-app-id-0', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(3000, 'ModifyNamespace+consumer-test-app-id-0+application', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(4000, 'ReleaseNamespace+consumer-test-app-id-0+application', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(5000, 'ModifyNamespace+consumer-test-app-id-0+application+DEV', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(6000, 'ReleaseNamespace+consumer-test-app-id-0+application+DEV', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(7000, 'Master+consumer-test-app-id-1', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(8000, 'ManageAppMaster+consumer-test-app-id-1', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(9000, 'ModifyNamespace+consumer-test-app-id-1+application', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(10000, 'ReleaseNamespace+consumer-test-app-id-1+application', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(11000, 'ModifyNamespace+consumer-test-app-id-1+application+DEV', 'apollo', 'apollo');
INSERT INTO "Role" (`Id`, `RoleName`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(12000, 'ReleaseNamespace+consumer-test-app-id-1+application+DEV', 'apollo', 'apollo');
/*!40000 ALTER TABLE `Role` ENABLE KEYS */;

/*!40000 ALTER TABLE `RolePermission` DISABLE KEYS */;
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 1000, 1000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 1000, 2000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(3000, 1000, 3000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(4000, 2000, 4000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(5000, 3000, 5000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(6000, 4000, 6000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(7000, 5000, 7000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(8000, 6000, 8000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(9000, 7000, 9000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(10000, 7000, 10000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(11000, 7000, 11000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(12000, 8000, 12000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(13000, 9000, 13000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(14000, 10000, 14000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(15000, 11000, 15000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(16000, 12000, 16000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(17000, 13000, 17000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(18000, 13000, 18000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(19000, 13000, 19000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(20000, 14000, 20000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(21000, 15000, 21000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(22000, 16000, 22000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(23000, 17000, 23000, 'apollo', 'apollo');
INSERT INTO "RolePermission" (`Id`, `RoleId`, `PermissionId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(24000, 18000, 24000, 'apollo', 'apollo');
/*!40000 ALTER TABLE `RolePermission` ENABLE KEYS */;

/*!40000 ALTER TABLE `UserRole` DISABLE KEYS */;
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(1000, 'apollo', 1000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(2000, 'apollo', 3000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(3000, 'apollo', 4000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(4000, 'apollo', 7000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(5000, 'apollo', 9000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(6000, 'apollo', 10000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(7000, 'apollo', 13000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(8000, 'apollo', 15000, 'apollo', 'apollo');
INSERT INTO "UserRole" (`Id`, `UserId`, `RoleId`, `DataChange_CreatedBy`, `DataChange_LastModifiedBy`) VALUES
(9000, 'apollo', 16000, 'apollo', 'apollo');
/*!40000 ALTER TABLE `UserRole` ENABLE KEYS */;

/*!40000 ALTER TABLE `Users` DISABLE KEYS */;
INSERT INTO "Users" (`Id`, `Username`, `Password`, `UserDisplayName`, `Email`, `Enabled`) VALUES
(1000, 'apollo', '$2a$10$7r20uS.BQ9uBpf3Baj3uQOZvMVvB1RN3PYoKE94gtz2.WAOuiiwXS', 'apollo', 'apollo@acme.com', 1);
/*!40000 ALTER TABLE `Users` ENABLE KEYS */;

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
