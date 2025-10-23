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
package com.ctrip.framework.apollo.openapi.auth;

import static com.ctrip.framework.apollo.portal.service.SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.service.ConsumerRolePermissionService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.AbstractPermissionValidator;
import com.ctrip.framework.apollo.portal.component.PermissionValidator;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("consumerPermissionValidator")
public class ConsumerPermissionValidator extends AbstractPermissionValidator implements
    PermissionValidator {

  private final ConsumerRolePermissionService permissionService;
  private final ConsumerAuthUtil consumerAuthUtil;

  public ConsumerPermissionValidator(final ConsumerRolePermissionService permissionService,
      final ConsumerAuthUtil consumerAuthUtil) {
    this.permissionService = permissionService;
    this.consumerAuthUtil = consumerAuthUtil;
  }

  @Override
  public boolean hasModifyNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
    if (hasCreateNamespacePermission(appId)){
      return true;
    }
    return super.hasModifyNamespacePermission(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
    if (hasCreateNamespacePermission(appId)){
      return true;
    }
    return super.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
    throw new UnsupportedOperationException("Not supported operation");
  }

  @Override
  public boolean isSuperAdmin() {
    // openapi shouldn't be
    return false;
  }

  @Override
  public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
    throw new UnsupportedOperationException("Not supported operation");
  }

  @Override
  public boolean hasCreateApplicationPermission() {
    long consumerId = consumerAuthUtil.retrieveConsumerIdFromCtx();
    return permissionService.consumerHasPermission(consumerId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  @Override
  public boolean hasCreateApplicationPermission(String userId) {
    return false;
  }

  @Override
  public boolean hasManageAppMasterPermission(String appId) {
    throw new UnsupportedOperationException("Not supported operation");
  }

  @Override
  protected boolean hasPermissions(List<Permission> requiredPerms) {
    if (requiredPerms == null || requiredPerms.isEmpty()) {
      return false;
    }
    long consumerId = consumerAuthUtil.retrieveConsumerIdFromCtx();
    return permissionService.hasAnyPermission(consumerId, requiredPerms);
  }
}
