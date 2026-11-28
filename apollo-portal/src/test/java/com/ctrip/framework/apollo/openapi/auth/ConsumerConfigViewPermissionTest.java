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
package com.ctrip.framework.apollo.openapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerRolePermissionService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.openapi.v1.controller.ItemController;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserTokenPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Verifies Consumer configuration visibility and isolation of application, environment and roles.
 */
@ExtendWith(MockitoExtension.class)
class ConsumerConfigViewPermissionTest {

  private static final long CONSUMER_ID = 1L;
  private static final String APP_ID = "app-1";
  private static final String NAMESPACE = "application";
  private static final String CLUSTER = "default";

  @Mock
  private ConsumerRolePermissionService permissionService;
  @Mock
  private ConsumerAuthUtil consumerAuthUtil;
  @Mock
  private PortalConfig portalConfig;
  @Mock
  private AppNamespaceService appNamespaceService;

  private ConsumerPermissionValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ConsumerPermissionValidator(permissionService, consumerAuthUtil, portalConfig,
        appNamespaceService);
    lenient().when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
    lenient().when(portalConfig.isConsumerConfigViewMemberOnly("PRO")).thenReturn(true);
    lenient().when(portalConfig.isConsumerConfigViewMemberOnly("FAT")).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PRO", "pro", "prod", "FAT"})
  void shouldHidePrivateConfigWithoutTargetPermission(String env) {
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, env, CLUSTER, NAMESPACE)).isTrue();
  }

  @Test
  void shouldKeepUnrestrictedEnvironmentReadable() {
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "DEV", CLUSTER, NAMESPACE))
        .isFalse();
    verifyNoInteractions(permissionService, appNamespaceService);
  }

  @Test
  void shouldKeepPublicNamespaceReadable() {
    AppNamespace publicNamespace = new AppNamespace();
    publicNamespace.setPublic(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(publicNamespace);

    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "PRO", CLUSTER, NAMESPACE))
        .isFalse();
    verifyNoInteractions(permissionService);
  }

  @ParameterizedTest
  @MethodSource("readPermissions")
  void shouldAllowApplicationAdminOrNamespaceOperator(Permission grantedPermission) {
    grant(Set.of(grantedPermission));

    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "prod", CLUSTER, NAMESPACE))
        .isFalse();
    verify(portalConfig).isConsumerConfigViewMemberOnly("PRO");
  }

  @ParameterizedTest
  @MethodSource("unrelatedPermissions")
  void shouldNotUsePermissionsForAnotherResource(Permission grantedPermission) {
    grant(Set.of(grantedPermission));

    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, "PRO", CLUSTER, NAMESPACE)).isTrue();
  }

  @Test
  void shouldRejectInvalidEnvironment() {
    assertThatThrownBy(
        () -> validator.shouldHideConfigToCurrentUser(APP_ID, "invalid", CLUSTER, NAMESPACE))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(permissionService, appNamespaceService, portalConfig);
  }

  @Test
  void shouldEnforceCurrentConsumerPermissionsBeforeLoadingItem() {
    Set<Permission> permissions = new HashSet<>();
    Permission permission = new Permission(PermissionType.MODIFY_NAMESPACE,
        RoleUtils.buildNamespaceTargetId(APP_ID, NAMESPACE, "PRO"));
    permissions.add(permission);
    grant(permissions);
    ItemOpenApiService itemService = mock(ItemOpenApiService.class);
    UnifiedPermissionValidator unifiedValidator = new UnifiedPermissionValidator(
        mock(UserPermissionValidator.class), mock(UserTokenPermissionValidator.class), validator);
    ItemController controller = new ItemController(itemService, mock(UserService.class),
        mock(UserInfoHolder.class), unifiedValidator, mock(NamespaceBranchService.class));
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);

    controller.getItem(APP_ID, "PRO", CLUSTER, NAMESPACE, "timeout");
    verify(itemService).getItem(APP_ID, "PRO", CLUSTER, NAMESPACE, "timeout");

    permissions.clear();
    assertThatThrownBy(() -> controller.getItem(APP_ID, "PRO", CLUSTER, NAMESPACE, "secret"))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoMoreInteractions(itemService);
  }

  private void grant(Set<Permission> permissions) {
    when(permissionService.hasAnyPermission(eq(CONSUMER_ID), anyList()))
        .thenAnswer(invocation -> {
          List<Permission> requiredPermissions = invocation.getArgument(1);
          return requiredPermissions.stream().anyMatch(permissions::contains);
        });
  }

  private static Stream<Permission> readPermissions() {
    return Stream.of(new Permission(PermissionType.ASSIGN_ROLE, APP_ID),
        new Permission(PermissionType.CREATE_NAMESPACE, APP_ID),
        new Permission(PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(APP_ID, NAMESPACE)),
        new Permission(PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(APP_ID, NAMESPACE)),
        new Permission(PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(APP_ID, NAMESPACE, "PRO")),
        new Permission(PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(APP_ID, NAMESPACE, "PRO")),
        new Permission(PermissionType.MODIFY_NAMESPACES_IN_CLUSTER,
            RoleUtils.buildClusterTargetId(APP_ID, "PRO", CLUSTER)),
        new Permission(PermissionType.RELEASE_NAMESPACES_IN_CLUSTER,
            RoleUtils.buildClusterTargetId(APP_ID, "PRO", CLUSTER)));
  }

  private static Stream<Permission> unrelatedPermissions() {
    return Stream.of(new Permission(PermissionType.ASSIGN_ROLE, "app-2"),
        new Permission(PermissionType.CREATE_NAMESPACE, "app-2"),
        new Permission(PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId("app-2", NAMESPACE)),
        new Permission(PermissionType.RELEASE_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(APP_ID, "secret", "PRO")),
        new Permission(PermissionType.MODIFY_NAMESPACE,
            RoleUtils.buildNamespaceTargetId(APP_ID, NAMESPACE, "FAT")),
        new Permission(PermissionType.RELEASE_NAMESPACES_IN_CLUSTER,
            RoleUtils.buildClusterTargetId(APP_ID, "PRO", "other")));
  }
}
