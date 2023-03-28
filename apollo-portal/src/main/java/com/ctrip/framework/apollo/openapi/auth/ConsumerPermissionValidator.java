/*
 * Copyright 2023 Apollo Authors
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

import com.ctrip.framework.apollo.openapi.service.ConsumerRolePermissionService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;

@Component
public class ConsumerPermissionValidator {

  private final ConsumerRolePermissionService permissionService;
  private final ConsumerAuthUtil consumerAuthUtil;

  public ConsumerPermissionValidator(final ConsumerRolePermissionService permissionService,
      final ConsumerAuthUtil consumerAuthUtil) {
    this.permissionService = permissionService;
    this.consumerAuthUtil = consumerAuthUtil;
  }

  public boolean hasModifyNamespacePermission(HttpServletRequest request, String appId,
      String namespaceName, String env) {
    if (hasCreateNamespacePermission(request, appId)) {
      return true;
    }
    return permissionService.consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.MODIFY_NAMESPACE, RoleUtils.buildNamespaceTargetId(appId, namespaceName))
        || permissionService.consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
            PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env));

  }

  public boolean hasReleaseNamespacePermission(HttpServletRequest request, String appId,
      String namespaceName, String env) {
    if (hasCreateNamespacePermission(request, appId)) {
      return true;
    }
    return permissionService.consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.RELEASE_NAMESPACE, RoleUtils.buildNamespaceTargetId(appId, namespaceName))
        || permissionService.consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
            PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(appId, namespaceName, env));

  }

  public boolean hasCreateNamespacePermission(HttpServletRequest request, String appId) {
    return permissionService.consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.CREATE_NAMESPACE, appId);
  }

  public boolean hasCreateClusterPermission(HttpServletRequest request, String appId) {
    return permissionService.consumerHasPermission(consumerAuthUtil.retrieveConsumerId(request),
        PermissionType.CREATE_CLUSTER, appId);
  }
}
