package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.portal.constant.AuthConstants;

import org.springframework.stereotype.Component;

@Component("unifiedPermissionValidator")
public class UnifiedPermissionValidator implements PermissionValidator{
    private final UserPermissionValidator userPermissionValidator;
    private final ConsumerPermissionValidator consumerPermissionValidator;

    public UnifiedPermissionValidator(UserPermissionValidator userPermissionValidator, ConsumerPermissionValidator consumerPermissionValidator) {
        this.userPermissionValidator = userPermissionValidator;
        this.consumerPermissionValidator = consumerPermissionValidator;
    }

    private PermissionValidator getDelegate() {
        String type = AuthContextHolder.getAuthType();
        if (AuthConstants.USER.equals(type)) {
            return userPermissionValidator;
        }
        if (AuthConstants.CONSUMER.equals(type)) {
            return consumerPermissionValidator;
        }
        throw new IllegalStateException("Unknown authentication type");
    }
    @Override
    public boolean hasModifyNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
        return getDelegate().hasModifyNamespacePermission(appId, env, clusterName, namespaceName);
    }

    @Override
    public boolean hasReleaseNamespacePermission(String appId, String env, String clusterName, String namespaceName) {
        return getDelegate().hasReleaseNamespacePermission(appId, env, clusterName, namespaceName);
    }

    @Override
    public boolean hasAssignRolePermission(String appId) {
        return getDelegate().hasAssignRolePermission(appId);
    }

    @Override
    public boolean hasCreateNamespacePermission(String appId) {
        return getDelegate().hasCreateNamespacePermission(appId);
    }

    @Override
    public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
        return getDelegate().hasCreateAppNamespacePermission(appId, appNamespace);
    }

    @Override
    public boolean hasCreateClusterPermission(String appId) {
        return getDelegate().hasCreateClusterPermission(appId);
    }

    @Override
    public boolean isSuperAdmin() {
        return getDelegate().isSuperAdmin();
    }

    @Override
    public boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName, String namespaceName) {
        return getDelegate().shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName);
    }

    @Override
    public boolean hasCreateApplicationPermission() {
        return getDelegate().hasCreateApplicationPermission();
    }


    @Override
    public boolean hasManageAppMasterPermission(String appId) {
        return getDelegate().hasManageAppMasterPermission(appId);
    }
}
