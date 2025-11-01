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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractPermissionValidator implements PermissionValidator {

  @Override
  public boolean hasModifyNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    List<Permission> requiredPermissions = Arrays.asList(
        new Permission(PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName)),
        new Permission(PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env)),
        new Permission(PermissionType.MODIFY_NAMESPACES_IN_CLUSTER,
            RoleUtils.buildClusterTargetId(appId, env, clusterName)));
    return hasPermissions(requiredPermissions);
  }

  @Override
  public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    List<Permission> requiredPermissions = Arrays.asList(
        new Permission(PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName)),
        new Permission(PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env)),
        new Permission(PermissionType.RELEASE_NAMESPACES_IN_CLUSTER,
            RoleUtils.buildClusterTargetId(appId, env, clusterName)));
    return hasPermissions(requiredPermissions);
  }

  @Override
  public boolean hasAssignRolePermission(String appId) {
    List<Permission> requiredPermissions =
        Collections.singletonList(new Permission(PermissionType.ASSIGN_ROLE, appId));
    return hasPermissions(requiredPermissions);
  }

  @Override
  public boolean hasCreateNamespacePermission(String appId) {
    List<Permission> requiredPermissions =
        Collections.singletonList(new Permission(PermissionType.CREATE_NAMESPACE, appId));
    return hasPermissions(requiredPermissions);
  }

  @Override
  public abstract boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace);

  @Override
  public boolean hasCreateClusterPermission(String appId) {
    List<Permission> requiredPermissions =
        Collections.singletonList(new Permission(PermissionType.CREATE_CLUSTER, appId));
    return hasPermissions(requiredPermissions);
  }

  @Override
  public abstract boolean isSuperAdmin();

  @Override
  public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
    return false;
  }

  @Override
  public abstract boolean hasCreateApplicationPermission();

  @Override
  public abstract boolean hasManageAppMasterPermission(String appId);

  protected abstract boolean hasPermissions(List<Permission> requiredPerms);
}
