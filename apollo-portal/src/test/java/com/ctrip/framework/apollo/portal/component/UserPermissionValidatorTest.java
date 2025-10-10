/*
 * Copyright 2024 Apollo Authors
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Unit tests for UserPermissionValidator
@ExtendWith(MockitoExtension.class)
class UserPermissionValidatorTest {

  private static final String USER_ID = "test-user";
  private static final String APP_ID = "test-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  @Mock
  private UserInfoHolder userInfoHolder;
  @Mock
  private RolePermissionService rolePermissionService;
  @Mock
  private PortalConfig portalConfig;
  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private SystemRoleManagerService systemRoleManagerService;
  @InjectMocks
  private UserPermissionValidator validator;

  @BeforeEach
  void setUp() {
    // Create a UserInfo instance
    UserInfo stubUser = new UserInfo();
    stubUser.setUserId(USER_ID);
    stubUser.setName("test");
    lenient().when(userInfoHolder.getUser()).thenReturn(stubUser);
  }

  // 1. hasCreateAppNamespacePermission tests

  @Test
  void hasCreateAppNamespacePermission_publicNamespace() {
    AppNamespace publicNs = new AppNamespace();
    publicNs.setPublic(true);
    List<Permission> requiredPermissions = Collections.singletonList(
        new Permission(PermissionType.CREATE_NAMESPACE, APP_ID));
    when(rolePermissionService.hasAnyPermission(USER_ID, requiredPermissions)).thenReturn(true);
    assertThat(validator.hasCreateAppNamespacePermission(APP_ID, publicNs)).isTrue();
  }

  @Test
  void hasCreateAppNamespacePermission_privateNamespace_adminCanCreate() {
    AppNamespace privateNs = new AppNamespace();
    privateNs.setPublic(false);

    when(portalConfig.canAppAdminCreatePrivateNamespace()).thenReturn(true);
    List<Permission> requiredPermissions = Collections.singletonList(
        new Permission(PermissionType.CREATE_NAMESPACE, APP_ID));
    when(rolePermissionService.hasAnyPermission(USER_ID, requiredPermissions)).thenReturn(true);

    assertThat(validator.hasCreateAppNamespacePermission(APP_ID, privateNs)).isTrue();
  }

  @Test
  void hasCreateAppNamespacePermission_privateNamespace_adminCannotCreate_andUserIsSuperAdmin() {
    AppNamespace privateNs = new AppNamespace();
    privateNs.setPublic(false);

    when(portalConfig.canAppAdminCreatePrivateNamespace()).thenReturn(false);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(true);

    assertThat(validator.hasCreateAppNamespacePermission(APP_ID, privateNs)).isTrue();
  }

  @Test
  void hasCreateAppNamespacePermission_privateNamespace_adminCannotCreate_andUserIsNotSuperAdmin() {
    AppNamespace privateNs = new AppNamespace();
    privateNs.setPublic(false);

    when(portalConfig.canAppAdminCreatePrivateNamespace()).thenReturn(false);
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);

    assertThat(validator.hasCreateAppNamespacePermission(APP_ID, privateNs)).isFalse();
  }

  // 2. isSuperAdmin tests

  @Test
  void isSuperAdmin_true() {
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(true);
    assertThat(validator.isSuperAdmin()).isTrue();
  }

  @Test
  void isSuperAdmin_false() {
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
    assertThat(validator.isSuperAdmin()).isFalse();
  }

  // 3. shouldHideConfigToCurrentUser tests

  @Test
  void shouldHideConfigToCurrentUser_envNotMemberOnly() {
    when(portalConfig.isConfigViewMemberOnly(ENV)).thenReturn(false);

    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE)).isFalse();
  }

  @Test
  void shouldHideConfigToCurrentUser_publicNamespace() {
    when(portalConfig.isConfigViewMemberOnly(ENV)).thenReturn(true);

    AppNamespace publicNs = new AppNamespace();
    publicNs.setPublic(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(publicNs);

    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE)).isFalse();
  }

  @Test
  void shouldHideConfigToCurrentUser_userIsAppAdmin() {
    when(portalConfig.isConfigViewMemberOnly(ENV)).thenReturn(true);
    when(appNamespaceService.findByAppIdAndName(APP_ID, NAMESPACE)).thenReturn(null);
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE)).isTrue();
  }

  @Test
  void shouldHideConfigToCurrentUser_userHasNoPermission() {
    when(portalConfig.isConfigViewMemberOnly(ENV)).thenReturn(false);
    assertThat(validator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE)).isFalse();
  }

  // 4. hasCreateApplicationPermission tests

  @Test
  void hasCreateApplicationPermission_true() {
    when(systemRoleManagerService.hasCreateApplicationPermission(USER_ID)).thenReturn(true);
    assertThat(validator.hasCreateApplicationPermission()).isTrue();
  }

  @Test
  void hasCreateApplicationPermission_false() {
    when(systemRoleManagerService.hasCreateApplicationPermission(USER_ID)).thenReturn(false);
    assertThat(validator.hasCreateApplicationPermission()).isFalse();
  }

  // 5. hasManageAppMasterPermission tests

  @Test
  void hasManageAppMasterPermission_superAdmin() {
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(true);
    assertThat(validator.hasManageAppMasterPermission(APP_ID)).isTrue();
  }

  @Test
  void hasManageAppMasterPermission_normalUser_withAssignRole_andManageAppMaster() {
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
    List<Permission> requiredPermissions = Collections.singletonList(
        new Permission(PermissionType.ASSIGN_ROLE, APP_ID));
    when(rolePermissionService.hasAnyPermission(USER_ID, requiredPermissions)).thenReturn(true);

    when(systemRoleManagerService.hasManageAppMasterPermission(USER_ID, APP_ID)).thenReturn(true);

    assertThat(validator.hasManageAppMasterPermission(APP_ID)).isTrue();
  }

  @Test
  void hasManageAppMasterPermission_normalUser_withoutManageAppMaster() {
    when(rolePermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
    List<Permission> requiredPermissions = Collections.singletonList(
            new Permission(PermissionType.ASSIGN_ROLE, APP_ID));
    when(rolePermissionService.hasAnyPermission(USER_ID, requiredPermissions)).thenReturn(true);
    when(systemRoleManagerService.hasManageAppMasterPermission(USER_ID, APP_ID)).thenReturn(false);

    assertThat(validator.hasManageAppMasterPermission(APP_ID)).isFalse();
  }


  @Test
  void hasPermissions_match() {
    List<Permission> requiredPerms = Lists.newArrayList(new Permission(), new Permission());
    when(rolePermissionService.hasAnyPermission(USER_ID, requiredPerms)).thenReturn(true);

    assertThat(validator.hasPermissions(requiredPerms)).isTrue();
  }

  @Test
  void hasPermissions_notMatch() {
    List<Permission> requiredPerms = Lists.newArrayList(new Permission(), new Permission());
    when(rolePermissionService.hasAnyPermission(USER_ID, requiredPerms)).thenReturn(false);

    assertThat(validator.hasPermissions(requiredPerms)).isFalse();
  }

  @Test
  void hasPermissions_emptyList() {
    assertThat(validator.hasPermissions(Collections.emptyList())).isFalse();
  }

}