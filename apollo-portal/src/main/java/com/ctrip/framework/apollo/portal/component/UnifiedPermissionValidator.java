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
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import org.springframework.stereotype.Component;

@Component("unifiedPermissionValidator")
public class UnifiedPermissionValidator implements PermissionValidator {

  private final UserPermissionValidator userPermissionValidator;
  private final ConsumerPermissionValidator consumerPermissionValidator;

  public UnifiedPermissionValidator(UserPermissionValidator userPermissionValidator,
      ConsumerPermissionValidator consumerPermissionValidator) {
    this.userPermissionValidator = userPermissionValidator;
    this.consumerPermissionValidator = consumerPermissionValidator;
  }

  private PermissionValidator getDelegate() {
    String type = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(type)) {
      return userPermissionValidator;
    }
    if (UserIdentityConstants.CONSUMER.equals(type)) {
      return consumerPermissionValidator;
    }
    throw new IllegalStateException("Unknown authentication type");
  }

  @Override
  public boolean hasModifyNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    return getDelegate().hasModifyNamespacePermission(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    return getDelegate().hasReleaseNamespacePermission(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasAssignRolePermission(String appId) {
    return getDelegate().hasAssignRolePermission(appId);
  }

  @Override
  public boolean hasCreateNamespacePermission(String appId) {
    return getDelegate().hasCreateNamespacePermission(appId);
  }

  @Override
  public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
    return getDelegate().hasCreateAppNamespacePermission(appId, appNamespace);
  }

  @Override
  public boolean hasCreateClusterPermission(String appId) {
    return getDelegate().hasCreateClusterPermission(appId);
  }

  @Override
  public boolean isSuperAdmin() {
    return getDelegate().isSuperAdmin();
  }

  @Override
  public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
    return getDelegate().shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasCreateApplicationPermission() {
    return getDelegate().hasCreateApplicationPermission();
  }

  @Override
  public boolean hasCreateApplicationPermission(String userId) {
    return getDelegate().hasCreateApplicationPermission(userId);
  }

  @Override
  public boolean hasDeleteNamespacePermission(String appId) {
    return getDelegate().hasDeleteNamespacePermission(appId);
  }

  @Override
  public boolean hasManageAppMasterPermission(String appId) {
    return getDelegate().hasManageAppMasterPermission(appId);
  }

  @Override
  public boolean hasCreateUserPermission() {
    return getDelegate().hasCreateUserPermission();
  }

  @Override
  public boolean hasCreateUserPermission(String userId) {
    return getDelegate().hasCreateUserPermission(userId);
  }
}
