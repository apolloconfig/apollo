/*
 * Copyright 2025 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.openapi.model.OpenAppRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenClusterNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenPermissionConditionDTO;
import java.util.List;
import java.util.Map;

/**
 * Server-side OpenAPI facade for permission checks and role assignments.
 */
public interface PermissionOpenApiService {

  void initAppPermission(String appId, String namespaceName, String operator);

  void initClusterNamespacePermission(String appId, String env, String clusterName,
      String operator);

  OpenPermissionConditionDTO hasAppPermission(String appId, String permissionType, String userId);

  OpenPermissionConditionDTO hasNamespacePermission(String appId, String namespaceName,
      String permissionType, String userId);

  OpenPermissionConditionDTO hasEnvNamespacePermission(String appId, String env,
      String namespaceName, String permissionType, String userId);

  OpenPermissionConditionDTO hasClusterNamespacePermission(String appId, String env,
      String clusterName, String permissionType, String userId);

  OpenPermissionConditionDTO hasRootPermission(String userId);

  OpenEnvNamespaceRoleUserDTO getNamespaceEnvRoleUsers(String appId, String env,
      String namespaceName);

  void assignNamespaceEnvRoleToUser(String appId, String env, String namespaceName, String roleType,
      String userId, String operator);

  void removeNamespaceEnvRoleFromUser(String appId, String env, String namespaceName,
      String roleType, String userId, String operator);

  OpenClusterNamespaceRoleUserDTO getClusterNamespaceRoles(String appId, String env,
      String clusterName);

  void assignClusterNamespaceRoleToUser(String appId, String env, String clusterName,
      String roleType, String userId, String operator);

  void removeClusterNamespaceRoleFromUser(String appId, String env, String clusterName,
      String roleType, String userId, String operator);

  OpenNamespaceRoleUserDTO getNamespaceRoles(String appId, String namespaceName);

  void assignNamespaceRoleToUser(String appId, String namespaceName, String roleType, String userId,
      String operator);

  void removeNamespaceRoleFromUser(String appId, String namespaceName, String roleType,
      String userId, String operator);

  OpenAppRoleUserDTO getAppRoles(String appId);

  void assignAppRoleToUser(String appId, String roleType, String userId, String operator);

  void removeAppRoleFromUser(String appId, String roleType, String userId, String operator);

  Map<String, Boolean> hasCreateApplicationPermission(String userId);

  void addCreateApplicationRoleToUsers(List<String> userIds, String operator);

  void deleteCreateApplicationRoleFromUser(String userId, String operator);

  List<String> getCreateApplicationRoleUsers();

  void addManageAppMasterRoleToUser(String appId, String userId, String operator);

  void removeManageAppMasterRoleFromUser(String appId, String userId, String operator);

  Map<String, Boolean> isManageAppMasterPermissionEnabled();
}
