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
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenScope;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Permission validator combining portal user permissions with user token scope restrictions.
 */
@Component("userTokenPermissionValidator")
public class UserTokenPermissionValidator implements PermissionValidator {

  private static final Set<String> APP_READ_OPERATIONS =
      new HashSet<>(Arrays.asList(UserTokenOperation.CONFIG_READ, UserTokenOperation.CONFIG_MODIFY,
          UserTokenOperation.CONFIG_RELEASE, UserTokenOperation.NAMESPACE_CREATE,
          UserTokenOperation.NAMESPACE_DELETE, UserTokenOperation.CLUSTER_CREATE,
          UserTokenOperation.APP_MANAGE_ROLE, UserTokenOperation.SYSTEM_ADMIN));

  private final UserPermissionValidator userPermissionValidator;
  private final UserTokenService userTokenService;
  private final UserTokenAuthUtil userTokenAuthUtil;

  public UserTokenPermissionValidator(final UserPermissionValidator userPermissionValidator,
      final UserTokenService userTokenService, final UserTokenAuthUtil userTokenAuthUtil) {
    this.userPermissionValidator = userPermissionValidator;
    this.userTokenService = userTokenService;
    this.userTokenAuthUtil = userTokenAuthUtil;
  }

  @Override
  public boolean hasModifyNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    return userPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
        namespaceName) && scope().allowsOperation(UserTokenOperation.CONFIG_MODIFY)
        && scope().allowsNamespace(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName,
      String namespaceName) {
    return userPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
        namespaceName) && scope().allowsOperation(UserTokenOperation.CONFIG_RELEASE)
        && scope().allowsNamespace(appId, env, clusterName, namespaceName);
  }

  @Override
  public boolean hasAssignRolePermission(String appId) {
    return userPermissionValidator.hasAssignRolePermission(appId)
        && scope().allowsOperation(UserTokenOperation.APP_MANAGE_ROLE) && scope().allowsApp(appId);
  }

  @Override
  public boolean hasCreateNamespacePermission(String appId) {
    return userPermissionValidator.hasCreateNamespacePermission(appId)
        && scope().allowsOperation(UserTokenOperation.NAMESPACE_CREATE) && scope().allowsApp(appId);
  }

  @Override
  public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
    return userPermissionValidator.hasCreateAppNamespacePermission(appId, appNamespace)
        && scope().allowsOperation(UserTokenOperation.NAMESPACE_CREATE) && scope().allowsApp(appId);
  }

  @Override
  public boolean hasCreateClusterPermission(String appId) {
    return userPermissionValidator.hasCreateClusterPermission(appId)
        && scope().allowsOperation(UserTokenOperation.CLUSTER_CREATE) && scope().allowsApp(appId);
  }

  @Override
  public boolean isSuperAdmin() {
    return userPermissionValidator.isSuperAdmin()
        && scope().allowsOperation(UserTokenOperation.SYSTEM_ADMIN);
  }

  @Override
  public boolean hasReadApplicationPermission(String appId) {
    UserTokenScope scope = scope();
    return allowsAnyOperation(scope, APP_READ_OPERATIONS) && scope.allowsApp(appId);
  }

  @Override
  public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
    return !scope().allowsOperation(UserTokenOperation.CONFIG_READ)
        || !scope().allowsNamespace(appId, env, clusterName, namespaceName)
        || userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName);
  }

  @Override
  public boolean hasCreateApplicationPermission() {
    return userPermissionValidator.hasCreateApplicationPermission()
        && scope().allowsOperation(UserTokenOperation.APP_CREATE);
  }

  @Override
  public boolean hasCreateApplicationPermission(String userId) {
    return userPermissionValidator.hasCreateApplicationPermission(userId)
        && scope().allowsOperation(UserTokenOperation.APP_CREATE);
  }

  @Override
  public boolean hasManageUsersPermission() {
    return userPermissionValidator.hasManageUsersPermission()
        && scope().allowsOperation(UserTokenOperation.USER_MANAGE);
  }

  @Override
  public boolean hasDeleteNamespacePermission(String appId) {
    return userPermissionValidator.hasDeleteNamespacePermission(appId)
        && scope().allowsOperation(UserTokenOperation.NAMESPACE_DELETE) && scope().allowsApp(appId);
  }

  @Override
  public boolean hasManageAppMasterPermission(String appId) {
    return userPermissionValidator.hasManageAppMasterPermission(appId)
        && scope().allowsOperation(UserTokenOperation.APP_MANAGE_ROLE) && scope().allowsApp(appId);
  }

  private UserTokenScope scope() {
    UserToken userToken = userTokenAuthUtil.retrieveUserTokenFromCtx();
    return userTokenService.parseScope(userToken);
  }

  private boolean allowsAnyOperation(UserTokenScope scope, Set<String> operations) {
    for (String operation : operations) {
      if (scope.allowsOperation(operation)) {
        return true;
      }
    }
    return false;
  }
}
