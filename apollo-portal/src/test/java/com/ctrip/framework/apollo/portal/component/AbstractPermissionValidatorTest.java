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
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AbstractPermissionValidatorTest {

    @Mock
    private AppNamespace appNamespace;

    private AbstractPermissionValidator permissionValidator;

    @Before
    public void setUp() {
        permissionValidator = new AbstractPermissionValidatorImpl();
    }

    @Test
    public void testHasModifyNamespacePermission() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";
        assertFalse(permissionValidator.hasModifyNamespacePermission(appId, env, clusterName, namespaceName));
    }

    @Test
    public void testHasReleaseNamespacePermission() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";
        assertFalse(permissionValidator.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName));
    }

    @Test
    public void testHasAssignRolePermission() {
        assertFalse(permissionValidator.hasAssignRolePermission("testApp"));
    }

    @Test
    public void testHasCreateNamespacePermission() {
        assertFalse(permissionValidator.hasCreateNamespacePermission("testApp"));
    }

    @Test
    public void testHasCreateAppNamespacePermission() {
        assertFalse(permissionValidator.hasCreateAppNamespacePermission("testApp", appNamespace));
    }

    @Test
    public void testHasCreateClusterPermission() {
        assertFalse(permissionValidator.hasCreateClusterPermission("testApp"));
    }

    @Test
    public void testIsSuperAdmin() {
        assertFalse(permissionValidator.isSuperAdmin());
    }

    @Test
    public void testShouldHideConfigToCurrentUser() {
        assertFalse(permissionValidator.shouldHideConfigToCurrentUser("testApp", "DEV", "default", "application"));
    }

    @Test
    public void testHasCreateApplicationPermission() {
        assertFalse(permissionValidator.hasCreateApplicationPermission());
    }

    @Test
    public void testHasManageAppMasterPermission() {
        assertFalse(permissionValidator.hasManageAppMasterPermission("testApp"));
    }

    /* test implementations */
    private static class AbstractPermissionValidatorImpl extends AbstractPermissionValidator {
        @Override
        protected boolean hasPermissions(List<Permission> requiredPerms) {
            return false;
        }
    }

    private static class AbstractPermissionValidatorWithPermissionsImpl extends AbstractPermissionValidator {
        private final List<Permission> allowed;

        AbstractPermissionValidatorWithPermissionsImpl(List<Permission> allowed) {
            this.allowed = allowed;
        }

        @Override
        protected boolean hasPermissions(List<Permission> requiredPerms) {
            return requiredPerms.stream().anyMatch(allowed::contains);
        }
    }

    /* positive cases */
    @Test
    public void testHasModifyNamespacePermissionWithGrantedPermissions() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        List<Permission> granted = Arrays.asList(
                new Permission(PermissionType.MODIFY_NAMESPACE,
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName)),
                new Permission(PermissionType.MODIFY_NAMESPACE,
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName, env)),
                new Permission(PermissionType.MODIFY_NAMESPACES_IN_CLUSTER,
                        RoleUtils.buildClusterTargetId(appId, env, clusterName))
        );

        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertTrue(validator.hasModifyNamespacePermission(appId, env, clusterName, namespaceName));
    }

    @Test
    public void testHasReleaseNamespacePermissionWithGrantedPermissions() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        List<Permission> granted = Arrays.asList(
                new Permission(PermissionType.RELEASE_NAMESPACE,
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName)),
                new Permission(PermissionType.RELEASE_NAMESPACE,
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName, env)),
                new Permission(PermissionType.RELEASE_NAMESPACES_IN_CLUSTER,
                        RoleUtils.buildClusterTargetId(appId, env, clusterName))
        );

        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertTrue(validator.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName));
    }
}