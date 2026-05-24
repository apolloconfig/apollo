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
package com.ctrip.framework.apollo.openapi.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.google.common.collect.Sets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ServerPermissionOpenApiService}.
 */
@ExtendWith(MockitoExtension.class)
class ServerPermissionOpenApiServiceTest {

  @Mock
  private RolePermissionService rolePermissionService;
  @Mock
  private UserService userService;
  @Mock
  private RoleInitializationService roleInitializationService;
  @Mock
  private SystemRoleManagerService systemRoleManagerService;

  private ServerPermissionOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerPermissionOpenApiService(rolePermissionService, userService,
        roleInitializationService, systemRoleManagerService);
  }

  @Test
  void assignNamespaceRoleShouldValidateTargetUserAndDelegateRoleName() {
    String appId = "some-app";
    String namespaceName = "application";
    String roleType = PermissionType.MODIFY_NAMESPACE;
    String userId = "target-user";
    String operator = "operator";
    String roleName = RoleUtils.buildNamespaceRoleName(appId, namespaceName, roleType);
    when(userService.findByUserId(userId)).thenReturn(new UserInfo(userId));
    when(rolePermissionService.assignRoleToUsers(roleName, Sets.newHashSet(userId), operator))
        .thenReturn(Sets.newHashSet(userId));

    service.assignNamespaceRoleToUser(appId, namespaceName, roleType, userId, operator);

    verify(rolePermissionService).assignRoleToUsers(roleName, Sets.newHashSet(userId), operator);
  }

  @Test
  void assignNamespaceRoleShouldRejectMissingTargetUser() {
    String userId = "missing-user";
    when(userService.findByUserId(userId)).thenReturn(null);

    assertThrows(BadRequestException.class, () -> service.assignNamespaceRoleToUser("some-app",
        "application", PermissionType.MODIFY_NAMESPACE, userId, "operator"));

    verify(rolePermissionService, never()).assignRoleToUsers(RoleUtils
        .buildNamespaceRoleName("some-app", "application", PermissionType.MODIFY_NAMESPACE),
        Sets.newHashSet(userId), "operator");
  }

  @Test
  void removeNamespaceRoleShouldRejectMissingTargetUser() {
    String userId = "missing-user";
    when(userService.findByUserId(userId)).thenReturn(null);

    assertThrows(BadRequestException.class, () -> service.removeNamespaceRoleFromUser("some-app",
        "application", PermissionType.MODIFY_NAMESPACE, userId, "operator"));

    verify(rolePermissionService, never()).removeRoleFromUsers(RoleUtils
        .buildNamespaceRoleName("some-app", "application", PermissionType.MODIFY_NAMESPACE),
        Sets.newHashSet(userId), "operator");
  }

  @Test
  void hasCreateApplicationPermissionShouldUseUiCompatibleKey() {
    when(systemRoleManagerService.hasCreateApplicationPermission("some-user")).thenReturn(true);

    Map<String, Boolean> result = service.hasCreateApplicationPermission("some-user");

    assertThat(result).containsEntry("hasCreateApplicationPermission", true);
  }

  @Test
  void isManageAppMasterPermissionEnabledShouldUseUiCompatibleKey() {
    when(systemRoleManagerService.isManageAppMasterPermissionEnabled()).thenReturn(true);

    Map<String, Boolean> result = service.isManageAppMasterPermissionEnabled();

    assertThat(result).containsEntry("isManageAppMasterPermissionEnabled", true);
  }
}
