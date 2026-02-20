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
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("userPermissionValidator")
public class UserPermissionValidator extends AbstractPermissionValidator
    implements PermissionValidator {

  private final UserInfoHolder userInfoHolder;
  private final RolePermissionService rolePermissionService;
  private final PortalConfig portalConfig;
  private final AppNamespaceService appNamespaceService;
  private final SystemRoleManagerService systemRoleManagerService;

  public UserPermissionValidator(final UserInfoHolder userInfoHolder,
      final RolePermissionService rolePermissionService, final PortalConfig portalConfig,
      final AppNamespaceService appNamespaceService,
      final SystemRoleManagerService systemRoleManagerService) {
    this.userInfoHolder = userInfoHolder;
    this.rolePermissionService = rolePermissionService;
    this.portalConfig = portalConfig;
    this.appNamespaceService = appNamespaceService;
    this.systemRoleManagerService = systemRoleManagerService;
  }


  @Override
  public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {

    boolean isPublicAppNamespace = appNamespace.isPublic();

    if (portalConfig.canAppAdminCreatePrivateNamespace() || isPublicAppNamespace) {
      return hasCreateNamespacePermission(appId);
    }

    return isSuperAdmin();
  }


  @Override
  public boolean isSuperAdmin() {
    return rolePermissionService.isSuperAdmin(userInfoHolder.getUser().getUserId());
  }

  @Override
  public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
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
    return !isAppAdmin(appId)
        && !hasOperateNamespacePermission(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasCreateApplicationPermission() {
    return systemRoleManagerService
        .hasCreateApplicationPermission(userInfoHolder.getUser().getUserId());
  }

  @Override
  public boolean hasCreateApplicationPermission(String userId) {
    return systemRoleManagerService.hasCreateApplicationPermission(userId);
  }

  @Override
  public boolean hasManageAppMasterPermission(String appId) {
    // the manage app master permission might not be initialized, so we need to check isSuperAdmin
    // first
    return isSuperAdmin() || (hasAssignRolePermission(appId) && systemRoleManagerService
        .hasManageAppMasterPermission(userInfoHolder.getUser().getUserId(), appId));
  }

  @Override
  public boolean hasCreateUserPermission() {
    return systemRoleManagerService.hasCreateUserPermission(userInfoHolder.getUser().getUserId());
  }

  @Override
  public boolean hasCreateUserPermission(String userId) {
    return systemRoleManagerService.hasCreateUserPermission(userId);
  }

  @Override
  protected boolean hasPermissions(List<Permission> requiredPerms) {
    if (requiredPerms == null || requiredPerms.isEmpty()) {
      return false;
    }
    String userId = userInfoHolder.getUser().getUserId();
    return rolePermissionService.hasAnyPermission(userId, requiredPerms);
  }
}
