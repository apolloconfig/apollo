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
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.util.RoleUtils;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractPermissionValidator implements PermissionValidator {

    @Override
    public boolean hasModifyNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
        List<String> requiredPerms = Arrays.asList(
                PermissionType.MODIFY_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName),
                PermissionType.MODIFY_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName, env),
                PermissionType.MODIFY_NAMESPACES_IN_CLUSTER + ":" +
                        RoleUtils.buildClusterTargetId(appId, env, clusterName)
        );
        return hasPermissions(requiredPerms);
    }

    @Override
    public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
        List<String> requiredPerms = Arrays.asList(
                PermissionType.RELEASE_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName),
                PermissionType.RELEASE_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName, env),
                PermissionType.RELEASE_NAMESPACES_IN_CLUSTER + ":" +
                        RoleUtils.buildClusterTargetId(appId, env, clusterName)
        );
        return hasPermissions(requiredPerms);
    }

    @Override
    public boolean hasAssignRolePermission(String appId) {
        return hasPermission(appId, PermissionType.ASSIGN_ROLE);
    }

    @Override
    public boolean hasCreateNamespacePermission(String appId) {
        return hasPermission(appId, PermissionType.CREATE_NAMESPACE);
    }

    @Override
    public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
        return false;
    }

    @Override
    public boolean hasCreateClusterPermission(String appId) {
        return hasPermission(appId, PermissionType.CREATE_CLUSTER);
    }

    @Override
    public boolean isSuperAdmin() {
        return false;
    }

    @Override
    public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName, String namespaceName) {
        return false;
    }

    @Override
    public boolean hasCreateApplicationPermission() {
        return false;
    }

    @Override
    public boolean hasManageAppMasterPermission(String appId) {
        return false;
    }

    protected abstract boolean hasPermission(String targetId, String
            permissionType);
    protected abstract boolean hasPermissions(List<String> requiredPerms);
}
