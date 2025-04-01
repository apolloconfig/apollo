/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.entity.AppNamespace;

public interface PermissionValidator {

  boolean hasModifyNamespacePermission(String appId, String env, String clusterName,
      String namespaceName);

  boolean hasReleaseNamespacePermission(String appId, String env, String clusterName,
      String namespaceName);

  default boolean hasDeleteNamespacePermission(String appId) {
    return hasAssignRolePermission(appId) || isSuperAdmin();
  }

  default boolean hasOperateNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    return hasModifyNamespacePermission(appId, env, clusterName, namespaceName)
        || hasReleaseNamespacePermission(appId, env, clusterName, namespaceName);
  }

  boolean hasAssignRolePermission(String appId);

  boolean hasCreateNamespacePermission(String appId);

  boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace);

  boolean hasCreateClusterPermission(String appId);

  default boolean isAppAdmin(String appId) {
    return isSuperAdmin() || hasAssignRolePermission(appId);
  }

  boolean isSuperAdmin();

  boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName);

  boolean hasCreateApplicationPermission();

  boolean hasManageAppMasterPermission(String appId);
}
