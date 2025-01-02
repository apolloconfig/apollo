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

import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.service.ConsumerRolePermissionService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.RolePermission;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RolePermissionRepository;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConsumerPermissionValidator {

  private final ConsumerAuthUtil consumerAuthUtil;
  private final RolePermissionRepository rolePermissionRepository;
  private final ConsumerRoleRepository consumerRoleRepository;
  private final PermissionRepository permissionRepository;

  public ConsumerPermissionValidator(final ConsumerAuthUtil consumerAuthUtil,
                                     final RolePermissionRepository rolePermissionRepository
          , final ConsumerRoleRepository consumerRoleRepository,
                                     final PermissionRepository permissionRepository) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.rolePermissionRepository = rolePermissionRepository;
    this.consumerRoleRepository = consumerRoleRepository;
    this.permissionRepository = permissionRepository;
  }

  public boolean hasModifyNamespacePermission(HttpServletRequest request, String appId,
      String namespaceName, String env) {
    if (hasCreateNamespacePermission(request, appId)) {
      return true;
    }
    return consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.MODIFY_NAMESPACE, RoleUtils.buildNamespaceTargetId(appId, namespaceName))
        || consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
            PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env));

  }

  public boolean hasReleaseNamespacePermission(HttpServletRequest request, String appId,
      String namespaceName, String env) {
    if (hasCreateNamespacePermission(request, appId)) {
      return true;
    }
    return consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.RELEASE_NAMESPACE, RoleUtils.buildNamespaceTargetId(appId, namespaceName))
        || consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
            PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env));

  }

  public boolean hasCreateNamespacePermission(HttpServletRequest request, String appId) {
    return consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.CREATE_NAMESPACE, appId);
  }

  public boolean hasCreateClusterPermission(HttpServletRequest request, String appId) {
    return consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.CREATE_CLUSTER, appId);
  }

  public boolean hasCreateApplicationPermission(HttpServletRequest request) {
    long consumerId = consumerAuthUtil.retrieveConsumerId(request);
    return consumerHasPermission(consumerId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  public boolean consumerHasPermission(long consumerId, String permissionType, String targetId) {
    Permission permission =
            permissionRepository.findTopByPermissionTypeAndTargetId(permissionType, targetId);
    if (permission == null) {
      return false;
    }

    List<ConsumerRole> consumerRoles = consumerRoleRepository.findByConsumerId(consumerId);
    if (CollectionUtils.isEmpty(consumerRoles)) {
      return false;
    }

    Set<Long> roleIds =
            consumerRoles.stream().map(ConsumerRole::getRoleId).collect(Collectors.toSet());
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
}
