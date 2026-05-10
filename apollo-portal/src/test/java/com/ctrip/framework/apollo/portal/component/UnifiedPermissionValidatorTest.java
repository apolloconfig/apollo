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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnifiedPermissionValidatorTest {

  @Mock
  private UserPermissionValidator userPermissionValidator;

  @Mock
  private ConsumerPermissionValidator consumerPermissionValidator;

  @InjectMocks
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @AfterEach
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void hasManageAppMasterPermission_UserAuthType_DelegatesToUserValidator() {
    final String appId = "testAppId";
    final boolean expectedPermission = true;

    // Set authentication type to USER
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(userPermissionValidator.hasManageAppMasterPermission(appId))
        .thenReturn(expectedPermission);

    boolean result = unifiedPermissionValidator.hasManageAppMasterPermission(appId);

    assertTrue(result);
  }

  @Test
  public void hasManageAppMasterPermission_ConsumerAuthType_DelegatesToConsumerValidator() {
    final String appId = "testAppId";
    final boolean expectedPermission = false;

    // Set authentication type to CONSUMER
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(consumerPermissionValidator.hasManageAppMasterPermission(appId))
        .thenReturn(expectedPermission);

    boolean result = unifiedPermissionValidator.hasManageAppMasterPermission(appId);

    assertFalse(result);
  }

  @Test
  public void hasManageAppMasterPermission_UnknownAuthType_ThrowsException() {
    final String appId = "testAppId";

    UserIdentityContextHolder.setAuthType("UNKNOWN");

    assertThrows(IllegalStateException.class,
        () -> unifiedPermissionValidator.hasManageAppMasterPermission(appId));
  }

  @Test
  public void hasCreateNamespacePermission_UserAuthType_UsesUserPermissionValidator() {
    final String appId = "testAppId";
    final boolean expectedPermission = true;

    // Set authentication type to USER
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(userPermissionValidator.hasCreateNamespacePermission(appId))
        .thenReturn(expectedPermission);

    boolean result = unifiedPermissionValidator.hasCreateNamespacePermission(appId);

    assertTrue(result);
  }

  @Test
  public void hasCreateNamespacePermission_ConsumerAuthType_UsesConsumerPermissionValidator() {
    final String appId = "testAppId";
    final boolean expectedPermission = true;

    // Set authentication type to CONSUMER
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(consumerPermissionValidator.hasCreateNamespacePermission(appId))
        .thenReturn(expectedPermission);

    boolean result = unifiedPermissionValidator.hasCreateNamespacePermission(appId);

    assertTrue(result);
  }

  @Test
  public void hasCreateNamespacePermission_NoAuthType_ThrowsIllegalStateException() {
    final String appId = "testAppId";

    assertThrows(IllegalStateException.class,
        () -> unifiedPermissionValidator.hasCreateNamespacePermission(appId));
  }

  @Test
  public void namespacePermissionMethods_UserAuthType_DelegateToUserValidator() {
    String appId = "testAppId";
    String env = "DEV";
    String clusterName = "default";
    String namespaceName = "application";
    AppNamespace appNamespace = new AppNamespace();

    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(userPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
        namespaceName)).thenReturn(true);
    when(userPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
        namespaceName)).thenReturn(false);
    when(userPermissionValidator.hasCreateAppNamespacePermission(appId, appNamespace))
        .thenReturn(true);
    when(userPermissionValidator.hasCreateClusterPermission(appId)).thenReturn(true);
    when(userPermissionValidator.hasDeleteNamespacePermission(appId)).thenReturn(false);
    when(userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName)).thenReturn(true);

    assertTrue(unifiedPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
        namespaceName));
    assertFalse(unifiedPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
        namespaceName));
    assertTrue(unifiedPermissionValidator.hasCreateAppNamespacePermission(appId, appNamespace));
    assertTrue(unifiedPermissionValidator.hasCreateClusterPermission(appId));
    assertFalse(unifiedPermissionValidator.hasDeleteNamespacePermission(appId));
    assertTrue(unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName));
  }

  @Test
  public void namespacePermissionMethods_ConsumerAuthType_DelegateToConsumerValidator() {
    String appId = "testAppId";
    String env = "DEV";
    String clusterName = "default";
    String namespaceName = "application";
    AppNamespace appNamespace = new AppNamespace();

    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(consumerPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
        namespaceName)).thenReturn(false);
    when(consumerPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
        namespaceName)).thenReturn(true);
    when(consumerPermissionValidator.hasCreateAppNamespacePermission(appId, appNamespace))
        .thenReturn(false);
    when(consumerPermissionValidator.hasCreateClusterPermission(appId)).thenReturn(true);
    when(consumerPermissionValidator.hasDeleteNamespacePermission(appId)).thenReturn(true);
    when(consumerPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName)).thenReturn(false);

    assertFalse(unifiedPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
        namespaceName));
    assertTrue(unifiedPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
        namespaceName));
    assertFalse(unifiedPermissionValidator.hasCreateAppNamespacePermission(appId, appNamespace));
    assertTrue(unifiedPermissionValidator.hasCreateClusterPermission(appId));
    assertTrue(unifiedPermissionValidator.hasDeleteNamespacePermission(appId));
    assertFalse(unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName));
  }

  @Test
  public void applicationPermissionMethods_UserAuthType_DelegateToUserValidator() {
    String appId = "testAppId";
    String userId = "apollo";

    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(userPermissionValidator.hasAssignRolePermission(appId)).thenReturn(true);
    when(userPermissionValidator.isSuperAdmin()).thenReturn(false);
    when(userPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    when(userPermissionValidator.hasCreateApplicationPermission(userId)).thenReturn(false);

    assertTrue(unifiedPermissionValidator.hasAssignRolePermission(appId));
    assertFalse(unifiedPermissionValidator.isSuperAdmin());
    assertTrue(unifiedPermissionValidator.hasCreateApplicationPermission());
    assertFalse(unifiedPermissionValidator.hasCreateApplicationPermission(userId));
  }

  @Test
  public void applicationPermissionMethods_ConsumerAuthType_DelegateToConsumerValidator() {
    String appId = "testAppId";
    String userId = "apollo";

    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(consumerPermissionValidator.hasAssignRolePermission(appId)).thenReturn(false);
    when(consumerPermissionValidator.isSuperAdmin()).thenReturn(true);
    when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(false);
    when(consumerPermissionValidator.hasCreateApplicationPermission(userId)).thenReturn(true);

    assertFalse(unifiedPermissionValidator.hasAssignRolePermission(appId));
    assertTrue(unifiedPermissionValidator.isSuperAdmin());
    assertFalse(unifiedPermissionValidator.hasCreateApplicationPermission());
    assertTrue(unifiedPermissionValidator.hasCreateApplicationPermission(userId));
  }
}
