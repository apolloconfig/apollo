# Apollo Portal 前端 URL 迁移清单（临时）

本文档由 `scripts/openapi/collect_portal_frontend_urls.py` 生成，用于跟踪 Portal 前端 service 到 OpenAPI 的迁移进度。迁移完成后应删除。

## 汇总

- Service 文件数：24
- URL 条目数：121
- OpenAPI 条目数：9
- WebAPI 条目数：112
- 未使用 `AppUtil.prefixPath()` 的条目数：0

## 按 Service 汇总

| Service | OpenAPI | WebAPI | No prefix | Total |
| --- | ---: | ---: | ---: | ---: |
| `AccessKeyService.js` | 0 | 5 | 0 | 5 |
| `AppService.js` | 4 | 9 | 0 | 13 |
| `AuditLogService.js` | 0 | 6 | 0 | 6 |
| `ClusterService.js` | 2 | 1 | 0 | 3 |
| `CommitService.js` | 0 | 1 | 0 | 1 |
| `CommonService.js` | 0 | 1 | 0 | 1 |
| `ConfigService.js` | 0 | 12 | 0 | 12 |
| `ConsumerService.js` | 0 | 5 | 0 | 5 |
| `EnvService.js` | 2 | 0 | 0 | 2 |
| `ExportService.js` | 0 | 1 | 0 | 1 |
| `FavoriteService.js` | 0 | 4 | 0 | 4 |
| `GlobalSearchValueService.js` | 0 | 1 | 0 | 1 |
| `InstanceService.js` | 0 | 4 | 0 | 4 |
| `NamespaceBranchService.js` | 0 | 6 | 0 | 6 |
| `NamespaceLockService.js` | 0 | 1 | 0 | 1 |
| `NamespaceService.js` | 0 | 11 | 0 | 11 |
| `OrganizationService.js` | 1 | 0 | 0 | 1 |
| `PermissionService.js` | 0 | 20 | 0 | 20 |
| `ReleaseHistoryService.js` | 0 | 1 | 0 | 1 |
| `ReleaseService.js` | 0 | 7 | 0 | 7 |
| `ServerConfigService.js` | 0 | 6 | 0 | 6 |
| `SystemInfoService.js` | 0 | 2 | 0 | 2 |
| `SystemRoleService.js` | 0 | 4 | 0 | 4 |
| `UserService.js` | 0 | 4 | 0 | 4 |

## URL 清单

| Service | Line | Action | Method | Surface | Prefix path | Path |
| --- | ---: | --- | --- | --- | --- | --- |
| `AccessKeyService.js` | 22 | `load_access_keys` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/accesskeys` |
| `AccessKeyService.js` | 26 | `create_access_key` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/accesskeys` |
| `AccessKeyService.js` | 30 | `remove_access_key` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/accesskeys/:id` |
| `AccessKeyService.js` | 34 | `enable_access_key` | `PUT` | WebAPI | yes | `/apps/:appId/envs/:env/accesskeys/:id/enable?mode=:mode` |
| `AccessKeyService.js` | 38 | `disable_access_key` | `PUT` | WebAPI | yes | `/apps/:appId/envs/:env/accesskeys/:id/disable` |
| `AppService.js` | 18 | `-` | `RESOURCE_BASE` | WebAPI | yes | `/apps/:appId` |
| `AppService.js` | 22 | `find_apps` | `GET` | OpenAPI | yes | `/openapi/v1/apps` |
| `AppService.js` | 27 | `find_app_by_self` | `GET` | OpenAPI | yes | `/openapi/v1/apps/by-self` |
| `AppService.js` | 32 | `load_navtree` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/navtree` |
| `AppService.js` | 40 | `create_app` | `POST` | WebAPI | yes | `/apps` |
| `AppService.js` | 44 | `update_app` | `PUT` | WebAPI | yes | `/apps/:appId` |
| `AppService.js` | 48 | `create_app_remote` | `POST` | WebAPI | yes | `/apps/envs/:env` |
| `AppService.js` | 52 | `find_miss_envs` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/miss_envs` |
| `AppService.js` | 56 | `create_missing_namespaces` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/missing-namespaces` |
| `AppService.js` | 60 | `find_missing_namespaces` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/missing-namespaces` |
| `AppService.js` | 68 | `allow_app_master_assign_role` | `POST` | WebAPI | yes | `/apps/:appId/system/master/:userId` |
| `AppService.js` | 72 | `delete_app_master_assign_role` | `DELETE` | WebAPI | yes | `/apps/:appId/system/master/:userId` |
| `AppService.js` | 76 | `has_create_application_role` | `GET` | WebAPI | yes | `/system/role/createApplication/:userId` |
| `AuditLogService.js` | 21 | `get_properties` | `GET` | WebAPI | yes | `/apollo/audit/properties` |
| `AuditLogService.js` | 26 | `find_all_logs` | `GET` | WebAPI | yes | `/apollo/audit/logs?page=:page&size=:size` |
| `AuditLogService.js` | 31 | `find_logs_by_opName` | `GET` | WebAPI | yes | `/apollo/audit/logs/opName?opName=:opName&page=:page&size=:size&startDate=:startDate&endDate=:endDate` |
| `AuditLogService.js` | 36 | `find_trace_details` | `GET` | WebAPI | yes | `/apollo/audit/trace?traceId=:traceId` |
| `AuditLogService.js` | 41 | `find_dataInfluences_by_field` | `GET` | WebAPI | yes | `/apollo/audit/logs/dataInfluences/field?entityName=:entityName&entityId=:entityId&fieldName=:fieldName&page=:page&size=:size` |
| `AuditLogService.js` | 46 | `search_by_name_or_type_or_operator` | `GET` | WebAPI | yes | `/apollo/audit/logs/by-name-or-type-or-operator?query=:query&page=:page&size=:size` |
| `ClusterService.js` | 21 | `create_cluster` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters` |
| `ClusterService.js` | 25 | `load_cluster` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName` |
| `ClusterService.js` | 29 | `delete_cluster` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName` |
| `CommitService.js` | 22 | `find_commits` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/commits?page=:page` |
| `CommonService.js` | 23 | `page_setting` | `GET` | WebAPI | yes | `/page-settings` |
| `ConfigService.js` | 22 | `load_namespace` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName` |
| `ConfigService.js` | 27 | `load_public_namespace_for_associated_namespace` | `GET` | WebAPI | yes | `/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/associated-public-namespace` |
| `ConfigService.js` | 32 | `load_all_namespaces` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces` |
| `ConfigService.js` | 37 | `find_items` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/items` |
| `ConfigService.js` | 41 | `modify_items` | `PUT` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/items` |
| `ConfigService.js` | 45 | `diff` | `POST` | WebAPI | yes | `/namespaces/:namespaceName/diff` |
| `ConfigService.js` | 50 | `sync_item` | `PUT` | WebAPI | yes | `/apps/:appId/namespaces/:namespaceName/items` |
| `ConfigService.js` | 55 | `create_item` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/item` |
| `ConfigService.js` | 59 | `update_item` | `PUT` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/item` |
| `ConfigService.js` | 63 | `delete_item` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/items/:itemId` |
| `ConfigService.js` | 67 | `syntax_check_text` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/syntax-check` |
| `ConfigService.js` | 71 | `revoke_item` | `PUT` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/revoke-items` |
| `ConsumerService.js` | 23 | `create_consumer` | `POST` | WebAPI | yes | `/consumers` |
| `ConsumerService.js` | 28 | `get_consumer_token_by_appId` | `GET` | WebAPI | yes | `/consumer-tokens/by-appId` |
| `ConsumerService.js` | 33 | `assign_role_to_consumer` | `POST` | WebAPI | yes | `/consumers/:token/assign-role` |
| `ConsumerService.js` | 38 | `get_consumer_list` | `GET` | WebAPI | yes | `/consumers` |
| `ConsumerService.js` | 43 | `delete_consumer` | `DELETE` | WebAPI | yes | `/consumers/by-appId` |
| `EnvService.js` | 18 | `-` | `RESOURCE_BASE` | OpenAPI | yes | `/openapi/v1/envs` |
| `EnvService.js` | 22 | `find_all_envs` | `GET` | OpenAPI | yes | `/openapi/v1/envs` |
| `ExportService.js` | 21 | `importConfig` | `POST` | WebAPI | yes | `/import` |
| `FavoriteService.js` | 21 | `find_favorites` | `GET` | WebAPI | yes | `/favorites` |
| `FavoriteService.js` | 26 | `add_favorite` | `POST` | WebAPI | yes | `/favorites` |
| `FavoriteService.js` | 30 | `delete_favorite` | `DELETE` | WebAPI | yes | `/favorites/:favoriteId` |
| `FavoriteService.js` | 34 | `to_top` | `PUT` | WebAPI | yes | `/favorites/:favoriteId` |
| `GlobalSearchValueService.js` | 22 | `get_item_Info_by_key_and_Value` | `GET` | WebAPI | yes | `/global-search/item-info/by-key-or-value` |
| `InstanceService.js` | 21 | `find_instances_by_release` | `GET` | WebAPI | yes | `/envs/:env/instances/by-release` |
| `InstanceService.js` | 26 | `find_instances_by_namespace` | `GET` | WebAPI | yes | `/envs/:env/instances/by-namespace` |
| `InstanceService.js` | 31 | `find_by_releases_not_in` | `GET` | WebAPI | yes | `/envs/:env/instances/by-namespace-and-releases-not-in` |
| `InstanceService.js` | 36 | `get_instance_count_by_namespace` | `GET` | WebAPI | yes | `/envs/:env/instances/by-namespace/count` |
| `NamespaceBranchService.js` | 22 | `find_namespace_branch` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches` |
| `NamespaceBranchService.js` | 27 | `create_branch` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches` |
| `NamespaceBranchService.js` | 32 | `delete_branch` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName` |
| `NamespaceBranchService.js` | 37 | `merge_and_release_branch` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/merge` |
| `NamespaceBranchService.js` | 42 | `find_branch_gray_rules` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/rules` |
| `NamespaceBranchService.js` | 47 | `update_branch_gray_rules` | `PUT` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/rules` |
| `NamespaceLockService.js` | 21 | `get_namespace_lock` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/lock-info` |
| `NamespaceService.js` | 22 | `find_public_namespaces` | `GET` | WebAPI | yes | `/appnamespaces/public` |
| `NamespaceService.js` | 26 | `createNamespace` | `POST` | WebAPI | yes | `/apps/:appId/namespaces` |
| `NamespaceService.js` | 31 | `createAppNamespace` | `POST` | WebAPI | yes | `/apps/:appId/appnamespaces?appendNamespacePrefix=:appendNamespacePrefix` |
| `NamespaceService.js` | 36 | `getNamespacePublishInfo` | `GET` | WebAPI | yes | `/apps/:appId/namespaces/publish_info` |
| `NamespaceService.js` | 40 | `deleteLinkedNamespace` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/linked-namespaces/:namespaceName` |
| `NamespaceService.js` | 44 | `getPublicAppNamespaceAllNamespaces` | `GET` | WebAPI | yes | `/envs/:env/appnamespaces/:publicNamespaceName/namespaces` |
| `NamespaceService.js` | 49 | `loadAppNamespace` | `GET` | WebAPI | yes | `/apps/:appId/appnamespaces/:namespaceName` |
| `NamespaceService.js` | 53 | `deleteAppNamespace` | `DELETE` | WebAPI | yes | `/apps/:appId/appnamespaces/:namespaceName` |
| `NamespaceService.js` | 57 | `getLinkedNamespaceUsage` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/linked-namespaces/:namespaceName/usage` |
| `NamespaceService.js` | 62 | `getNamespaceUsage` | `GET` | WebAPI | yes | `/apps/:appId/namespaces/:namespaceName/usage` |
| `NamespaceService.js` | 68 | `findPublicNamespaceNames` | `GET` | WebAPI | yes | `/appnamespaces/public/names` |
| `OrganizationService.js` | 22 | `find_organizations` | `GET` | OpenAPI | yes | `/openapi/v1/organizations` |
| `PermissionService.js` | 21 | `init_app_namespace_permission` | `POST` | WebAPI | yes | `/apps/:appId/initPermission` |
| `PermissionService.js` | 28 | `init_cluster_ns_permission` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/initNsPermission` |
| `PermissionService.js` | 35 | `has_app_permission` | `GET` | WebAPI | yes | `/apps/:appId/permissions/:permissionType` |
| `PermissionService.js` | 39 | `has_namespace_permission` | `GET` | WebAPI | yes | `/apps/:appId/namespaces/:namespaceName/permissions/:permissionType` |
| `PermissionService.js` | 43 | `has_namespace_env_permission` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/namespaces/:namespaceName/permissions/:permissionType` |
| `PermissionService.js` | 47 | `has_cluster_ns_permission` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/ns_permissions/:permissionType` |
| `PermissionService.js` | 51 | `has_root_permission` | `GET` | WebAPI | yes | `/permissions/root` |
| `PermissionService.js` | 55 | `get_namespace_role_users` | `GET` | WebAPI | yes | `/apps/:appId/namespaces/:namespaceName/role_users` |
| `PermissionService.js` | 59 | `get_namespace_env_role_users` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/namespaces/:namespaceName/role_users` |
| `PermissionService.js` | 63 | `assign_namespace_role_to_user` | `POST` | WebAPI | yes | `/apps/:appId/namespaces/:namespaceName/roles/:roleType` |
| `PermissionService.js` | 70 | `assign_namespace_env_role_to_user` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/namespaces/:namespaceName/roles/:roleType` |
| `PermissionService.js` | 77 | `remove_namespace_role_from_user` | `DELETE` | WebAPI | yes | `/apps/:appId/namespaces/:namespaceName/roles/:roleType?user=:user` |
| `PermissionService.js` | 81 | `remove_namespace_env_role_from_user` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/namespaces/:namespaceName/roles/:roleType?user=:user` |
| `PermissionService.js` | 85 | `get_app_role_users` | `GET` | WebAPI | yes | `/apps/:appId/role_users` |
| `PermissionService.js` | 89 | `assign_app_role_to_user` | `POST` | WebAPI | yes | `/apps/:appId/roles/:roleType` |
| `PermissionService.js` | 96 | `remove_app_role_from_user` | `DELETE` | WebAPI | yes | `/apps/:appId/roles/:roleType?user=:user` |
| `PermissionService.js` | 100 | `has_open_manage_app_master_role_limit` | `GET` | WebAPI | yes | `/system/role/manageAppMaster` |
| `PermissionService.js` | 104 | `get_cluster_ns_role_users` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/ns_role_users` |
| `PermissionService.js` | 108 | `assign_cluster_ns_role_to_user` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/ns_roles/:roleType` |
| `PermissionService.js` | 115 | `remove_cluster_ns_role_from_user` | `DELETE` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/ns_roles/:roleType?user=:user` |
| `ReleaseHistoryService.js` | 21 | `find_release_history_by_namespace` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/releases/histories` |
| `ReleaseService.js` | 21 | `get` | `GET` | WebAPI | yes | `/envs/:env/releases/:releaseId` |
| `ReleaseService.js` | 25 | `find_all_releases` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/releases/all` |
| `ReleaseService.js` | 30 | `find_active_releases` | `GET` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/releases/active` |
| `ReleaseService.js` | 35 | `compare` | `GET` | WebAPI | yes | `/envs/:env/releases/compare` |
| `ReleaseService.js` | 39 | `release` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/releases` |
| `ReleaseService.js` | 43 | `gray_release` | `POST` | WebAPI | yes | `/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/releases` |
| `ReleaseService.js` | 47 | `rollback` | `PUT` | WebAPI | yes | `/envs/:env/releases/:releaseId/rollback` |
| `ServerConfigService.js` | 21 | `create_portal_db_config` | `POST` | WebAPI | yes | `/server/portal-db/config` |
| `ServerConfigService.js` | 25 | `create_config_db_config` | `POST` | WebAPI | yes | `/server/envs/:env/config-db/config` |
| `ServerConfigService.js` | 29 | `delete_portal_db_config` | `DELETE` | WebAPI | yes | `/server/portal-db/config` |
| `ServerConfigService.js` | 33 | `delete_config_db_config` | `DELETE` | WebAPI | yes | `/server/envs/:env/config-db/config` |
| `ServerConfigService.js` | 38 | `find_portal_db_config` | `GET` | WebAPI | yes | `/server/portal-db/config/find-all-config` |
| `ServerConfigService.js` | 44 | `find_config_db_config` | `GET` | WebAPI | yes | `/server/envs/:env/config-db/config/find-all-config` |
| `SystemInfoService.js` | 21 | `load_system_info` | `GET` | WebAPI | yes | `/system-info` |
| `SystemInfoService.js` | 25 | `check_health` | `GET` | WebAPI | yes | `/system-info/health` |
| `SystemRoleService.js` | 21 | `add_create_application_role` | `POST` | WebAPI | yes | `/system/role/createApplication` |
| `SystemRoleService.js` | 25 | `delete_create_application_role` | `DELETE` | WebAPI | yes | `/system/role/createApplication/:userId` |
| `SystemRoleService.js` | 29 | `get_create_application_role_users` | `GET` | WebAPI | yes | `/system/role/createApplication` |
| `SystemRoleService.js` | 34 | `has_open_manage_app_master_role_limit` | `GET` | WebAPI | yes | `/system/role/manageAppMaster` |
| `UserService.js` | 21 | `load_user` | `GET` | WebAPI | yes | `/user` |
| `UserService.js` | 26 | `find_users` | `GET` | WebAPI | yes | `/users?keyword=:keyword&includeInactiveUsers=:includeInactiveUsers&offset=:offset&limit=:limit` |
| `UserService.js` | 30 | `change_user_enabled` | `PUT` | WebAPI | yes | `/users/enabled` |
| `UserService.js` | 34 | `create_or_update_user` | `POST` | WebAPI | yes | `/users?isCreate=:isCreate` |
