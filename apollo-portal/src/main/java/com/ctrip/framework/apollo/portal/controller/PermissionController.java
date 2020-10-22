package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.component.PermissionValidator;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.constant.RoleType;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.vo.AppRolesAssignedUsers;
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
import org.springframework.beans.factory.annotation.Autowired;
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
  private final PermissionValidator permissionValidator;

  @Autowired
  public PermissionController(
          final UserInfoHolder userInfoHolder,
          final RolePermissionService rolePermissionService,
          final UserService userService,
          final RoleInitializationService roleInitializationService,
          final SystemRoleManagerService systemRoleManagerService,
          final PermissionValidator permissionValidator) {
    this.userInfoHolder = userInfoHolder;
    this.rolePermissionService = rolePermissionService;
    this.userService = userService;
    this.roleInitializationService = roleInitializationService;
    this.systemRoleManagerService = systemRoleManagerService;
    this.permissionValidator = permissionValidator;
  }

  @PostMapping("/apps/{appId}/initPermission")
  public ResponseEntity<Void> initAppPermission(@PathVariable String appId, @RequestBody String namespaceName) {
    roleInitializationService.initNamespaceEnvRoles(appId, namespaceName, userInfoHolder.getUser().getUserId());
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
      throw new BadRequestException("env is illegal");
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

  @PreAuthorize(value = "@permissionValidator.hasAssignRolePermission(#appId)")
  @PostMapping("/apps/{appId}/envs/{env}/namespaces/{namespaceName}/roles/{roleType}")
  public ResponseEntity<Void> assignNamespaceEnvRoleToUser(@PathVariable String appId, @PathVariable String env, @PathVariable String namespaceName,
                                                           @PathVariable String roleType, @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw new BadRequestException("role type is illegal");
    }

    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw new BadRequestException("env is illegal");
    }
    Set<String> assignedUser = rolePermissionService.assignRoleToUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType, env),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUser)) {
      throw new BadRequestException(user + " already authorized");
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@permissionValidator.hasAssignRolePermission(#appId)")
  @DeleteMapping("/apps/{appId}/envs/{env}/namespaces/{namespaceName}/roles/{roleType}")
  public ResponseEntity<Void> removeNamespaceEnvRoleFromUser(@PathVariable String appId, @PathVariable String env, @PathVariable String namespaceName,
                                                             @PathVariable String roleType, @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw new BadRequestException("role type is illegal");
    }
    // validate env parameter
    if (Env.UNKNOWN == Env.transformEnv(env)) {
      throw new BadRequestException("env is illegal");
    }
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType, env),
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

  @PreAuthorize(value = "@permissionValidator.hasAssignRolePermission(#appId)")
  @PostMapping("/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}")
  public ResponseEntity<Void> assignNamespaceRoleToUser(@PathVariable String appId, @PathVariable String namespaceName,
                                                        @PathVariable String roleType, @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw new BadRequestException("role type is illegal");
    }
    Set<String> assignedUser = rolePermissionService.assignRoleToUsers(RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUser)) {
      throw new BadRequestException(user + " already authorized");
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@permissionValidator.hasAssignRolePermission(#appId)")
  @DeleteMapping("/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}")
  public ResponseEntity<Void> removeNamespaceRoleFromUser(@PathVariable String appId, @PathVariable String namespaceName,
                                                          @PathVariable String roleType, @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw new BadRequestException("role type is illegal");
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

  @PreAuthorize(value = "@permissionValidator.hasManageAppMasterPermission(#appId)")
  @PostMapping("/apps/{appId}/roles/{roleType}")
  public ResponseEntity<Void> assignAppRoleToUser(@PathVariable String appId, @PathVariable String roleType,
                                                  @RequestBody String user) {
    checkUserExists(user);
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw new BadRequestException("role type is illegal");
    }
    Set<String> assignedUsers = rolePermissionService.assignRoleToUsers(RoleUtils.buildAppRoleName(appId, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    if (CollectionUtils.isEmpty(assignedUsers)) {
      throw new BadRequestException(user + " already authorized");
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@permissionValidator.hasManageAppMasterPermission(#appId)")
  @DeleteMapping("/apps/{appId}/roles/{roleType}")
  public ResponseEntity<Void> removeAppRoleFromUser(@PathVariable String appId, @PathVariable String roleType,
                                                    @RequestParam String user) {
    RequestPrecondition.checkArgumentsNotEmpty(user);

    if (!RoleType.isValidRoleType(roleType)) {
      throw new BadRequestException("role type is illegal");
    }
    rolePermissionService.removeRoleFromUsers(RoleUtils.buildAppRoleName(appId, roleType),
        Sets.newHashSet(user), userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  private void checkUserExists(String userId) {
    if (userService.findByUserId(userId) == null) {
      throw new BadRequestException(String.format("User %s does not exist!", userId));
    }
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @PostMapping("/system/role/createApplication")
  public ResponseEntity<Void> addCreateApplicationRoleToUser(@RequestBody List<String> userIds) {

    userIds.forEach(this::checkUserExists);
    rolePermissionService.assignRoleToUsers(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME,
            new HashSet<>(userIds), userInfoHolder.getUser().getUserId());

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @DeleteMapping("/system/role/createApplication/{userId}")
  public ResponseEntity<Void> deleteCreateApplicationRoleFromUser(@PathVariable("userId") String userId) {
    checkUserExists(userId);
    Set<String> userIds = new HashSet<>();
    userIds.add(userId);
    rolePermissionService.removeRoleFromUsers(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME,
            userIds, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @GetMapping("/system/role/createApplication")
  public List<String> getCreateApplicationRoleUsers() {
    return rolePermissionService.queryUsersWithRole(SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME)
            .stream().map(UserInfo::getUserId).collect(Collectors.toList());
  }

  @GetMapping("/system/role/createApplication/{userId}")
  public JsonObject hasCreateApplicationPermission(@PathVariable String userId) {
    JsonObject rs = new JsonObject();
    rs.addProperty("hasCreateApplicationPermission", permissionValidator.hasCreateApplicationPermission(userId));
    return rs;
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @PostMapping("/apps/{appId}/system/master/{userId}")
  public ResponseEntity<Void> addManageAppMasterRoleToUser(@PathVariable String appId, @PathVariable String userId) {
    checkUserExists(userId);
    roleInitializationService.initManageAppMasterRole(appId, userInfoHolder.getUser().getUserId());
    Set<String> userIds = new HashSet<>();
    userIds.add(userId);
    rolePermissionService.assignRoleToUsers(RoleUtils.buildAppRoleName(appId, PermissionType.MANAGE_APP_MASTER),
            userIds, userInfoHolder.getUser().getUserId());
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @DeleteMapping("/apps/{appId}/system/master/{userId}")
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
