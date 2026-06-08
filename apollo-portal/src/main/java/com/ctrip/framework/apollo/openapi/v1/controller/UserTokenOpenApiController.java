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
package com.ctrip.framework.apollo.openapi.v1.controller;

import com.ctrip.framework.apollo.openapi.api.AppManagementApi;
import com.ctrip.framework.apollo.openapi.api.AppNamespaceManagementApi;
import com.ctrip.framework.apollo.openapi.api.ClusterManagementApi;
import com.ctrip.framework.apollo.openapi.api.EnvironmentManagementApi;
import com.ctrip.framework.apollo.openapi.api.ItemManagementApi;
import com.ctrip.framework.apollo.openapi.api.NamespaceLockManagementApi;
import com.ctrip.framework.apollo.openapi.api.NamespaceManagementApi;
import com.ctrip.framework.apollo.openapi.api.PermissionManagementApi;
import com.ctrip.framework.apollo.openapi.api.ReleaseManagementApi;
import com.ctrip.framework.apollo.openapi.api.UserManagementApi;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenCurrentCapability;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOpenApiAction;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenScope;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI endpoint exposing the current user token identity and scopes.
 */
@RestController("openapiUserTokenController")
@RequestMapping("/openapi/v1/user-tokens")
public class UserTokenOpenApiController {

  private static final String HTTP_GET = "GET";
  private static final String HTTP_POST = "POST";
  private static final String HTTP_PUT = "PUT";
  private static final String HTTP_DELETE = "DELETE";
  private static final String OPERATION_MATCH_ANY = "ANY";
  private static final String OPERATION_MATCH_NONE = "NONE";

  private static final List<String> NO_REQUIRED_OPERATIONS = Collections.emptyList();

  private static final List<String> APP_READ_OPERATIONS = Collections.unmodifiableList(
      Arrays.asList(UserTokenOperation.CONFIG_READ, UserTokenOperation.CONFIG_MODIFY,
          UserTokenOperation.CONFIG_RELEASE, UserTokenOperation.NAMESPACE_CREATE,
          UserTokenOperation.NAMESPACE_DELETE, UserTokenOperation.CLUSTER_CREATE,
          UserTokenOperation.APP_MANAGE_ROLE, UserTokenOperation.SYSTEM_ADMIN));

  private static final List<UserTokenOpenApiAction> ACTION_CATALOG =
      Collections.unmodifiableList(Arrays.asList(
          action("user-token.current", HTTP_GET, "/openapi/v1/user-tokens/current",
              NO_REQUIRED_OPERATIONS, "identity", "Read current user token identity and scopes."),
          action("user-token.capabilities", HTTP_GET,
              "/openapi/v1/user-tokens/current/capabilities", NO_REQUIRED_OPERATIONS, "identity",
              "Read current user token identity, scopes, and OpenAPI action catalog."),
          action("user-token.whoami", HTTP_GET, "/openapi/v1/user-tokens/whoami",
              NO_REQUIRED_OPERATIONS, "identity", "Alias for current user token identity."),
          action("user.current", HTTP_GET, UserManagementApi.PATH_GET_CURRENT_USER,
              NO_REQUIRED_OPERATIONS, "identity", "Read the user represented by this token."),
          action("env.list", HTTP_GET, EnvironmentManagementApi.PATH_GET_ENVS,
              NO_REQUIRED_OPERATIONS, "environment", "List configured Apollo environments."),

          action("app.create", HTTP_POST, AppManagementApi.PATH_CREATE_APP,
              UserTokenOperation.APP_CREATE, "app", "Create a new Apollo app."),
          action("app.create-in-env", HTTP_POST, AppManagementApi.PATH_CREATE_APP_IN_ENV,
              UserTokenOperation.APP_CREATE, "app-env", "Create an existing app in one env."),
          action("app.update", HTTP_PUT, AppManagementApi.PATH_UPDATE_APP,
              Arrays.asList(UserTokenOperation.APP_MANAGE_ROLE, UserTokenOperation.SYSTEM_ADMIN),
              "app", "Update app metadata."),
          action("app.delete", HTTP_DELETE, AppManagementApi.PATH_DELETE_APP,
              UserTokenOperation.SYSTEM_ADMIN, "app", "Delete an Apollo app."),
          action("app.list", HTTP_GET, AppManagementApi.PATH_FIND_APPS, APP_READ_OPERATIONS, "app",
              "List apps readable by the token scope."),
          action("app.authorized", HTTP_GET, AppManagementApi.PATH_FIND_APPS_AUTHORIZED,
              APP_READ_OPERATIONS, "app", "List apps authorized for the current identity."),
          action("app.by-self", HTTP_GET, AppManagementApi.PATH_GET_APPS_BY_SELF,
              APP_READ_OPERATIONS, "app", "Page through apps authorized for the current identity."),
          action("app.get", HTTP_GET, AppManagementApi.PATH_GET_APP, APP_READ_OPERATIONS, "app",
              "Read one app's metadata."),
          action("app.missing-envs", HTTP_GET, AppManagementApi.PATH_FIND_MISS_ENVS,
              APP_READ_OPERATIONS, "app", "List environments missing this app."),
          action("app.env-cluster-info", HTTP_GET, AppManagementApi.PATH_GET_ENV_CLUSTER_INFO,
              APP_READ_OPERATIONS, "app", "Read env and cluster information for one app."),
          action("app.env-clusters", HTTP_GET, AppManagementApi.PATH_GET_ENV_CLUSTERS,
              APP_READ_OPERATIONS, "app", "Read clusters grouped by env for one app."),

          action("cluster.get", HTTP_GET, ClusterManagementApi.PATH_GET_CLUSTER,
              APP_READ_OPERATIONS, "cluster", "Read one cluster's metadata."),
          action("cluster.create", HTTP_POST, ClusterManagementApi.PATH_CREATE_CLUSTER,
              UserTokenOperation.CLUSTER_CREATE, "cluster", "Create a cluster for one app/env."),

          action("app-namespace.list", HTTP_GET, AppNamespaceManagementApi.PATH_GET_APP_NAMESPACES,
              APP_READ_OPERATIONS, "namespace", "List app namespaces readable by the token scope."),
          action("app-namespace.list-by-app", HTTP_GET,
              AppNamespaceManagementApi.PATH_GET_APP_NAMESPACES_BY_APP_ID, APP_READ_OPERATIONS,
              "namespace", "List app namespaces for one app."),
          action("app-namespace.get", HTTP_GET, AppNamespaceManagementApi.PATH_FIND_APP_NAMESPACE,
              APP_READ_OPERATIONS, "namespace", "Read one app namespace definition."),
          action("app-namespace.usage", HTTP_GET,
              AppNamespaceManagementApi.PATH_FIND_APP_NAMESPACE_USAGE, APP_READ_OPERATIONS,
              "namespace", "Read app namespace usage."),
          action("app-namespace.public-instances", HTTP_GET,
              AppNamespaceManagementApi.PATH_GET_PUBLIC_APP_NAMESPACE_INSTANCES,
              UserTokenOperation.CONFIG_READ, "namespace",
              "List namespaces associated with a public namespace."),
          action("app-namespace.create", HTTP_POST,
              AppNamespaceManagementApi.PATH_CREATE_APP_NAMESPACE,
              UserTokenOperation.NAMESPACE_CREATE, "namespace", "Create an app namespace."),
          action("app-namespace.delete", HTTP_DELETE,
              AppNamespaceManagementApi.PATH_DELETE_APP_NAMESPACE,
              UserTokenOperation.NAMESPACE_DELETE, "namespace", "Delete an app namespace."),

          action("namespace.list", HTTP_GET, NamespaceManagementApi.PATH_FIND_NAMESPACES,
              UserTokenOperation.CONFIG_READ, "namespace",
              "List namespaces and configuration details in one cluster."),
          action("namespace.get", HTTP_GET, NamespaceManagementApi.PATH_FIND_NAMESPACE,
              UserTokenOperation.CONFIG_READ, "namespace",
              "Read one namespace and optional configuration details."),
          action("namespace.lock", HTTP_GET, NamespaceLockManagementApi.PATH_GET_NAMESPACE_LOCK,
              UserTokenOperation.CONFIG_READ, "namespace", "Read current namespace edit lock."),
          action("namespace.usage", HTTP_GET, NamespaceManagementApi.PATH_FIND_NAMESPACE_USAGE,
              UserTokenOperation.CONFIG_READ, "namespace", "Read namespace usage."),
          action("namespace.associated-public", HTTP_GET,
              NamespaceManagementApi.PATH_FIND_PUBLIC_NAMESPACE_FOR_ASSOCIATED_NAMESPACE,
              UserTokenOperation.CONFIG_READ, "namespace",
              "Read the public namespace associated with one namespace."),
          action("namespace.release-status", HTTP_GET,
              NamespaceManagementApi.PATH_GET_NAMESPACES_RELEASE_STATUS, APP_READ_OPERATIONS,
              "namespace", "Read namespace release status for one app."),
          action("namespace.missing", HTTP_GET, NamespaceManagementApi.PATH_FIND_MISSING_NAMESPACES,
              APP_READ_OPERATIONS, "namespace", "List namespaces missing in one cluster."),
          action("namespace.create", HTTP_POST, NamespaceManagementApi.PATH_CREATE_NAMESPACES,
              UserTokenOperation.NAMESPACE_CREATE, "namespace", "Create namespaces."),
          action("namespace.create-missing", HTTP_POST,
              NamespaceManagementApi.PATH_CREATE_MISSING_NAMESPACES,
              UserTokenOperation.NAMESPACE_CREATE, "namespace", "Create missing namespaces."),
          action("namespace.delete", HTTP_DELETE, NamespaceManagementApi.PATH_DELETE_NAMESPACE,
              UserTokenOperation.NAMESPACE_DELETE, "namespace", "Delete a namespace."),

          action("item.list", HTTP_GET, ItemManagementApi.PATH_FIND_ITEMS_BY_NAMESPACE,
              UserTokenOperation.CONFIG_READ, "item", "Page through namespace items."),
          action("item.branch-list", HTTP_GET, ItemManagementApi.PATH_FIND_BRANCH_ITEMS,
              UserTokenOperation.CONFIG_READ, "item", "Read branch namespace items."),
          action("item.get", HTTP_GET, ItemManagementApi.PATH_GET_ITEM,
              UserTokenOperation.CONFIG_READ, "item", "Read one item by key."),
          action("item.get-encoded", HTTP_GET, ItemManagementApi.PATH_GET_ITEM_BY_ENCODED_KEY,
              UserTokenOperation.CONFIG_READ, "item", "Read one item by encoded key."),
          action("item.diff", HTTP_POST, ItemManagementApi.PATH_COMPARE_ITEMS,
              UserTokenOperation.CONFIG_READ, "item", "Compare item differences."),
          action("item.create", HTTP_POST, ItemManagementApi.PATH_CREATE_ITEM,
              UserTokenOperation.CONFIG_MODIFY, "item", "Create one item."),
          action("item.update", HTTP_PUT, ItemManagementApi.PATH_UPDATE_ITEM,
              UserTokenOperation.CONFIG_MODIFY, "item", "Update one item."),
          action("item.update-encoded", HTTP_PUT, ItemManagementApi.PATH_UPDATE_ITEM_BY_ENCODED_KEY,
              UserTokenOperation.CONFIG_MODIFY, "item", "Update one item by encoded key."),
          action("item.delete", HTTP_DELETE, ItemManagementApi.PATH_DELETE_ITEM,
              UserTokenOperation.CONFIG_MODIFY, "item", "Delete one item."),
          action("item.delete-encoded", HTTP_DELETE,
              ItemManagementApi.PATH_DELETE_ITEM_BY_ENCODED_KEY, UserTokenOperation.CONFIG_MODIFY,
              "item", "Delete one item by encoded key."),
          action("item.batch-update-text", HTTP_PUT,
              ItemManagementApi.PATH_BATCH_UPDATE_ITEMS_BY_TEXT, UserTokenOperation.CONFIG_MODIFY,
              "item", "Replace namespace items from text."),
          action("item.revert", HTTP_POST, ItemManagementApi.PATH_REVERT_ITEMS,
              UserTokenOperation.CONFIG_MODIFY, "item", "Revert unpublished item changes."),
          action("item.sync", HTTP_POST, ItemManagementApi.PATH_SYNC_ITEMS,
              UserTokenOperation.CONFIG_MODIFY, "item", "Synchronize item changes."),
          action("item.syntax-check", HTTP_POST, ItemManagementApi.PATH_SYNTAX_CHECK,
              UserTokenOperation.CONFIG_MODIFY, "item", "Validate namespace text syntax."),

          action("release.latest", HTTP_GET, ReleaseManagementApi.PATH_LOAD_LATEST_ACTIVE_RELEASE,
              UserTokenOperation.CONFIG_READ, "release", "Read latest active release."),
          action("release.active-list", HTTP_GET, ReleaseManagementApi.PATH_FIND_ACTIVE_RELEASES,
              UserTokenOperation.CONFIG_READ, "release", "Page through active releases."),
          action("release.get", HTTP_GET, ReleaseManagementApi.PATH_GET_RELEASE_BY_ID,
              UserTokenOperation.CONFIG_READ, "release", "Read one release by id."),
          action("release.compare", HTTP_GET, ReleaseManagementApi.PATH_COMPARE_RELEASE,
              UserTokenOperation.CONFIG_READ, "release", "Compare two releases."),
          action("release.create", HTTP_POST, ReleaseManagementApi.PATH_CREATE_RELEASE,
              UserTokenOperation.CONFIG_RELEASE, "release", "Create a namespace release."),
          action("release.gray-create", HTTP_POST, ReleaseManagementApi.PATH_CREATE_GRAY_RELEASE,
              UserTokenOperation.CONFIG_RELEASE, "release", "Create a gray release."),
          action("release.gray-delete", HTTP_POST,
              ReleaseManagementApi.PATH_CREATE_GRAY_DEL_RELEASE, UserTokenOperation.CONFIG_RELEASE,
              "release", "Create a gray deletion release."),
          action("release.rollback", HTTP_PUT, ReleaseManagementApi.PATH_ROLLBACK,
              UserTokenOperation.CONFIG_RELEASE, "release", "Rollback a release."),

          action("role.app-users", HTTP_GET, PermissionManagementApi.PATH_GET_APP_ROLES,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "List app role users."),
          action("role.namespace-users", HTTP_GET, PermissionManagementApi.PATH_GET_NAMESPACE_ROLES,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "List namespace role users."),
          action("role.namespace-env-users", HTTP_GET,
              PermissionManagementApi.PATH_GET_NAMESPACE_ENV_ROLE_USERS,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "List namespace env role users."),
          action("role.cluster-namespace-users", HTTP_GET,
              PermissionManagementApi.PATH_GET_CLUSTER_NAMESPACE_ROLES,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "List cluster namespace role users."),
          action("role.assign-app", HTTP_POST, PermissionManagementApi.PATH_ASSIGN_APP_ROLE_TO_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "Assign an app role to a user."),
          action("role.assign-namespace", HTTP_POST,
              PermissionManagementApi.PATH_ASSIGN_NAMESPACE_ROLE_TO_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "Assign a namespace role to a user."),
          action("role.assign-namespace-env", HTTP_POST,
              PermissionManagementApi.PATH_ASSIGN_NAMESPACE_ENV_ROLE_TO_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "Assign a namespace env role to a user."),
          action("role.assign-cluster-namespace", HTTP_POST,
              PermissionManagementApi.PATH_ASSIGN_CLUSTER_NAMESPACE_ROLE_TO_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role",
              "Assign a cluster namespace role to a user."),
          action("role.remove-app", HTTP_DELETE,
              PermissionManagementApi.PATH_REMOVE_APP_ROLE_FROM_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "Remove an app role from a user."),
          action("role.remove-namespace", HTTP_DELETE,
              PermissionManagementApi.PATH_REMOVE_NAMESPACE_ROLE_FROM_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role", "Remove a namespace role from a user."),
          action("role.remove-namespace-env", HTTP_DELETE,
              PermissionManagementApi.PATH_REMOVE_NAMESPACE_ENV_ROLE_FROM_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role",
              "Remove a namespace env role from a user."),
          action("role.remove-cluster-namespace", HTTP_DELETE,
              PermissionManagementApi.PATH_REMOVE_CLUSTER_NAMESPACE_ROLE_FROM_USER,
              UserTokenOperation.APP_MANAGE_ROLE, "role",
              "Remove a cluster namespace role from a user."),

          action("user.search", HTTP_GET, UserManagementApi.PATH_SEARCH_USERS,
              UserTokenOperation.USER_MANAGE, "user", "Search users."),
          action("user.get", HTTP_GET, UserManagementApi.PATH_GET_USER_BY_USER_ID,
              UserTokenOperation.USER_MANAGE, "user", "Read one user by id."),
          action("user.create-or-update", HTTP_POST, UserManagementApi.PATH_CREATE_OR_UPDATE_USER,
              UserTokenOperation.USER_MANAGE, "user", "Create or update a user."),
          action("user.change-enabled", HTTP_PUT, UserManagementApi.PATH_CHANGE_USER_ENABLED,
              UserTokenOperation.USER_MANAGE, "user", "Enable or disable a user."),

          action("system.create-app-role-users", HTTP_GET,
              PermissionManagementApi.PATH_GET_CREATE_APPLICATION_ROLE_USERS,
              UserTokenOperation.SYSTEM_ADMIN, "system", "List create-application role users."),
          action("system.add-create-app-role-users", HTTP_POST,
              PermissionManagementApi.PATH_ADD_CREATE_APPLICATION_ROLE_TO_USERS,
              UserTokenOperation.SYSTEM_ADMIN, "system", "Grant create-application role to users."),
          action("system.remove-create-app-role-user", HTTP_DELETE,
              PermissionManagementApi.PATH_DELETE_CREATE_APPLICATION_ROLE_FROM_USER,
              UserTokenOperation.SYSTEM_ADMIN, "system",
              "Remove create-application role from one user."),
          action("system.add-manage-app-master", HTTP_POST,
              PermissionManagementApi.PATH_ADD_MANAGE_APP_MASTER_ROLE_TO_USER,
              UserTokenOperation.SYSTEM_ADMIN, "system",
              "Grant manage-app-master role to one user."),
          action("system.remove-manage-app-master", HTTP_DELETE,
              PermissionManagementApi.PATH_REMOVE_MANAGE_APP_MASTER_ROLE_FROM_USER,
              UserTokenOperation.SYSTEM_ADMIN, "system",
              "Remove manage-app-master role from one user."),
          action("system.root-permission", HTTP_GET,
              PermissionManagementApi.PATH_HAS_ROOT_PERMISSION, UserTokenOperation.SYSTEM_ADMIN,
              "system", "Check root permission.")));

  private final UserTokenService userTokenService;
  private final UserTokenAuthUtil userTokenAuthUtil;

  public UserTokenOpenApiController(final UserTokenService userTokenService,
      final UserTokenAuthUtil userTokenAuthUtil) {
    this.userTokenService = userTokenService;
    this.userTokenAuthUtil = userTokenAuthUtil;
  }

  @GetMapping({"/current", "/current/capabilities", "/whoami"})
  public ResponseEntity<UserTokenCurrentCapability> current() {
    UserToken userToken = requireUserToken();
    UserTokenScope scope = userTokenService.parseScope(userToken);

    UserTokenCurrentCapability capability = new UserTokenCurrentCapability();
    capability.setAuthType(UserIdentityConstants.USER_TOKEN);
    capability.setUserId(userToken.getUserId());
    capability.setTokenId(userToken.getId());
    capability.setTokenName(userToken.getName());
    capability.setTokenPrefix(userToken.getTokenPrefix());
    capability.setRateLimit(userToken.getRateLimit());
    capability.setExpires(userToken.getExpires());
    capability.setLastUsedTime(userToken.getLastUsedTime());
    capability.setDataChangeCreatedTime(userToken.getDataChangeCreatedTime());
    capability.setDenyAll(scope.isDenyAll());
    capability.setOperations(scope.getOperations());
    capability.setAllOperations(!scope.isDenyAll() && scope.getOperations().isEmpty());
    capability.setAppIds(scope.getAppIds());
    capability.setAllApps(!scope.isDenyAll() && scope.getAppIds().isEmpty());
    capability.setEnvs(scope.getEnvs());
    capability.setAllEnvs(!scope.isDenyAll() && scope.getEnvs().isEmpty());
    capability.setNamespaces(scope.getNamespaces());
    capability.setAllNamespaces(!scope.isDenyAll() && scope.getNamespaces().isEmpty());
    capability.setActions(actionsFor(scope));
    return ResponseEntity.ok(capability);
  }

  private UserToken requireUserToken() {
    if (!UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())) {
      throw new AccessDeniedException("User token is required");
    }
    UserToken userToken = userTokenAuthUtil.retrieveUserTokenFromCtx();
    if (userToken == null) {
      throw new AccessDeniedException("User token is required");
    }
    return userToken;
  }

  private List<UserTokenOpenApiAction> actionsFor(UserTokenScope scope) {
    return ACTION_CATALOG.stream().filter(action -> allowsAction(scope, action))
        .map(action -> withGrantedOperations(scope, action)).collect(Collectors.toList());
  }

  private boolean allowsAction(UserTokenScope scope, UserTokenOpenApiAction action) {
    List<String> requiredOperations = action.getRequiredOperations();
    if (requiredOperations == null || requiredOperations.isEmpty()) {
      return true;
    }
    for (String operation : requiredOperations) {
      if (scope.allowsOperation(operation)) {
        return true;
      }
    }
    return false;
  }

  private UserTokenOpenApiAction withGrantedOperations(UserTokenScope scope,
      UserTokenOpenApiAction action) {
    UserTokenOpenApiAction result = new UserTokenOpenApiAction(action.getId(), action.getMethod(),
        action.getPath(), action.getRequiredOperations(), action.getOperationMatch(),
        action.getResourceScope(), action.getDescription());
    result.setGrantedOperations(grantedOperationsFor(scope, action.getRequiredOperations()));
    return result;
  }

  private List<String> grantedOperationsFor(UserTokenScope scope, List<String> requiredOperations) {
    if (requiredOperations == null || requiredOperations.isEmpty()) {
      return NO_REQUIRED_OPERATIONS;
    }
    return requiredOperations.stream().filter(scope::allowsOperation).collect(Collectors.toList());
  }

  private static UserTokenOpenApiAction action(String id, String method, String path,
      String requiredOperation, String resourceScope, String description) {
    return action(id, method, path, Collections.singletonList(requiredOperation), resourceScope,
        description);
  }

  private static UserTokenOpenApiAction action(String id, String method, String path,
      List<String> requiredOperations, String resourceScope, String description) {
    List<String> safeRequiredOperations =
        requiredOperations == null ? NO_REQUIRED_OPERATIONS : requiredOperations;
    return new UserTokenOpenApiAction(id, method, path, safeRequiredOperations,
        safeRequiredOperations.isEmpty() ? OPERATION_MATCH_NONE : OPERATION_MATCH_ANY,
        resourceScope, description);
  }
}
