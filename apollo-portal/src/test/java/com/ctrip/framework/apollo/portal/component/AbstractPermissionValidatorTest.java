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

    private AbstractPermissionValidator permissionValidator;

    @Mock
    private AppNamespace appNamespace;

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

        // Test the behavior in the concrete implementation class
        boolean result = permissionValidator.hasModifyNamespacePermission(appId, env, clusterName, namespaceName);

        // Verify the result (based on mock implementation)
        assertFalse(result);
    }

    @Test
    public void testHasReleaseNamespacePermission() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        boolean result = permissionValidator.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName);

        assertFalse(result);
    }

    @Test
    public void testHasAssignRolePermission() {
        String appId = "testApp";

        boolean result = permissionValidator.hasAssignRolePermission(appId);

        assertFalse(result);
    }

    @Test
    public void testHasCreateNamespacePermission() {
        String appId = "testApp";

        boolean result = permissionValidator.hasCreateNamespacePermission(appId);

        assertFalse(result);
    }

    @Test
    public void testHasCreateAppNamespacePermission() {
        String appId = "testApp";

        boolean result = permissionValidator.hasCreateAppNamespacePermission(appId, appNamespace);

        assertFalse(result);
    }

    @Test
    public void testHasCreateClusterPermission() {
        String appId = "testApp";

        boolean result = permissionValidator.hasCreateClusterPermission(appId);

        assertFalse(result);
    }

    @Test
    public void testIsSuperAdmin() {
        boolean result = permissionValidator.isSuperAdmin();

        assertFalse(result);
    }

    @Test
    public void testShouldHideConfigToCurrentUser() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        boolean result = permissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName);

        assertFalse(result);
    }

    @Test
    public void testHasCreateApplicationPermission() {
        boolean result = permissionValidator.hasCreateApplicationPermission();

        assertFalse(result);
    }

    @Test
    public void testHasManageAppMasterPermission() {
        String appId = "testApp";

        boolean result = permissionValidator.hasManageAppMasterPermission(appId);

        assertFalse(result);
    }

    // Concrete implementation class for testing
    private static class AbstractPermissionValidatorImpl extends AbstractPermissionValidator {

        @Override
        protected boolean hasPermission(String targetId, String permissionType) {
            return false;
        }

        @Override
        protected boolean hasPermissions(List<String> requiredPerms) {
            return false;
        }
    }

    // Another test implementation class to verify permission check logic
    private static class AbstractPermissionValidatorWithPermissionsImpl extends AbstractPermissionValidator {
        private final List<String> allowedPermissions;
        private final List<String> allowedPermissionList;

        public AbstractPermissionValidatorWithPermissionsImpl(List<String> allowedPermissions,
                                                              List<String> allowedPermissionList) {
            this.allowedPermissions = allowedPermissions;
            this.allowedPermissionList = allowedPermissionList;
        }

        @Override
        protected boolean hasPermission(String targetId, String permissionType) {
            String requiredPermission = permissionType + ":" + targetId;
            return allowedPermissions.contains(requiredPermission);
        }

        @Override
        protected boolean hasPermissions(List<String> requiredPerms) {
            return requiredPerms.stream().anyMatch(allowedPermissionList::contains);
        }
    }

    @Test
    public void testHasModifyNamespacePermissionWithGrantedPermissions() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        List<String> requiredPerms = Arrays.asList(
                PermissionType.MODIFY_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName),
                PermissionType.MODIFY_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName, env),
                PermissionType.MODIFY_NAMESPACES_IN_CLUSTER + ":" +
                        RoleUtils.buildClusterTargetId(appId, env, clusterName)
        );

        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(
                Arrays.asList(), requiredPerms);

        boolean result = validator.hasModifyNamespacePermission(appId, env, clusterName, namespaceName);

        assertTrue(result);
    }

    @Test
    public void testHasReleaseNamespacePermissionWithGrantedPermissions() {
        String appId = "testApp";
        String env = "DEV";
        String clusterName = "default";
        String namespaceName = "application";

        List<String> requiredPerms = Arrays.asList(
                PermissionType.RELEASE_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName),
                PermissionType.RELEASE_NAMESPACE + ":" +
                        RoleUtils.buildNamespaceTargetId(appId, namespaceName, env),
                PermissionType.RELEASE_NAMESPACES_IN_CLUSTER + ":" +
                        RoleUtils.buildClusterTargetId(appId, env, clusterName)
        );

        AbstractPermissionValidator validator = new AbstractPermissionValidatorWithPermissionsImpl(
                Arrays.asList(), requiredPerms);

        boolean result = validator.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName);

        assertTrue(result);
    }
}