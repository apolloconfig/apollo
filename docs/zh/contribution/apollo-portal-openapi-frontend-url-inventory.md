# Apollo Portal 前端 URL 迁移清单（临时）

本文档由 `scripts/openapi/collect_portal_frontend_urls.py` 生成，用于跟踪 Portal 前端 API 调用到 OpenAPI 的迁移进度。迁移完成后应删除。

## 汇总

- 前端文件数: 28
- URL 条目数: 131
- OpenAPI 条目数: 131
- WebAPI 条目数: 0
- 未使用 `AppUtil.prefixPath()` 的条目数: 0

## 按来源汇总

| Source | OpenAPI | WebAPI | No prefix | Total |
| --- | ---: | ---: | ---: | ---: |
| `AccessKeyService.js` | 5 | 0 | 0 | 5 |
| `AppService.js` | 14 | 0 | 0 | 14 |
| `AuditLogService.js` | 6 | 0 | 0 | 6 |
| `ClusterService.js` | 3 | 0 | 0 | 3 |
| `CommitService.js` | 1 | 0 | 0 | 1 |
| `CommonService.js` | 1 | 0 | 0 | 1 |
| `ConfigService.js` | 12 | 0 | 0 | 12 |
| `ConsumerService.js` | 5 | 0 | 0 | 5 |
| `EnvService.js` | 2 | 0 | 0 | 2 |
| `ExportService.js` | 1 | 0 | 0 | 1 |
| `FavoriteService.js` | 4 | 0 | 0 | 4 |
| `GlobalSearchValueService.js` | 1 | 0 | 0 | 1 |
| `InstanceService.js` | 4 | 0 | 0 | 4 |
| `NamespaceBranchService.js` | 6 | 0 | 0 | 6 |
| `NamespaceLockService.js` | 1 | 0 | 0 | 1 |
| `NamespaceService.js` | 11 | 0 | 0 | 11 |
| `OrganizationService.js` | 1 | 0 | 0 | 1 |
| `PermissionService.js` | 20 | 0 | 0 | 20 |
| `ReleaseHistoryService.js` | 1 | 0 | 0 | 1 |
| `ReleaseService.js` | 6 | 0 | 0 | 6 |
| `ServerConfigService.js` | 6 | 0 | 0 | 6 |
| `SystemInfoService.js` | 2 | 0 | 0 | 2 |
| `SystemRoleService.js` | 4 | 0 | 0 | 4 |
| `UserService.js` | 4 | 0 | 0 | 4 |
| `controller/ConfigExportController.js` | 5 | 0 | 0 | 5 |
| `directive/directive.js` | 3 | 0 | 0 | 3 |
| `directive/import-namespace-modal-directive.js` | 1 | 0 | 0 | 1 |
| `directive/namespace-panel-directive.js` | 1 | 0 | 0 | 1 |

## URL 清单

| Source | Line | Action | Method | Surface | Prefix path | Path |
| --- | ---: | --- | --- | --- | --- | --- |
| `AccessKeyService.js` | 22 | `load_access_keys` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/accesskeys` |
| `AccessKeyService.js` | 26 | `create_access_key` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/accesskeys` |
| `AccessKeyService.js` | 30 | `remove_access_key` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/accesskeys/:id` |
| `AccessKeyService.js` | 34 | `enable_access_key` | `PUT` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/accesskeys/:id/activation` |
| `AccessKeyService.js` | 38 | `disable_access_key` | `PUT` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/accesskeys/:id/deactivation` |
| `AppService.js` | 22 | `find_apps` | `GET` | OpenAPI | yes | `/openapi/v1/apps` |
| `AppService.js` | 27 | `find_app_by_self` | `GET` | OpenAPI | yes | `/openapi/v1/apps/by-self` |
| `AppService.js` | 31 | `load_navtree` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/env-cluster-info` |
| `AppService.js` | 37 | `load_app` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId` |
| `AppService.js` | 41 | `create_app` | `POST` | OpenAPI | yes | `/openapi/v1/apps` |
| `AppService.js` | 45 | `update_app` | `PUT` | OpenAPI | yes | `/openapi/v1/apps/:appId` |
| `AppService.js` | 49 | `create_app_remote` | `POST` | OpenAPI | yes | `/openapi/v1/apps/envs/:env` |
| `AppService.js` | 53 | `find_miss_envs` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/miss-envs` |
| `AppService.js` | 58 | `create_missing_namespaces` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/missing-namespaces` |
| `AppService.js` | 62 | `find_missing_namespaces` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/missing-namespaces` |
| `AppService.js` | 68 | `delete_app` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId` |
| `AppService.js` | 72 | `allow_app_master_assign_role` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/roles/master` |
| `AppService.js` | 76 | `delete_app_master_assign_role` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/roles/master` |
| `AppService.js` | 80 | `has_create_application_role` | `GET` | OpenAPI | yes | `/openapi/v1/system/roles/create-application` |
| `AuditLogService.js` | 21 | `get_properties` | `GET` | OpenAPI | yes | `/openapi/v1/apollo/audit/properties` |
| `AuditLogService.js` | 26 | `find_all_logs` | `GET` | OpenAPI | yes | `/openapi/v1/apollo/audit/logs?page=:page&size=:size` |
| `AuditLogService.js` | 31 | `find_logs_by_opName` | `GET` | OpenAPI | yes | `/openapi/v1/apollo/audit/logs/opName?opName=:opName&page=:page&size=:size&startDate=:startDate&endDate=:endDate` |
| `AuditLogService.js` | 36 | `find_trace_details` | `GET` | OpenAPI | yes | `/openapi/v1/apollo/audit/trace?traceId=:traceId` |
| `AuditLogService.js` | 41 | `find_dataInfluences_by_field` | `GET` | OpenAPI | yes | `/openapi/v1/apollo/audit/logs/dataInfluences/field?entityName=:entityName&entityId=:entityId&fieldName=:fieldName&page=:page&size=:size` |
| `AuditLogService.js` | 46 | `search_by_name_or_type_or_operator` | `GET` | OpenAPI | yes | `/openapi/v1/apollo/audit/logs/by-name-or-type-or-operator?query=:query&page=:page&size=:size` |
| `ClusterService.js` | 21 | `create_cluster` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters` |
| `ClusterService.js` | 25 | `load_cluster` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName` |
| `ClusterService.js` | 29 | `delete_cluster` | `DELETE` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName` |
| `CommitService.js` | 22 | `find_commits` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/commits?page=:page` |
| `CommonService.js` | 23 | `page_setting` | `GET` | OpenAPI | yes | `/openapi/v1/page-settings` |
| `ConfigService.js` | 25 | `load_namespace` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName` |
| `ConfigService.js` | 30 | `load_public_namespace_for_associated_namespace` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/associated-public-namespace` |
| `ConfigService.js` | 35 | `load_all_namespaces` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces` |
| `ConfigService.js` | 40 | `find_items` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items` |
| `ConfigService.js` | 44 | `modify_items` | `PUT` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items` |
| `ConfigService.js` | 48 | `diff` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/diff` |
| `ConfigService.js` | 53 | `sync_item` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/synchronize` |
| `ConfigService.js` | 58 | `create_item` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items` |
| `ConfigService.js` | 62 | `update_item` | `PUT` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/encodedItems/:key` |
| `ConfigService.js` | 66 | `delete_item` | `DELETE` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/encodedItems/:key` |
| `ConfigService.js` | 70 | `syntax_check_text` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/validation` |
| `ConfigService.js` | 74 | `revoke_item` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/items/revocation` |
| `ConsumerService.js` | 23 | `create_consumer` | `POST` | OpenAPI | yes | `/openapi/v1/consumers` |
| `ConsumerService.js` | 28 | `get_consumer_token_by_appId` | `GET` | OpenAPI | yes | `/openapi/v1/consumer-tokens/by-appId` |
| `ConsumerService.js` | 33 | `assign_role_to_consumer` | `POST` | OpenAPI | yes | `/openapi/v1/consumers/:token/assign-role` |
| `ConsumerService.js` | 38 | `get_consumer_list` | `GET` | OpenAPI | yes | `/openapi/v1/consumers` |
| `ConsumerService.js` | 43 | `delete_consumer` | `DELETE` | OpenAPI | yes | `/openapi/v1/consumers/by-appId` |
| `EnvService.js` | 18 | `-` | `RESOURCE_BASE` | OpenAPI | yes | `/openapi/v1/envs` |
| `EnvService.js` | 22 | `find_all_envs` | `GET` | OpenAPI | yes | `/openapi/v1/envs` |
| `ExportService.js` | 21 | `importConfig` | `POST` | OpenAPI | yes | `/openapi/v1/import` |
| `FavoriteService.js` | 21 | `find_favorites` | `GET` | OpenAPI | yes | `/openapi/v1/favorites` |
| `FavoriteService.js` | 26 | `add_favorite` | `POST` | OpenAPI | yes | `/openapi/v1/favorites` |
| `FavoriteService.js` | 30 | `delete_favorite` | `DELETE` | OpenAPI | yes | `/openapi/v1/favorites/:favoriteId` |
| `FavoriteService.js` | 34 | `to_top` | `PUT` | OpenAPI | yes | `/openapi/v1/favorites/:favoriteId` |
| `GlobalSearchValueService.js` | 22 | `get_item_Info_by_key_and_Value` | `GET` | OpenAPI | yes | `/openapi/v1/global-search/item-info/by-key-or-value` |
| `InstanceService.js` | 29 | `find_instances_by_release` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/instances/by-release` |
| `InstanceService.js` | 35 | `find_instances_by_namespace` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/instances/by-namespace` |
| `InstanceService.js` | 41 | `find_by_releases_not_in` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/instances/by-namespace-and-releases-not-in` |
| `InstanceService.js` | 46 | `get_instance_count_by_namespace` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/instances` |
| `NamespaceBranchService.js` | 22 | `find_namespace_branch` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches` |
| `NamespaceBranchService.js` | 27 | `create_branch` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches` |
| `NamespaceBranchService.js` | 32 | `delete_branch` | `DELETE` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName` |
| `NamespaceBranchService.js` | 37 | `merge_and_release_branch` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/merge` |
| `NamespaceBranchService.js` | 42 | `find_branch_gray_rules` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/rules` |
| `NamespaceBranchService.js` | 47 | `update_branch_gray_rules` | `PUT` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/rules` |
| `NamespaceLockService.js` | 21 | `get_namespace_lock` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/lock` |
| `NamespaceService.js` | 22 | `find_public_namespaces` | `GET` | OpenAPI | yes | `/openapi/v1/appnamespaces` |
| `NamespaceService.js` | 26 | `createNamespace` | `POST` | OpenAPI | yes | `/openapi/v1/namespaces` |
| `NamespaceService.js` | 31 | `createAppNamespace` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/appnamespaces` |
| `NamespaceService.js` | 36 | `getNamespacePublishInfo` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/namespaces/releases/status` |
| `NamespaceService.js` | 40 | `deleteLinkedNamespace` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName` |
| `NamespaceService.js` | 44 | `getPublicAppNamespaceAllNamespaces` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/appnamespaces/:publicNamespaceName/instances` |
| `NamespaceService.js` | 49 | `loadAppNamespace` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/appnamespaces/:namespaceName` |
| `NamespaceService.js` | 53 | `deleteAppNamespace` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/appnamespaces/:namespaceName` |
| `NamespaceService.js` | 57 | `getLinkedNamespaceUsage` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/usage` |
| `NamespaceService.js` | 62 | `getNamespaceUsage` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/appnamespaces/:namespaceName/usage` |
| `NamespaceService.js` | 68 | `findPublicNamespaceNames` | `GET` | OpenAPI | yes | `/openapi/v1/appnamespaces` |
| `OrganizationService.js` | 22 | `find_organizations` | `GET` | OpenAPI | yes | `/openapi/v1/organizations` |
| `PermissionService.js` | 21 | `init_app_namespace_permission` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/namespaces/:namespaceName/permission-init` |
| `PermissionService.js` | 25 | `init_cluster_ns_permission` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/permission-init` |
| `PermissionService.js` | 29 | `has_app_permission` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/permissions/:permissionType` |
| `PermissionService.js` | 33 | `has_namespace_permission` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/namespaces/:namespaceName/permissions/:permissionType` |
| `PermissionService.js` | 37 | `has_namespace_env_permission` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/permissions/:permissionType` |
| `PermissionService.js` | 41 | `has_cluster_ns_permission` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/permissions/:permissionType` |
| `PermissionService.js` | 45 | `has_root_permission` | `GET` | OpenAPI | yes | `/openapi/v1/permissions/root` |
| `PermissionService.js` | 49 | `get_namespace_role_users` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/namespaces/:namespaceName/role-users` |
| `PermissionService.js` | 53 | `get_namespace_env_role_users` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/role-users` |
| `PermissionService.js` | 57 | `assign_namespace_role_to_user` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/namespaces/:namespaceName/roles/:roleType` |
| `PermissionService.js` | 61 | `assign_namespace_env_role_to_user` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/roles/:roleType` |
| `PermissionService.js` | 65 | `remove_namespace_role_from_user` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/namespaces/:namespaceName/roles/:roleType` |
| `PermissionService.js` | 69 | `remove_namespace_env_role_from_user` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/namespaces/:namespaceName/roles/:roleType` |
| `PermissionService.js` | 73 | `get_app_role_users` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/role-users` |
| `PermissionService.js` | 77 | `assign_app_role_to_user` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/roles/:roleType` |
| `PermissionService.js` | 81 | `remove_app_role_from_user` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/roles/:roleType` |
| `PermissionService.js` | 85 | `has_open_manage_app_master_role_limit` | `GET` | OpenAPI | yes | `/openapi/v1/system/role/manage-app-master` |
| `PermissionService.js` | 89 | `get_cluster_ns_role_users` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/role-users` |
| `PermissionService.js` | 93 | `assign_cluster_ns_role_to_user` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/roles/:roleType` |
| `PermissionService.js` | 97 | `remove_cluster_ns_role_from_user` | `DELETE` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/roles/:roleType` |
| `ReleaseHistoryService.js` | 21 | `find_release_history_by_namespace` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/releases/histories` |
| `ReleaseService.js` | 48 | `get` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/releases/:releaseId` |
| `ReleaseService.js` | 52 | `find_active_releases` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/releases/active` |
| `ReleaseService.js` | 57 | `compare` | `GET` | OpenAPI | yes | `/openapi/v1/envs/:env/releases/comparison` |
| `ReleaseService.js` | 61 | `release` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/releases` |
| `ReleaseService.js` | 65 | `gray_release` | `POST` | OpenAPI | yes | `/openapi/v1/envs/:env/apps/:appId/clusters/:clusterName/namespaces/:namespaceName/branches/:branchName/releases` |
| `ReleaseService.js` | 69 | `rollback` | `PUT` | OpenAPI | yes | `/openapi/v1/envs/:env/releases/:releaseId/rollback` |
| `ServerConfigService.js` | 21 | `create_portal_db_config` | `POST` | OpenAPI | yes | `/openapi/v1/server/portal-db/config` |
| `ServerConfigService.js` | 25 | `create_config_db_config` | `POST` | OpenAPI | yes | `/openapi/v1/server/envs/:env/config-db/config` |
| `ServerConfigService.js` | 29 | `delete_portal_db_config` | `DELETE` | OpenAPI | yes | `/openapi/v1/server/portal-db/config` |
| `ServerConfigService.js` | 33 | `delete_config_db_config` | `DELETE` | OpenAPI | yes | `/openapi/v1/server/envs/:env/config-db/config` |
| `ServerConfigService.js` | 38 | `find_portal_db_config` | `GET` | OpenAPI | yes | `/openapi/v1/server/portal-db/config/find-all-config` |
| `ServerConfigService.js` | 44 | `find_config_db_config` | `GET` | OpenAPI | yes | `/openapi/v1/server/envs/:env/config-db/config/find-all-config` |
| `SystemInfoService.js` | 21 | `load_system_info` | `GET` | OpenAPI | yes | `/openapi/v1/system-info` |
| `SystemInfoService.js` | 25 | `check_health` | `GET` | OpenAPI | yes | `/openapi/v1/system-info/health` |
| `SystemRoleService.js` | 21 | `add_create_application_role` | `POST` | OpenAPI | yes | `/openapi/v1/system/roles/create-application` |
| `SystemRoleService.js` | 25 | `delete_create_application_role` | `DELETE` | OpenAPI | yes | `/openapi/v1/system/roles/create-application` |
| `SystemRoleService.js` | 29 | `get_create_application_role_users` | `GET` | OpenAPI | yes | `/openapi/v1/system/roles/create-application/role-users` |
| `SystemRoleService.js` | 34 | `has_open_manage_app_master_role_limit` | `GET` | OpenAPI | yes | `/openapi/v1/system/role/manage-app-master` |
| `UserService.js` | 21 | `load_user` | `GET` | OpenAPI | yes | `/openapi/v1/user` |
| `UserService.js` | 26 | `find_users` | `GET` | OpenAPI | yes | `/openapi/v1/users?keyword=:keyword&includeInactiveUsers=:includeInactiveUsers&offset=:offset&limit=:limit` |
| `UserService.js` | 30 | `change_user_enabled` | `PUT` | OpenAPI | yes | `/openapi/v1/users/enabled` |
| `UserService.js` | 34 | `create_or_update_user` | `POST` | OpenAPI | yes | `/openapi/v1/users?isCreate=:isCreate` |
| `controller/ConfigExportController.js` | 70 | `$window.location.href` | `GET` | OpenAPI | yes | `/openapi/v1/configs/export?envs=:param` |
| `controller/ConfigExportController.js` | 100 | `-` | `POST` | OpenAPI | yes | `/openapi/v1/configs/import?envs=:param&conflictAction=:param` |
| `controller/ConfigExportController.js` | 144 | `-` | `HEAD` | OpenAPI | yes | `/openapi/v1/apps/:param/envs/:param/clusters/:param/export` |
| `controller/ConfigExportController.js` | 146 | `$window.location.href` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:param/envs/:param/clusters/:param/export` |
| `controller/ConfigExportController.js` | 175 | `-` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:param/envs/:param/clusters/:param/import?conflictAction=:param` |
| `directive/directive.js` | 41 | `ajax` | `GET` | OpenAPI | yes | `/openapi/v1/apps/search/by-appid-or-name` |
| `directive/directive.js` | 309 | `ajax` | `GET` | OpenAPI | yes | `/openapi/v1/users` |
| `directive/directive.js` | 360 | `ajax` | `GET` | OpenAPI | yes | `/openapi/v1/users` |
| `directive/import-namespace-modal-directive.js` | 57 | `-` | `POST` | OpenAPI | yes | `/openapi/v1/apps/:param/envs/:param/clusters/:param/namespaces/:param/items/import` |
| `directive/namespace-panel-directive.js` | 1187 | `$window.location.href` | `GET` | OpenAPI | yes | `/openapi/v1/apps/:param/envs/:param/clusters/:param/namespaces/:param/items/export` |
