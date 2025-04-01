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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
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
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@RestController
public class PermissionController {

  private final UserInfoHolder userInfoHolder;
  private final RolePermissionService rolePermissionService;
  private final UserService userService;
  private final RoleInitializationService roleInitializationService;
  private final SystemRoleManagerService systemRoleManagerService;
  private final UserPermissionValidator userPermissionValidator;

  public PermissionController(
          final UserInfoHolder userInfoHolder,
          final RolePermissionService rolePermissionService,
          final UserService userService,
          final RoleInitializationService roleInitializationService,
          final SystemRoleManagerService systemRoleManagerService,
          final UserPermissionValidator userPermissionValidator) {
    this.userInfoHolder = userInfoHolder;
    this.rolePermissionService = rolePermissionService;
    this.userService = userService;
    this.roleInitializationService = roleInitializationService;
    this.systemRoleManagerService = systemRoleManagerService;
    this.userPermissionValidator = userPermissionValidator;
  }

  @PostMapping("/apps/{appId}/initPermission")
  public ResponseEntity<Void> initAppPermission(@PathVariable String appId, @RequestBody String namespaceName) {
    roleInitializationService.initNamespaceEnvRoles(appId, namespaceName, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/initNsPermission")
  public ResponseEntity<Void> initClusterNamespacePermission(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName) {
    roleInitializationService.initClusterNamespaceRoles(appId, env, clusterName, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/apps/{appId}/permissions/{permissionType}")
  public ResponseEntity<PermissionCondition> hasPermission(@PathVariable String appId, @PathVariable String permissionType) {
    PermissionCondition permissionCondition = new PermissionCondition();

    permissionCondition.setHasPermission(
        rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(), permissionType, appId));

    return ResponseEntity.ok().body(permissionCondition);
  }

  @GetMapping("/apps/{appId}/namespaces/{namespaceName}/permissions/{permissionType}")
  public ResponseEntity<PermissionCondition> hasPermission(@PathVariable String appId, @PathVariable String namespaceName,
                                                           @PathVariable String permissionType) {
    PermissionCondition permissionCondition = new PermissionCondition();

    permissionCondition.setHasPermission(
        rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(), permissionType,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName)));

    return ResponseEntity.ok().body(permissionCondition);
  }

  @GetMapping("/apps/{appId}/envs/{env}/namespaces/{namespaceName}/permissions/{permissionType}")
  public ResponseEntity<PermissionCondition> hasPermission(@PathVariable String appId, @PathVariable String env, @PathVariable String namespaceName,
                                                           @PathVariable String permissionType) {
    PermissionCondition permissionCondition = new PermissionCondition();

    permissionCondition.setHasPermission(
        rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(), permissionType,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env)));

    return ResponseEntity.ok().body(permissionCondition);
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_permissions/{permissionType}")
  public ResponseEntity<PermissionCondition> hasClusterNamespacePermission(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
                                                           @PathVariable String permissionType) {
    PermissionCondition permissionCondition = new PermissionCondition();

    permissionCondition.setHasPermission(
        rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(), permissionType,
            RoleUtils.buildClusterTargetId(appId, env, clusterName)));

    return ResponseEntity.ok().body(permissionCondition);
  }

  @GetMapping("/permissions/root")
  public ResponseEntity<PermissionCondition> hasRootPermission() {
    PermissionCondition permissionCondition = new PermissionCondition();

    permissionCondition.setHasPermission(rolePermissionService.isSuperAdmin(userInfoHolder.getUser().getUserId()));

    return ResponseEntity.ok().body(permissionCondition);
  }


  @GetMapping("/apps/{appId}/envs/{env}/namespaces/{namespaceName}/role_users")
  public NamespaceEnvRolesAssignedUsers getNamespaceEnvRoles(@PathVariable String appId, @PathVariable String env, @PathVariable String namespaceName) {

    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw BadRequestException.invalidEnvFormat(env);
    }

    NamespaceEnvRolesAssignedUsers assignedUsers = new NamespaceEnvRolesAssignedUsers();
    assignedUsers.setNamespaceName(namespaceName);
    assignedUsers.setAppId(appId);
    assignedUsers.setEnv(Env.valueOf(env));

    Set<UserInfo> releaseNamespaceUsers =
        rolePermissionService.queryUsersWithRole(RoleUtils.buildReleaseNamespaceRoleName(appId, namespaceName, env));
    assignedUsers.setReleaseRoleUsers(releaseNamespaceUsers);

    Set<UserInfo> modifyNamespaceUsers =
        rolePermissionService.queryUsersWithRole(RoleUtils.buildModifyNamespaceRoleName(appId, namespaceName, env));
    assignedUsers.setModifyRoleUsers(modifyNamespaceUsers);

    return assignedUsers;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasAssignRolePermission(#appId)")
  @PostMapping("/apps/{appId}/envs/{env}/namespaces/{namespaceName}/roles/{roleType}")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignNamespaceEnvRoleToUser")
  public ResponseEntity<Void> assignNamespaceEnvRoleToUser(@PathVariable String appId, @PathVariable String env, @PathVariable String namespaceName,
                                                           @PathVariable String roleType, @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }

    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw BadRequestException.invalidEnvFormat(env);
    }
    Set<String> assignedUser = rolePermissionService.assignRoleToUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType, env),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUser)) {
      throw BadRequestException.userAlreadyAuthorized(user);
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.hasAssignRolePermission(#appId)")
  @DeleteMapping("/apps/{appId}/envs/{env}/namespaces/{namespaceName}/roles/{roleType}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeNamespaceEnvRoleFromUser")
  public ResponseEntity<Void> removeNamespaceEnvRoleFromUser(@PathVariable String appId, @PathVariable String env, @PathVariable String namespaceName,
                                                             @PathVariable String roleType, @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw BadRequestException.invalidEnvFormat(env);
    }
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType, env),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_role_users")
  public ClusterNamespaceRolesAssignedUsers getClusterNamespaceRoles(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName) {

    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw BadRequestException.invalidEnvFormat(env);
    }

    ClusterNamespaceRolesAssignedUsers assignedUsers = new ClusterNamespaceRolesAssignedUsers();
    assignedUsers.setAppId(appId);
    assignedUsers.setEnv(env);
    assignedUsers.setCluster(clusterName);

    Set<UserInfo> releaseNamespacesInClusterUsers =
        rolePermissionService.queryUsersWithRole(RoleUtils.buildReleaseNamespacesInClusterRoleName(appId, env, clusterName));
    assignedUsers.setReleaseRoleUsers(releaseNamespacesInClusterUsers);

    Set<UserInfo> modifyNamespacesInClusterUsers =
        rolePermissionService.queryUsersWithRole(RoleUtils.buildModifyNamespacesInClusterRoleName(appId, env, clusterName));
    assignedUsers.setModifyRoleUsers(modifyNamespacesInClusterUsers);

    return assignedUsers;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasAssignRolePermission(#appId)")
  @PostMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}")
  public ResponseEntity<Void> assignClusterNamespaceRoleToUser(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
      @PathVariable String roleType, @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }

    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw BadRequestException.invalidEnvFormat(env);
    }
    Set<String> assignedUser = rolePermissionService.assignRoleToUsers(RoleUtils.buildClusterRoleName(appId, env, clusterName, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUser)) {
      throw BadRequestException.userAlreadyAuthorized(user);
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.hasAssignRolePermission(#appId)")
  @DeleteMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}")
  public ResponseEntity<Void> removeClusterNamespaceRoleFromUser(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
      @PathVariable String roleType, @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw BadRequestException.invalidEnvFormat(env);
    }
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildClusterRoleName(appId, env, clusterName, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/apps/{appId}/namespaces/{namespaceName}/role_users")
  public NamespaceRolesAssignedUsers getNamespaceRoles(@PathVariable String appId, @PathVariable String namespaceName) {

    NamespaceRolesAssignedUsers assignedUsers = new NamespaceRolesAssignedUsers();
    assignedUsers.setNamespaceName(namespaceName);
    assignedUsers.setAppId(appId);

    Set<UserInfo> releaseNamespaceUsers =
        rolePermissionService.queryUsersWithRole(RoleUtils.buildReleaseNamespaceRoleName(appId, namespaceName));
    assignedUsers.setReleaseRoleUsers(releaseNamespaceUsers);

    Set<UserInfo> modifyNamespaceUsers =
        rolePermissionService.queryUsersWithRole(RoleUtils.buildModifyNamespaceRoleName(appId, namespaceName));
    assignedUsers.setModifyRoleUsers(modifyNamespaceUsers);

    return assignedUsers;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasAssignRolePermission(#appId)")
  @PostMapping("/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignNamespaceRoleToUser")
  public ResponseEntity<Void> assignNamespaceRoleToUser(@PathVariable String appId, @PathVariable String namespaceName,
                                                        @PathVariable String roleType, @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
    Set<String> assignedUser = rolePermissionService.assignRoleToUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUser)) {
      throw BadRequestException.userAlreadyAuthorized(user);
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.hasAssignRolePermission(#appId)")
  @DeleteMapping("/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeNamespaceRoleFromUser")
  public ResponseEntity<Void> removeNamespaceRoleFromUser(@PathVariable String appId, @PathVariable String namespaceName,
                                                          @PathVariable String roleType, @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/apps/{appId}/role_users")
  public AppRolesAssignedUsers getAppRoles(@PathVariable String appId) {
    AppRolesAssignedUsers users = new AppRolesAssignedUsers();
    users.setAppId(appId);

    Set<UserInfo> masterUsers = rolePermissionService.queryUsersWithRole(RoleUtils.buildAppMasterRoleName(appId));
    users.setMasterUsers(masterUsers);

    return users;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasManageAppMasterPermission(#appId)")
  @PostMapping("/apps/{appId}/roles/{roleType}")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignAppRoleToUser")
  public ResponseEntity<Void> assignAppRoleToUser(@PathVariable String appId, @PathVariable String roleType,
                                                  @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
    Set<String> assignedUsers = rolePermissionService.assignRoleToUsers(RoleUtils.buildAppRoleName(appId, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUsers)) {
      throw BadRequestException.userAlreadyAuthorized(user);
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.hasManageAppMasterPermission(#appId)")
  @DeleteMapping("/apps/{appId}/roles/{roleType}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeAppRoleFromUser")
  public ResponseEntity<Void> removeAppRoleFromUser(@PathVariable String appId, @PathVariable String roleType,
                                                    @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw BadRequestException.invalidRoleTypeFormat(roleType);
    }
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildAppRoleName(appId, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  private void checkUserExists(String userId) {
    if (userService.findByUserId(userId) == null) {
      throw BadRequestException.userNotExists(userId);
    }
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @PostMapping("/system/role/createApplication")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.addCreateApplicationRoleToUser")
  public ResponseEntity<Void> addCreateApplicationRoleToUser(@RequestBody List<String> userIds) {

    userIds.forEach(this::checkUserExists);
    rolePermissionService.assignRoleToUsers(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME,
            new HashSet<>(userIds), userInfoHolder.getUser().getUserId());

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @DeleteMapping("/system/role/createApplication/{userId}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.deleteCreateApplicationRoleFromUser")
  public ResponseEntity<Void> deleteCreateApplicationRoleFromUser(@PathVariable("userId") String userId) {
    checkUserExists(userId);
    Set<String> userIds = new HashSet<>();
    userIds.add(userId);
    rolePermissionService.removeRoleFromUsers(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME,
            userIds, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @GetMapping("/system/role/createApplication")
  public List<String> getCreateApplicationRoleUsers() {
    return rolePermissionService.queryUsersWithRole(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME)
            .stream().map(UserInfo::getUserId).collect(Collectors.toList());
  }

  @GetMapping("/system/role/createApplication/{userId}")
  public JsonObject hasCreateApplicationPermission(@PathVariable String userId) {
    JsonObject rs = new JsonObject();
    rs.addProperty("hasCreateApplicationPermission", userPermissionValidator.hasCreateApplicationPermission(userId));
    return rs;
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @PostMapping("/apps/{appId}/system/master/{userId}")
  @ApolloAuditLog(type = OpType.CREATE, name = "Auth.addManageAppMasterRoleToUser")
  public ResponseEntity<Void> addManageAppMasterRoleToUser(@PathVariable String appId, @PathVariable String userId) {
    checkUserExists(userId);
    roleInitializationService.initManageAppMasterRole(appId, userInfoHolder.getUser().getUserId());
    Set<String> userIds = new HashSet<>();
    userIds.add(userId);
    rolePermissionService.assignRoleToUsers(RoleUtils.buildAppRoleName(appId, PermissionType.MANAGE_APP_MASTER),
            userIds, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @DeleteMapping("/apps/{appId}/system/master/{userId}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Auth.forbidManageAppMaster")
  public ResponseEntity<Void> forbidManageAppMaster(@PathVariable String appId, @PathVariable String  userId) {
    checkUserExists(userId);
    roleInitializationService.initManageAppMasterRole(appId, userInfoHolder.getUser().getUserId());
    Set<String> userIds = new HashSet<>();
    userIds.add(userId);
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildAppRoleName(appId, PermissionType.MANAGE_APP_MASTER),
            userIds, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

    @GetMapping("/system/role/manageAppMaster")
    public JsonObject isManageAppMasterPermissionEnabled() {
      JsonObject rs = new JsonObject();
      rs.addProperty("isManageAppMasterPermissionEnabled", systemRoleManagerService.isManageAppMasterPermissionEnabled());
      return rs;
    }
}
