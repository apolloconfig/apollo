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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.openapi.model.OpenAppRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenPermissionConditionDTO;
import com.ctrip.framework.apollo.openapi.server.service.PermissionOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for OpenAPI permission controller user-token authorization gates.
 */
@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

  private static final String APP_ID = "sample-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";

  @Mock
  private PermissionOpenApiService permissionOpenApiService;

  @Mock
  private OpenApiOperatorResolver operatorResolver;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  private PermissionController controller;

  @BeforeEach
  void setUp() {
    controller = new PermissionController(permissionOpenApiService, operatorResolver,
        unifiedPermissionValidator);
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  void getAppRolesShouldRejectUserTokenWithoutAppManageRole() {
    when(unifiedPermissionValidator.hasAssignRolePermission(APP_ID)).thenReturn(false);

    assertThatThrownBy(() -> controller.getAppRoles(APP_ID))
        .isInstanceOf(AccessDeniedException.class);

    verifyNoInteractions(permissionOpenApiService);
  }

  @Test
  void getAppRolesShouldAllowUserTokenWithAppManageRole() {
    when(unifiedPermissionValidator.hasAssignRolePermission(APP_ID)).thenReturn(true);
    OpenAppRoleUserDTO response = new OpenAppRoleUserDTO();
    when(permissionOpenApiService.getAppRoles(APP_ID)).thenReturn(response);

    controller.getAppRoles(APP_ID);

    verify(permissionOpenApiService).getAppRoles(APP_ID);
  }

  @Test
  void hasAppPermissionShouldRejectUserTokenWithoutAppManageRole() {
    when(unifiedPermissionValidator.hasAssignRolePermission(APP_ID)).thenReturn(false);

    assertThatThrownBy(() -> controller.hasAppPermission(APP_ID, "AssignRole", "apollo"))
        .isInstanceOf(AccessDeniedException.class);

    verify(permissionOpenApiService, never()).hasAppPermission(anyString(), anyString(),
        anyString());
  }

  @Test
  void getNamespaceEnvRoleUsersShouldRejectUserTokenWithoutNamespaceScope() {
    when(unifiedPermissionValidator.hasAssignRolePermission(APP_ID, ENV, null, NAMESPACE))
        .thenReturn(false);

    assertThatThrownBy(() -> controller.getNamespaceEnvRoleUsers(APP_ID, ENV, NAMESPACE))
        .isInstanceOf(AccessDeniedException.class);

    verify(permissionOpenApiService, never()).getNamespaceEnvRoleUsers(anyString(), anyString(),
        anyString());
  }

  @Test
  void assignClusterNamespaceRoleShouldRejectUserTokenWithoutClusterScope() {
    when(unifiedPermissionValidator.hasAssignRolePermission(APP_ID, ENV, CLUSTER, null))
        .thenReturn(false);

    assertThatThrownBy(() -> controller.assignClusterNamespaceRoleToUser(APP_ID, ENV, CLUSTER,
        "ModifyNamespace", "apollo", null)).isInstanceOf(AccessDeniedException.class);

    verify(permissionOpenApiService, never()).assignClusterNamespaceRoleToUser(anyString(),
        anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void hasRootPermissionShouldRejectUserTokenWithoutSystemAdmin() {
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    assertThatThrownBy(() -> controller.hasRootPermission("apollo"))
        .isInstanceOf(AccessDeniedException.class);

    verify(permissionOpenApiService, never()).hasRootPermission(anyString());
  }

  @Test
  void hasRootPermissionShouldAllowUserTokenWithSystemAdmin() {
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);
    when(permissionOpenApiService.hasRootPermission("apollo"))
        .thenReturn(new OpenPermissionConditionDTO());

    controller.hasRootPermission("apollo");

    verify(permissionOpenApiService).hasRootPermission("apollo");
  }

  @Test
  void hasCreateApplicationPermissionShouldRejectUserTokenWithoutSystemAdmin() {
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    assertThatThrownBy(() -> controller.hasCreateApplicationPermission("apollo"))
        .isInstanceOf(AccessDeniedException.class);

    verify(permissionOpenApiService, never()).hasCreateApplicationPermission(anyString());
  }

  @Test
  void initAppPermissionShouldAllowUserTokenWithAppManageRole() {
    when(unifiedPermissionValidator.hasAssignRolePermission(APP_ID, null, null, NAMESPACE))
        .thenReturn(true);
    when(operatorResolver.resolve(null)).thenReturn("token-user");

    controller.initAppPermission(APP_ID, NAMESPACE, null);

    verify(permissionOpenApiService).initAppPermission(APP_ID, NAMESPACE, "token-user");
  }

  @Test
  void isManageAppMasterPermissionEnabledShouldRejectUserTokenWithoutSystemAdmin() {
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    assertThatThrownBy(() -> controller.isManageAppMasterPermissionEnabled())
        .isInstanceOf(AccessDeniedException.class);

    verify(permissionOpenApiService, never()).isManageAppMasterPermissionEnabled();
  }

  @Test
  void isManageAppMasterPermissionEnabledShouldAllowUserTokenWithSystemAdmin() {
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);
    when(permissionOpenApiService.isManageAppMasterPermissionEnabled())
        .thenReturn(Collections.emptyMap());

    controller.isManageAppMasterPermissionEnabled();

    verify(permissionOpenApiService).isManageAppMasterPermissionEnabled();
  }
}
