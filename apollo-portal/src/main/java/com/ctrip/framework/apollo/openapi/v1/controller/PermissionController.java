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

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.openapi.api.PermissionManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenAppRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenClusterNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenPermissionConditionDTO;
import com.ctrip.framework.apollo.openapi.server.service.PermissionOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI v1 controller for permission checks and role management.
 */
@RestController("openapiPermissionController")
public class PermissionController implements PermissionManagementApi {

  private final PermissionOpenApiService permissionOpenApiService;
  private final OpenApiOperatorResolver operatorResolver;
  private final UnifiedPermissionValidator unifiedPermissionValidator;

  public PermissionController(PermissionOpenApiService permissionOpenApiService,
      OpenApiOperatorResolver operatorResolver,
      UnifiedPermissionValidator unifiedPermissionValidator) {
    this.permissionOpenApiService = permissionOpenApiService;
    this.operatorResolver = operatorResolver;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.addCreateApplicationRoleToUser")
  public ResponseEntity<Void> addCreateApplicationRoleToUsers(String operator,
      List<String> requestBody) {
    permissionOpenApiService.addCreateApplicationRoleToUsers(requestBody,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.addManageAppMasterRoleToUser")
  public ResponseEntity<Void> addManageAppMasterRoleToUser(String appId, String userId,
      String operator) {
    permissionOpenApiService.addManageAppMasterRoleToUser(appId, userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasManageAppMasterPermission(#appId)")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignAppRoleToUser")
  public ResponseEntity<Void> assignAppRoleToUser(String appId, String roleType, String userId,
      String operator, String body) {
    permissionOpenApiService.assignAppRoleToUser(appId, roleType, userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasAssignRolePermission(#appId)")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignClusterNamespaceRoleToUser")
  public ResponseEntity<Void> assignClusterNamespaceRoleToUser(String appId, String env,
      String clusterName, String roleType, String userId, String operator) {
    permissionOpenApiService.assignClusterNamespaceRoleToUser(appId, env, clusterName, roleType,
        userId, operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasAssignRolePermission(#appId)")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignNamespaceEnvRoleToUser")
  public ResponseEntity<Void> assignNamespaceEnvRoleToUser(String appId, String env,
      String namespaceName, String roleType, String userId, String operator) {
    permissionOpenApiService.assignNamespaceEnvRoleToUser(appId, env, namespaceName, roleType,
        userId, operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasAssignRolePermission(#appId)")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignNamespaceRoleToUser")
  public ResponseEntity<Void> assignNamespaceRoleToUser(String appId, String namespaceName,
      String roleType, String userId, String operator) {
    permissionOpenApiService.assignNamespaceRoleToUser(appId, namespaceName, roleType, userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.deleteCreateApplicationRoleFromUser")
  public ResponseEntity<Void> deleteCreateApplicationRoleFromUser(String userId, String operator) {
    permissionOpenApiService.deleteCreateApplicationRoleFromUser(userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<OpenAppRoleUserDTO> getAppRoles(String appId) {
    return ResponseEntity.ok(permissionOpenApiService.getAppRoles(appId));
  }

  @Override
  public ResponseEntity<OpenClusterNamespaceRoleUserDTO> getClusterNamespaceRoles(String appId,
      String env, String clusterName) {
    return ResponseEntity
        .ok(permissionOpenApiService.getClusterNamespaceRoles(appId, env, clusterName));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public ResponseEntity<List<String>> getCreateApplicationRoleUsers() {
    return ResponseEntity.ok(permissionOpenApiService.getCreateApplicationRoleUsers());
  }

  @Override
  public ResponseEntity<OpenEnvNamespaceRoleUserDTO> getNamespaceEnvRoleUsers(String appId,
      String env, String namespaceName) {
    return ResponseEntity
        .ok(permissionOpenApiService.getNamespaceEnvRoleUsers(appId, env, namespaceName));
  }

  @Override
  public ResponseEntity<OpenNamespaceRoleUserDTO> getNamespaceRoles(String appId,
      String namespaceName) {
    return ResponseEntity.ok(permissionOpenApiService.getNamespaceRoles(appId, namespaceName));
  }

  @Override
  public ResponseEntity<OpenPermissionConditionDTO> hasAppPermission(String appId,
      String permissionType, String userId) {
    return ResponseEntity
        .ok(permissionOpenApiService.hasAppPermission(appId, permissionType, userId));
  }

  @Override
  public ResponseEntity<OpenPermissionConditionDTO> hasClusterNamespacePermission(String appId,
      String env, String clusterName, String permissionType, String userId) {
    return ResponseEntity.ok(permissionOpenApiService.hasClusterNamespacePermission(appId, env,
        clusterName, permissionType, userId));
  }

  @Override
  public ResponseEntity<Map<String, Boolean>> hasCreateApplicationPermission(String userId) {
    return ResponseEntity.ok(permissionOpenApiService.hasCreateApplicationPermission(userId));
  }

  @Override
  public ResponseEntity<OpenPermissionConditionDTO> hasEnvNamespacePermission(String appId,
      String env, String namespaceName, String permissionType, String userId) {
    return ResponseEntity.ok(permissionOpenApiService.hasEnvNamespacePermission(appId, env,
        namespaceName, permissionType, userId));
  }

  @Override
  public ResponseEntity<OpenPermissionConditionDTO> hasNamespacePermission(String appId,
      String namespaceName, String permissionType, String userId) {
    return ResponseEntity.ok(permissionOpenApiService.hasNamespacePermission(appId, namespaceName,
        permissionType, userId));
  }

  @Override
  public ResponseEntity<OpenPermissionConditionDTO> hasRootPermission(String userId) {
    return ResponseEntity.ok(permissionOpenApiService.hasRootPermission(userId));
  }

  @Override
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.initAppPermission")
  public ResponseEntity<Void> initAppPermission(String appId, String namespaceName,
      String operator) {
    requirePortalUserOrAssignRolePermission(appId);
    permissionOpenApiService.initAppPermission(appId, namespaceName,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.initClusterNamespacePermission")
  public ResponseEntity<Void> initClusterNamespacePermission(String appId, String env,
      String clusterName, String operator) {
    requirePortalUserOrAssignRolePermission(appId);
    permissionOpenApiService.initClusterNamespacePermission(appId, env, clusterName,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Map<String, Boolean>> isManageAppMasterPermissionEnabled() {
    return ResponseEntity.ok(permissionOpenApiService.isManageAppMasterPermissionEnabled());
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasManageAppMasterPermission(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeAppRoleFromUser")
  public ResponseEntity<Void> removeAppRoleFromUser(String appId, String roleType, String userId,
      String operator) {
    permissionOpenApiService.removeAppRoleFromUser(appId, roleType, userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasAssignRolePermission(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeClusterNamespaceRoleFromUser")
  public ResponseEntity<Void> removeClusterNamespaceRoleFromUser(String appId, String env,
      String clusterName, String roleType, String userId, String operator) {
    permissionOpenApiService.removeClusterNamespaceRoleFromUser(appId, env, clusterName, roleType,
        userId, operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.forbidManageAppMaster")
  public ResponseEntity<Void> removeManageAppMasterRoleFromUser(String appId, String userId,
      String operator) {
    permissionOpenApiService.removeManageAppMasterRoleFromUser(appId, userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasAssignRolePermission(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeNamespaceEnvRoleFromUser")
  public ResponseEntity<Void> removeNamespaceEnvRoleFromUser(String appId, String env,
      String namespaceName, String roleType, String userId, String operator) {
    permissionOpenApiService.removeNamespaceEnvRoleFromUser(appId, env, namespaceName, roleType,
        userId, operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasAssignRolePermission(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeNamespaceRoleFromUser")
  public ResponseEntity<Void> removeNamespaceRoleFromUser(String appId, String namespaceName,
      String roleType, String userId, String operator) {
    permissionOpenApiService.removeNamespaceRoleFromUser(appId, namespaceName, roleType, userId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  /**
   * The permission-init operations are idempotent bootstrap hooks used by Portal UI, and the
   * legacy WebAPI routes did not require assign-role permission from session users. Keep that
   * behavior for Portal USER requests so role records can be initialized during app or namespace
   * setup before normal role assignment.
   *
   * <p>OpenAPI CONSUMER token requests still use the public role-management surface, so they must
   * prove app-scoped ASSIGN_ROLE permission and then provide a valid explicit operator through
   * {@link OpenApiOperatorResolver}.
   */
  private void requirePortalUserOrAssignRolePermission(String appId) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(authType)) {
      return;
    }
    if (UserIdentityConstants.CONSUMER.equals(authType)
        && unifiedPermissionValidator.hasAssignRolePermission(appId)) {
      return;
    }
    throw new AccessDeniedException("Assign role permission is required");
  }
}
