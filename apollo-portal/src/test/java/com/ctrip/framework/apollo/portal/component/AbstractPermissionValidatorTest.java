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
import java.util.Collections;
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
    public void testHasModifyNamespacePermission_WhenNoPermission() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";
        assertFalse(permissionValidator.hasModifyNamespacePermission(appId, env, clusterName, namespaceName));
    }

    @Test
    public void testHasReleaseNamespacePermission_WhenNoPermission() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";
        assertFalse(permissionValidator.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName));
    }

    @Test
    public void testHasAssignRolePermission_WhenNoPermission() {
        assertFalse(permissionValidator.hasAssignRolePermission("testApp"));
    }

    @Test
    public void testHasCreateNamespacePermission_WhenNoPermission() {
        assertFalse(permissionValidator.hasCreateNamespacePermission("testApp"));
    }

    @Test
    public void testHasCreateAppNamespacePermission_WhenNoPermission() {
        assertFalse(permissionValidator.hasCreateAppNamespacePermission("testApp", appNamespace));
    }

    @Test
    public void testHasCreateClusterPermission_WhenNoPermission() {
        assertFalse(permissionValidator.hasCreateClusterPermission("testApp"));
    }

    @Test
    public void testIsSuperAdmin_WhenNoPermission() {
        assertFalse(permissionValidator.isSuperAdmin());
    }

    @Test
    public void testShouldHideConfigToCurrentUser_WhenNoPermission() {
        assertFalse(permissionValidator.shouldHideConfigToCurrentUser("testApp", "DEV", "default", "application"));
    }

    @Test
    public void testHasCreateApplicationPermission_WhenNoPermission() {
        assertFalse(permissionValidator.hasCreateApplicationPermission());
    }

    @Test
    public void testHasManageAppMasterPermission_WhenNoPermission() {
        assertFalse(permissionValidator.hasManageAppMasterPermission("testApp"));
    }

    @Test
    public void testHasModifyNamespacePermission_WhenWithPermission() {
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
    public void testHasReleaseNamespacePermission_WhenWithPermission() {
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

    @Test
    public void testHasAssignRolePermission_WhenWithPermission() {
        String appId = "testApp";
        List<Permission> granted = Collections.singletonList(
                new Permission(PermissionType.ASSIGN_ROLE, appId)
        );
        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertTrue(validator.hasAssignRolePermission(appId));
    }

    @Test
    public void testHasCreateNamespacePermission_WhenWithPermission() {
        String appId = "testApp";
        List<Permission> granted = Collections.singletonList(
                new Permission(PermissionType.CREATE_NAMESPACE, appId)
        );
        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertTrue(validator.hasCreateNamespacePermission(appId));
    }

    @Test
    public void testHasCreateClusterPermission_WhenWithPermission() {
        String appId = "testApp";
        List<Permission> granted = Collections.singletonList(
                new Permission(PermissionType.CREATE_CLUSTER, appId)
        );
        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertTrue(validator.hasCreateClusterPermission(appId));
    }

    @Test
    public void testShouldHideConfigToCurrentUser_WhenWithPermission() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        List<Permission> granted = Arrays.asList(
                new Permission(PermissionType.MODIFY_NAMESPACE,
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName)),
                new Permission(PermissionType.RELEASE_NAMESPACE,
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName))
        );

        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertFalse(validator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName));
    }

    @Test
    public void testHasManageAppMasterPermission_WhenWithPermission() {
        String appId = "testApp";
        List<Permission> granted = Collections.singletonList(
                new Permission(PermissionType.MANAGE_APP_MASTER, appId)
        );
        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(granted);
        assertTrue(validator.hasManageAppMasterPermission(appId));
    }

    private static class AbstractPermissionValidatorImpl extends AbstractPermissionValidator {
        @Override
        public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
            return false;
        }

        @Override
        public boolean isSuperAdmin() {
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

        @Override
        protected boolean hasPermissions(List<Permission> requiredPerms) {
            return false;
        }

        @Override
        public boolean hasCreateApplicationPermission(String userId) {
            return false;
        }
    }

    private static class AbstractPermissionValidatorWithPermissionsImpl extends AbstractPermissionValidator {
        private final List<Permission> allowed;

        AbstractPermissionValidatorWithPermissionsImpl(List<Permission> allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
            return true;
        }

        @Override
        public boolean isSuperAdmin() {
            return true;
        }

        @Override
        public boolean hasCreateApplicationPermission() {
            return true;
        }

        @Override
        public boolean hasManageAppMasterPermission(String appId) {
            return true;
        }

        @Override
        protected boolean hasPermissions(List<Permission> requiredPerms) {
            return requiredPerms.stream().anyMatch(allowed::contains);
        }

        @Override
        public boolean hasCreateApplicationPermission(String userId) {
            return true;
        }
    }
}