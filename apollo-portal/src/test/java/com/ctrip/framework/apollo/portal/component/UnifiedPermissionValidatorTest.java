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

import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.portal.constant.AuthConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnifiedPermissionValidatorTest {

    @Mock
    private UserPermissionValidator userPermissionValidator;

    @Mock
    private ConsumerPermissionValidator consumerPermissionValidator;

    @InjectMocks
    private UnifiedPermissionValidator unifiedPermissionValidator;

    // No additional initialization required before each test method (keep as is)
    @BeforeEach
    public void setUp() {
        // No operation needed, AuthContextHolder state will be set separately in each test
    }

    // Clean up AuthContextHolder state after each test method (critical! avoid pollution between tests)
    @AfterEach
    public void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    public void hasManageAppMasterPermission_UserAuthType_DelegatesToUserValidator() {
        final String appId = "testAppId";
        final boolean expectedPermission = true;

        // Set authentication type to USER
        AuthContextHolder.setAuthType(AuthConstants.USER);
        when(userPermissionValidator.hasManageAppMasterPermission(appId)).thenReturn(expectedPermission);

        boolean result = unifiedPermissionValidator.hasManageAppMasterPermission(appId);

        assertTrue(result);
    }

    @Test
    public void hasManageAppMasterPermission_ConsumerAuthType_DelegatesToConsumerValidator() {
        final String appId = "testAppId";
        final boolean expectedPermission = false;

        // Set authentication type to CONSUMER
        AuthContextHolder.setAuthType(AuthConstants.CONSUMER);
        when(consumerPermissionValidator.hasManageAppMasterPermission(appId)).thenReturn(expectedPermission);

        boolean result = unifiedPermissionValidator.hasManageAppMasterPermission(appId);

        assertFalse(result);
    }

    @Test
    public void hasManageAppMasterPermission_UnknownAuthType_ThrowsException() {
        final String appId = "testAppId";

        // Set authentication type to UNKNOWN
        AuthContextHolder.setAuthType("UNKNOWN");

        assertThrows(IllegalStateException.class, () -> {
            unifiedPermissionValidator.hasManageAppMasterPermission(appId);
        });
    }

    @Test
    public void hasCreateNamespacePermission_UserAuthType_UsesUserPermissionValidator() {
        final String appId = "testAppId";
        final boolean expectedPermission = true;

        // Set authentication type to USER
        AuthContextHolder.setAuthType(AuthConstants.USER);
        when(userPermissionValidator.hasCreateNamespacePermission(appId)).thenReturn(expectedPermission);

        boolean result = unifiedPermissionValidator.hasCreateNamespacePermission(appId);

        assertTrue(result);
    }

    @Test
    public void hasCreateNamespacePermission_ConsumerAuthType_UsesConsumerPermissionValidator() {
        final String appId = "testAppId";
        final boolean expectedPermission = true;

        // Set authentication type to CONSUMER
        AuthContextHolder.setAuthType(AuthConstants.CONSUMER);
        when(consumerPermissionValidator.hasCreateNamespacePermission(appId)).thenReturn(expectedPermission);

        boolean result = unifiedPermissionValidator.hasCreateNamespacePermission(appId);

        assertTrue(result);
    }

    @Test
    public void hasCreateNamespacePermission_UnknownAuthType_ThrowsIllegalStateException() {
        final String appId = "testAppId";

        // Set authentication type to UNKNOWN
        AuthContextHolder.setAuthType("UNKNOWN");

        assertThrows(IllegalStateException.class, () -> unifiedPermissionValidator.hasCreateNamespacePermission(appId));
    }
}