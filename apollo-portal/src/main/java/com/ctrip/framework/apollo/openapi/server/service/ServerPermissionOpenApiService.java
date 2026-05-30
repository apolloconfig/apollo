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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.openapi.model.OpenAppRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenClusterNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenPermissionConditionDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.constant.RoleType;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.vo.AppRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.ClusterNamespaceRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceEnvRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.PermissionCondition;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Default server-side OpenAPI implementation for permission and role management.
 */
@Service
public class ServerPermissionOpenApiService implements PermissionOpenApiService {

  private static final String HAS_CREATE_APPLICATION_PERMISSION = "hasCreateApplicationPermission";
  private static final String IS_MANAGE_APP_MASTER_PERMISSION_ENABLED =
      "isManageAppMasterPermissionEnabled";

  private final RolePermissionService rolePermissionService;
  private final UserService userService;
  private final RoleInitializationService roleInitializationService;
  private final SystemRoleManagerService systemRoleManagerService;

  public ServerPermissionOpenApiService(RolePermissionService rolePermissionService,
      UserService userService, RoleInitializationService roleInitializationService,
      SystemRoleManagerService systemRoleManagerService) {
    this.rolePermissionService = rolePermissionService;
    this.userService = userService;
    this.roleInitializationService = roleInitializationService;
    this.systemRoleManagerService = systemRoleManagerService;
  }

  @Override
  public void initAppPermission(String appId, String namespaceName, String operator) {
    roleInitializationService.initNamespaceEnvRoles(appId, namespaceName, operator);
  }

  @Override
  public void initClusterNamespacePermission(String appId, String env, String clusterName,
      String operator) {
    roleInitializationService.initClusterNamespaceRoles(appId, normalizeEnv(env), clusterName,
        operator);
  }

  @Override
  public OpenPermissionConditionDTO hasAppPermission(String appId, String permissionType,
      String userId) {
    return permission(rolePermissionService.userHasPermission(userId, permissionType, appId));
  }

  @Override
  public OpenPermissionConditionDTO hasNamespacePermission(String appId, String namespaceName,
      String permissionType, String userId) {
    return permission(rolePermissionService.userHasPermission(userId, permissionType,
        RoleUtils.buildNamespaceTargetId(appId, namespaceName)));
  }

  @Override
  public OpenPermissionConditionDTO hasEnvNamespacePermission(String appId, String env,
      String namespaceName, String permissionType, String userId) {
    return permission(rolePermissionService.userHasPermission(userId, permissionType,
        RoleUtils.buildNamespaceTargetId(appId, namespaceName, normalizeEnv(env))));
  }

  @Override
  public OpenPermissionConditionDTO hasClusterNamespacePermission(String appId, String env,
      String clusterName, String permissionType, String userId) {
    String normalizedEnv = normalizeEnv(env);
    return permission(rolePermissionService.userHasPermission(userId, permissionType,
        RoleUtils.buildClusterTargetId(appId, normalizedEnv, clusterName)));
  }

  @Override
  public OpenPermissionConditionDTO hasRootPermission(String userId) {
    return permission(rolePermissionService.isSuperAdmin(userId));
  }

  @Override
  public OpenEnvNamespaceRoleUserDTO getNamespaceEnvRoleUsers(String appId, String env,
      String namespaceName) {
    String normalizedEnv = normalizeEnv(env);
    NamespaceEnvRolesAssignedUsers assignedUsers = new NamespaceEnvRolesAssignedUsers();
    assignedUsers.setAppId(appId);
    assignedUsers.setNamespaceName(namespaceName);
    assignedUsers.setEnv(Env.valueOf(normalizedEnv));
    assignedUsers.setReleaseRoleUsers(rolePermissionService.queryUsersWithRole(
        RoleUtils.buildReleaseNamespaceRoleName(appId, namespaceName, normalizedEnv)));
    assignedUsers.setModifyRoleUsers(rolePermissionService.queryUsersWithRole(
        RoleUtils.buildModifyNamespaceRoleName(appId, namespaceName, normalizedEnv)));
    return OpenApiModelConverters.fromNamespaceEnvRolesAssignedUsers(assignedUsers);
  }

  @Override
  public void assignNamespaceEnvRoleToUser(String appId, String env, String namespaceName,
      String roleType, String userId, String operator) {
    assignRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType, normalizeEnv(env)),
        roleType, userId, operator);
  }

  @Override
  public void removeNamespaceEnvRoleFromUser(String appId, String env, String namespaceName,
      String roleType, String userId, String operator) {
    removeRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType, normalizeEnv(env)),
        roleType, userId, operator);
  }

  @Override
  public OpenClusterNamespaceRoleUserDTO getClusterNamespaceRoles(String appId, String env,
      String clusterName) {
    String normalizedEnv = normalizeEnv(env);
    ClusterNamespaceRolesAssignedUsers assignedUsers = new ClusterNamespaceRolesAssignedUsers();
    assignedUsers.setAppId(appId);
    assignedUsers.setEnv(normalizedEnv);
    assignedUsers.setCluster(clusterName);
    assignedUsers.setReleaseRoleUsers(rolePermissionService.queryUsersWithRole(
        RoleUtils.buildReleaseNamespacesInClusterRoleName(appId, normalizedEnv, clusterName)));
    assignedUsers.setModifyRoleUsers(rolePermissionService.queryUsersWithRole(
        RoleUtils.buildModifyNamespacesInClusterRoleName(appId, normalizedEnv, clusterName)));
    return OpenApiModelConverters.fromClusterNamespaceRolesAssignedUsers(assignedUsers);
  }

  @Override
  public void assignClusterNamespaceRoleToUser(String appId, String env, String clusterName,
      String roleType, String userId, String operator) {
    assignRole(RoleUtils.buildClusterRoleName(appId, normalizeEnv(env), clusterName, roleType),
        roleType, userId, operator);
  }

  @Override
  public void removeClusterNamespaceRoleFromUser(String appId, String env, String clusterName,
      String roleType, String userId, String operator) {
    removeRole(RoleUtils.buildClusterRoleName(appId, normalizeEnv(env), clusterName, roleType),
        roleType, userId, operator);
  }

  @Override
  public OpenNamespaceRoleUserDTO getNamespaceRoles(String appId, String namespaceName) {
    NamespaceRolesAssignedUsers assignedUsers = new NamespaceRolesAssignedUsers();
    assignedUsers.setAppId(appId);
    assignedUsers.setNamespaceName(namespaceName);
    assignedUsers.setReleaseRoleUsers(rolePermissionService
        .queryUsersWithRole(RoleUtils.buildReleaseNamespaceRoleName(appId, namespaceName)));
    assignedUsers.setModifyRoleUsers(rolePermissionService
        .queryUsersWithRole(RoleUtils.buildModifyNamespaceRoleName(appId, namespaceName)));
    return OpenApiModelConverters.fromNamespaceRolesAssignedUsers(assignedUsers);
  }

  @Override
  public void assignNamespaceRoleToUser(String appId, String namespaceName, String roleType,
      String userId, String operator) {
    assignRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType), roleType, userId,
        operator);
  }

  @Override
  public void removeNamespaceRoleFromUser(String appId, String namespaceName, String roleType,
      String userId, String operator) {
    removeRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType), roleType, userId,
        operator);
  }

  @Override
  public OpenAppRoleUserDTO getAppRoles(String appId) {
    AppRolesAssignedUsers assignedUsers = new AppRolesAssignedUsers();
    assignedUsers.setAppId(appId);
    assignedUsers.setMasterUsers(
        rolePermissionService.queryUsersWithRole(RoleUtils.buildAppMasterRoleName(appId)));
    return OpenApiModelConverters.fromAppRolesAssignedUsers(assignedUsers);
  }

  @Override
  public void assignAppRoleToUser(String appId, String roleType, String userId, String operator) {
    assignRole(RoleUtils.buildAppRoleName(appId, roleType), roleType, userId, operator);
  }

  @Override
  public void removeAppRoleFromUser(String appId, String roleType, String userId, String operator) {
    removeRole(RoleUtils.buildAppRoleName(appId, roleType), roleType, userId, operator);
  }

  @Override
  public Map<String, Boolean> hasCreateApplicationPermission(String userId) {
    return Collections.singletonMap(HAS_CREATE_APPLICATION_PERMISSION,
        systemRoleManagerService.hasCreateApplicationPermission(userId));
  }

  @Override
  public void addCreateApplicationRoleToUsers(List<String> userIds, String operator) {
    if (CollectionUtils.isEmpty(userIds)) {
      throw new BadRequestException("userIds should not be null or empty");
    }
    userIds.forEach(this::checkUserExists);
    rolePermissionService.assignRoleToUsers(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME,
        new HashSet<>(userIds), operator);
  }

  @Override
  public void deleteCreateApplicationRoleFromUser(String userId, String operator) {
    RequestPrecondition.checkArgumentsNotEmpty(userId);
    checkUserExists(userId);
    rolePermissionService.removeRoleFromUsers(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME,
        Sets.newHashSet(userId), operator);
  }

  @Override
  public List<String> getCreateApplicationRoleUsers() {
    Set<UserInfo> users = rolePermissionService
        .queryUsersWithRole(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME);
    if (CollectionUtils.isEmpty(users)) {
      return Collections.emptyList();
    }
    return users.stream().map(UserInfo::getUserId).sorted().collect(Collectors.toList());
  }

  @Override
  public void addManageAppMasterRoleToUser(String appId, String userId, String operator) {
    checkUserExists(userId);
    roleInitializationService.initManageAppMasterRole(appId, operator);
    rolePermissionService.assignRoleToUsers(
        RoleUtils.buildAppRoleName(appId, PermissionType.MANAGE_APP_MASTER),
        Sets.newHashSet(userId), operator);
  }

  @Override
  public void removeManageAppMasterRoleFromUser(String appId, String userId, String operator) {
    RequestPrecondition.checkArgumentsNotEmpty(userId);
    checkUserExists(userId);
    roleInitializationService.initManageAppMasterRole(appId, operator);
    rolePermissionService.removeRoleFromUsers(
        RoleUtils.buildAppRoleName(appId, PermissionType.MANAGE_APP_MASTER),
        Sets.newHashSet(userId), operator);
  }

  @Override
  public Map<String, Boolean> isManageAppMasterPermissionEnabled() {
    return Collections.singletonMap(IS_MANAGE_APP_MASTER_PERMISSION_ENABLED,
        systemRoleManagerService.isManageAppMasterPermissionEnabled());
  }

  private OpenPermissionConditionDTO permission(boolean hasPermission) {
    PermissionCondition permissionCondition = new PermissionCondition();
    permissionCondition.setHasPermission(hasPermission);
    return OpenApiModelConverters.fromPermissionCondition(permissionCondition);
  }

  private void assignRole(String roleName, String roleType, String userId, String operator) {
    validateRoleAssignment(roleType, userId);
    Set<String> assignedUsers =
        rolePermissionService.assignRoleToUsers(roleName, Sets.newHashSet(userId), operator);
    if (CollectionUtils.isEmpty(assignedUsers)) {
      throw BadRequestException.userAlreadyAuthorized(userId);
    }
  }

  private void removeRole(String roleName, String roleType, String userId, String operator) {
    validateRoleRemoval(roleType, userId);
    rolePermissionService.removeRoleFromUsers(roleName, Sets.newHashSet(userId), operator);
  }

  private void validateRoleAssignment(String roleType, String userId) {
    validateRoleRemoval(roleType, userId);
    checkUserExists(userId);
  }

  private void validateRoleRemoval(String roleType, String userId) {
    RequestPrecondition.checkArgumentsNotEmpty(userId);
    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
  }

  private void checkUserExists(String userId) {
    if (userService.findByUserId(userId) == null) {
      throw BadRequestException.userNotExists(userId);
    }
  }

  private String normalizeEnv(String env) {
    Env transformedEnv = Env.transformEnv(env);
    if (Env.UNKNOWN == transformedEnv) {
      throw BadRequestException.invalidEnvFormat(env);
    }
    return transformedEnv.getName();
  }
}
