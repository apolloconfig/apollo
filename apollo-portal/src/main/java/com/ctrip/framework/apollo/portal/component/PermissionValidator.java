/*
 * Copyright 2021 Apollo Authors
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
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("permissionValidator")
public class PermissionValidator {

  private final UserInfoHolder userInfoHolder;
  private final RolePermissionService rolePermissionService;
  private final PortalConfig portalConfig;
  private final AppNamespaceService appNamespaceService;
  private final SystemRoleManagerService systemRoleManagerService;

  @Autowired
  public PermissionValidator(
          final UserInfoHolder userInfoHolder,
          final RolePermissionService rolePermissionService,
          final PortalConfig portalConfig,
          final AppNamespaceService appNamespaceService,
          final SystemRoleManagerService systemRoleManagerService) {
    this.userInfoHolder = userInfoHolder;
    this.rolePermissionService = rolePermissionService;
    this.portalConfig = portalConfig;
    this.appNamespaceService = appNamespaceService;
    this.systemRoleManagerService = systemRoleManagerService;
  }

  public boolean hasModifyNamespacePermission(String appId, String namespaceName) {
    return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
        PermissionType.MODIFY_NAMESPACE,
        RoleUtils.buildNamespaceTargetId(appId, namespaceName));
  }

  public boolean hasModifyNamespacePermission(String appId, String namespaceName, String env) {
    return hasModifyNamespacePermission(appId, namespaceName) ||
        rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
            PermissionType.MODIFY_NAMESPACE, RoleUtils.buildNamespaceTargetId(appId, namespaceName, env));
  }

  public boolean hasReleaseNamespacePermission(String appId, String namespaceName) {
    return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
        PermissionType.RELEASE_NAMESPACE,
        RoleUtils.buildNamespaceTargetId(appId, namespaceName));
  }

  public boolean hasReleaseNamespacePermission(String appId, String namespaceName, String env) {
    return hasReleaseNamespacePermission(appId, namespaceName) ||
        rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
        PermissionType.RELEASE_NAMESPACE, RoleUtils.buildNamespaceTargetId(appId, namespaceName, env));
  }

  public boolean hasDeleteNamespacePermission(String appId) {
    return hasAssignRolePermission(appId) || isSuperAdmin();
  }

  public boolean hasOperateNamespacePermission(String appId, String namespaceName) {
    return hasModifyNamespacePermission(appId, namespaceName) || hasReleaseNamespacePermission(appId, namespaceName);
  }

  public boolean hasOperateNamespacePermission(String appId, String namespaceName, String env) {
    return hasOperateNamespacePermission(appId, namespaceName) ||
        hasModifyNamespacePermission(appId, namespaceName, env) ||
        hasReleaseNamespacePermission(appId, namespaceName, env);
  }

  public boolean hasAssignRolePermission(String appId) {
    return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
        PermissionType.ASSIGN_ROLE,
        appId);
  }

  public boolean hasCreateNamespacePermission(String appId) {

    return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
        PermissionType.CREATE_NAMESPACE,
        appId);
  }

  public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {

    boolean isPublicAppNamespace = appNamespace.isPublic();

    if (portalConfig.canAppAdminCreatePrivateNamespace() || isPublicAppNamespace) {
      return hasCreateNamespacePermission(appId);
    }

    return isSuperAdmin();
  }

  public boolean hasCreateClusterPermission(String appId) {
    return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
        PermissionType.CREATE_CLUSTER,
        appId);
  }

  public boolean isAppAdmin(String appId) {
    return isSuperAdmin() || hasAssignRolePermission(appId);
  }

  public boolean isSuperAdmin() {
    return rolePermissionService.isSuperAdmin(userInfoHolder.getUser().getUserId());
  }

  public boolean shouldHideConfigToCurrentUser(String appId, String env, String namespaceName) {
    // 1. check whether the current environment enables member only function
    if (!portalConfig.isConfigViewMemberOnly(env)) {
      return false;
    }

    // 2. public namespace is open to every one
    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);
    if (appNamespace != null && appNamespace.isPublic()) {
      return false;
    }

    // 3. check app admin and operate permissions
    return !isAppAdmin(appId) && !hasOperateNamespacePermission(appId, namespaceName, env);
  }

  public boolean hasCreateApplicationPermission() {
    return hasCreateApplicationPermission(userInfoHolder.getUser().getUserId());
  }

  public boolean hasCreateApplicationPermission(String userId) {
    return systemRoleManagerService.hasCreateApplicationPermission(userId);
  }

  public boolean hasManageAppMasterPermission(String appId) {
    // the manage app master permission might not be initialized, so we need to check isSuperAdmin first
    return isSuperAdmin() ||
        (hasAssignRolePermission(appId) &&
         systemRoleManagerService.hasManageAppMasterPermission(userInfoHolder.getUser().getUserId(), appId)
        );
  }
}
