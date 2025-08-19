package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.util.RoleUtils;

public abstract class AbstractPermissionValidator implements PermissionValidator {

    @Override
    public boolean hasModifyNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
        // 生成不同层级的 TargetId
        String globalNsTarget = RoleUtils.buildNamespaceTargetId(appId,
                namespaceName);
        String envNsTarget = RoleUtils.buildNamespaceTargetId(appId,
                namespaceName, env);
        String clusterTarget = RoleUtils.buildClusterTargetId(appId, env,
                clusterName);
        // 统一调用 hasPermission
        return hasPermission(globalNsTarget, PermissionType.MODIFY_NAMESPACE) ||
                hasPermission(envNsTarget, PermissionType.MODIFY_NAMESPACE) ||
                hasPermission(clusterTarget,
                        PermissionType.MODIFY_NAMESPACES_IN_CLUSTER);
    }

    @Override
    public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
        String globalNsTarget = RoleUtils.buildNamespaceTargetId(appId,
                namespaceName);
        String envNsTarget = RoleUtils.buildNamespaceTargetId(appId,
                namespaceName, env);
        String clusterTarget = RoleUtils.buildClusterTargetId(appId, env,
                clusterName);
        return hasPermission(globalNsTarget, PermissionType.RELEASE_NAMESPACE) ||
                hasPermission(envNsTarget, PermissionType.RELEASE_NAMESPACE) ||
                hasPermission(clusterTarget,
                        PermissionType.RELEASE_NAMESPACES_IN_CLUSTER);
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
}
