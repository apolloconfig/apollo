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
package com.ctrip.framework.apollo.portal.spi.defaultimpl;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.po.RolePermission;
import com.ctrip.framework.apollo.portal.entity.po.UserRole;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RolePermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.repository.UserRoleRepository;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by timothy on 2017/4/26.
 */
public class DefaultRolePermissionService implements RolePermissionService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final PortalConfig portalConfig;
    private final ConsumerRoleRepository consumerRoleRepository;
    private final UserService userService;

    public DefaultRolePermissionService(final RoleRepository roleRepository,
        final RolePermissionRepository rolePermissionRepository,
        final UserRoleRepository userRoleRepository,
        final PermissionRepository permissionRepository,
        final PortalConfig portalConfig,
        final ConsumerRoleRepository consumerRoleRepository,
        final UserService userService) {
      this.roleRepository = roleRepository;
      this.rolePermissionRepository = rolePermissionRepository;
      this.userRoleRepository = userRoleRepository;
      this.permissionRepository = permissionRepository;
      this.portalConfig = portalConfig;
      this.consumerRoleRepository = consumerRoleRepository;
      this.userService = userService;
    }

    /**
     * Create role with permissions, note that role name should be unique
     */
    @Transactional
    @Override
    public Role createRoleWithPermissions(Role role, Set<Long> permissionIds) {
        Role current = findRoleByRoleName(role.getRoleName());
        Preconditions.checkState(current == null, "Role %s already exists!", role.getRoleName());

        Role createdRole = roleRepository.save(role);

        if (!CollectionUtils.isEmpty(permissionIds)) {
            Iterable<RolePermission> rolePermissions = permissionIds.stream().map(permissionId -> {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(createdRole.getId());
                rolePermission.setPermissionId(permissionId);
                rolePermission.setDataChangeCreatedBy(createdRole.getDataChangeCreatedBy());
                rolePermission.setDataChangeLastModifiedBy(createdRole.getDataChangeLastModifiedBy());
                return rolePermission;
            }).collect(Collectors.toList());
            rolePermissionRepository.saveAll(rolePermissions);
        }

        return createdRole;
    }

    /**
     * Assign role to users
     *
     * @return the users assigned roles
     */
    @Transactional
    @ApolloAuditLog(type = OpType.CREATE, name = "Auth.assignRoleToUsers")
    @Override
    public Set<String> assignRoleToUsers(String roleName, Set<String> userIds,
                                         String operatorUserId) {
        Role role = findRoleByRoleName(roleName);
        Preconditions.checkState(role != null, "Role %s doesn't exist!", roleName);

        List<UserRole> existedUserRoles =
                userRoleRepository.findByUserIdInAndRoleId(userIds, role.getId());
        Set<String> existedUserIds =
            existedUserRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());

        Set<String> toAssignUserIds = Sets.difference(userIds, existedUserIds);

        Iterable<UserRole> toCreate = toAssignUserIds.stream().map(userId -> {
            UserRole userRole = new UserRole();
            userRole.setRoleId(role.getId());
            userRole.setUserId(userId);
            userRole.setDataChangeCreatedBy(operatorUserId);
            userRole.setDataChangeLastModifiedBy(operatorUserId);
            return userRole;
        }).collect(Collectors.toList());

        userRoleRepository.saveAll(toCreate);
        return toAssignUserIds;
    }

    /**
     * Remove role from users
     */
    @Transactional
    @ApolloAuditLog(type = OpType.DELETE, name = "Auth.removeRoleFromUsers")
    @Override
    public void removeRoleFromUsers(
        @ApolloAuditLogDataInfluence
        @ApolloAuditLogDataInfluenceTable(tableName = "UserRole")
        @ApolloAuditLogDataInfluenceTableField(fieldName = "RoleName") String roleName,
        @ApolloAuditLogDataInfluence
        @ApolloAuditLogDataInfluenceTable(tableName = "UserRole")
        @ApolloAuditLogDataInfluenceTableField(fieldName = "UserId") Set<String> userIds, String operatorUserId) {
        Role role = findRoleByRoleName(roleName);
        Preconditions.checkState(role != null, "Role %s doesn't exist!", roleName);

        List<UserRole> existedUserRoles =
                userRoleRepository.findByUserIdInAndRoleId(userIds, role.getId());

        for (UserRole userRole : existedUserRoles) {
            userRole.setDeleted(true);
            userRole.setDataChangeLastModifiedTime(new Date());
            userRole.setDataChangeLastModifiedBy(operatorUserId);
        }

        userRoleRepository.saveAll(existedUserRoles);
    }

    /**
     * Query users with role
     */
    @Override
    public Set<UserInfo> queryUsersWithRole(String roleName) {
        Role role = findRoleByRoleName(roleName);

        if (role == null) {
            return Collections.emptySet();
        }

        List<UserRole> userRoles = userRoleRepository.findByRoleId(role.getId());
        List<UserInfo> userInfos = userService.findByUserIds(userRoles.stream().map(UserRole::getUserId).collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(userInfos)) {
            return Collections.emptySet();
        }

        return userInfos.stream()
            .sorted(Comparator.comparing(UserInfo::getUserId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Find role by role name, note that roleName should be unique
     */
    @Override
    public Role findRoleByRoleName(String roleName) {
        return roleRepository.findTopByRoleName(roleName);
    }

    /**
     * Check whether user has the permission
     */
    @Override
    public boolean userHasPermission(String userId, String permissionType, String targetId) {
        Permission permission =
                permissionRepository.findTopByPermissionTypeAndTargetId(permissionType, targetId);
        if (permission == null) {
            return false;
        }

        if (isSuperAdmin(userId)) {
            return true;
        }

        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            return false;
        }

        Set<Long> roleIds =
            userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleIdIn(roleIds);
        if (CollectionUtils.isEmpty(rolePermissions)) {
            return false;
        }

        for (RolePermission rolePermission : rolePermissions) {
            if (rolePermission.getPermissionId() == permission.getId()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Role> findUserRoles(String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }

        Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());

        return Lists.newLinkedList(roleRepository.findAllById(roleIds));
    }

    @Override
    public boolean isSuperAdmin(String userId) {
        return portalConfig.superAdmins().contains(userId);
    }

    /**
     * Create permission, note that permissionType + targetId should be unique
     */
    @Transactional
    @Override
    public Permission createPermission(Permission permission) {
        String permissionType = permission.getPermissionType();
        String targetId = permission.getTargetId();
        Permission current =
                permissionRepository.findTopByPermissionTypeAndTargetId(permissionType, targetId);
        Preconditions.checkState(current == null,
                "Permission with permissionType %s targetId %s already exists!", permissionType, targetId);

        return permissionRepository.save(permission);
    }

    /**
     * Create permissions, note that permissionType + targetId should be unique
     */
    @Transactional
    @Override
    public Set<Permission> createPermissions(Set<Permission> permissions) {
        Multimap<String, String> targetIdPermissionTypes = HashMultimap.create();
        for (Permission permission : permissions) {
            targetIdPermissionTypes.put(permission.getTargetId(), permission.getPermissionType());
        }

        for (String targetId : targetIdPermissionTypes.keySet()) {
            Collection<String> permissionTypes = targetIdPermissionTypes.get(targetId);
            List<Permission> current =
                    permissionRepository.findByPermissionTypeInAndTargetId(permissionTypes, targetId);
            Preconditions.checkState(CollectionUtils.isEmpty(current),
                    "Permission with permissionType %s targetId %s already exists!", permissionTypes,
                    targetId);
        }

        Iterable<Permission> results = permissionRepository.saveAll(permissions);
        return StreamSupport.stream(results.spliterator(), false).collect(Collectors.toSet());
    }

    @Transactional
    @Override
    public void deleteRolePermissionsByAppId(String appId, String operator) {
        appId = EscapeCharacter.DEFAULT.escape(appId);
        List<Long> permissionIds = permissionRepository.findPermissionIdsByAppId(appId);

        if (!permissionIds.isEmpty()) {
            // 1. delete Permission
            permissionRepository.batchDelete(permissionIds, operator);

            // 2. delete Role Permission
            rolePermissionRepository.batchDeleteByPermissionIds(permissionIds, operator);
        }

        List<Long> roleIds = roleRepository.findRoleIdsByAppId(appId);

        if (!roleIds.isEmpty()) {
            // 3. delete Role
            roleRepository.batchDelete(roleIds, operator);

            // 4. delete User Role
            userRoleRepository.batchDeleteByRoleIds(roleIds, operator);

            // 5. delete Consumer Role
            consumerRoleRepository.batchDeleteByRoleIds(roleIds, operator);
        }
    }

    @Transactional
    @Override
    public void deleteRolePermissionsByAppIdAndNamespace(String appId, String namespaceName, String operator) {
        appId = EscapeCharacter.DEFAULT.escape(appId);
        List<Long> permissionIds = permissionRepository.findPermissionIdsByAppIdAndNamespace(appId, namespaceName);

        if (!permissionIds.isEmpty()) {
            // 1. delete Permission
            permissionRepository.batchDelete(permissionIds, operator);

            // 2. delete Role Permission
            rolePermissionRepository.batchDeleteByPermissionIds(permissionIds, operator);
        }

        List<Long> roleIds = roleRepository.findRoleIdsByAppIdAndNamespace(appId, namespaceName);

        if (!roleIds.isEmpty()) {
            // 3. delete Role
            roleRepository.batchDelete(roleIds, operator);

            // 4. delete User Role
            userRoleRepository.batchDeleteByRoleIds(roleIds, operator);

            // 5. delete Consumer Role
            consumerRoleRepository.batchDeleteByRoleIds(roleIds, operator);
        }
    }
}
