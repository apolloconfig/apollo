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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.model.OpenUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenUserInfoDTO;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;
import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests User Management OpenAPI endpoints.
 */
@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

  @InjectMocks
  private UserController userController;

  @Mock
  private SpringSecurityUserService userService;

  @Mock
  private AuthUserPasswordChecker passwordChecker;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private OpenApiOperatorResolver operatorResolver;

  @BeforeEach
  public void setUp() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
  }

  @AfterEach
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void getCurrentUserShouldReturnPortalSessionUser() {
    UserInfo currentUser = new UserInfo();
    currentUser.setUserId("jason");
    currentUser.setName("Jason");
    currentUser.setEmail("jason@example.com");
    currentUser.setEnabled(1);
    when(userInfoHolder.getUser()).thenReturn(currentUser);

    ResponseEntity<OpenUserInfoDTO> response = userController.getCurrentUser();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals("jason", response.getBody().getUserId());
    assertEquals("Jason", response.getBody().getName());
    assertEquals("jason@example.com", response.getBody().getEmail());
    assertEquals(Integer.valueOf(1), response.getBody().getEnabled());
  }

  @Test
  public void searchUsersShouldDelegateToUserService() {
    UserInfo user = new UserInfo("jason");
    when(userService.searchUsers("ja", 2, 20, true)).thenReturn(Collections.singletonList(user));

    ResponseEntity<List<OpenUserInfoDTO>> response = userController.searchUsers("ja", true, 2, 20);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("jason", response.getBody().get(0).getUserId());
    verify(userService).searchUsers("ja", 2, 20, true);
  }

  @Test
  public void createOrUpdateUserShouldCreateViaSpringSecurityService() {
    OpenUserDTO user = new OpenUserDTO();
    user.setUsername("jason");
    user.setUserDisplayName("Jason");
    user.setPassword("password");
    user.setEmail("jason@example.com");
    user.setEnabled(1);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(new CheckResult(true, ""));

    ResponseEntity<Void> response = userController.createOrUpdateUser(user, true, "ignored");

    assertEquals(200, response.getStatusCode().value());
    ArgumentCaptor<UserPO> captor = ArgumentCaptor.forClass(UserPO.class);
    verify(userService).create(captor.capture());
    assertEquals("jason", captor.getValue().getUsername());
    assertEquals("Jason", captor.getValue().getUserDisplayName());
    assertEquals("password", captor.getValue().getPassword());
    assertEquals("jason@example.com", captor.getValue().getEmail());
    assertEquals(1, captor.getValue().getEnabled());
    verifyNoInteractions(operatorResolver);
  }

  @Test
  public void createOrUpdateUserShouldRejectUnauthorizedUserUpdate() {
    OpenUserDTO user = new OpenUserDTO();
    user.setUsername("other");
    user.setPassword("password");
    user.setEnabled(1);
    UserInfo currentUser = new UserInfo("jason");
    when(userInfoHolder.getUser()).thenReturn(currentUser);

    assertThrows(AccessDeniedException.class,
        () -> userController.createOrUpdateUser(user, false, "ignored"));

    verifyNoInteractions(passwordChecker, userService, operatorResolver);
  }

  @Test
  public void searchUsersShouldRejectConsumerTokenWithoutManageUsersPermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasManageUsersPermission()).thenReturn(false);

    assertThrows(AccessDeniedException.class, () -> userController.searchUsers("ja", false, 0, 10));

    verify(unifiedPermissionValidator).hasManageUsersPermission();
    verifyNoInteractions(userService);
  }

  @Test
  public void searchUsersShouldAllowConsumerTokenWithManageUsersPermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasManageUsersPermission()).thenReturn(true);
    UserInfo user = new UserInfo("jason");
    when(userService.searchUsers("ja", 0, 10, false)).thenReturn(Collections.singletonList(user));

    ResponseEntity<List<OpenUserInfoDTO>> response = userController.searchUsers("ja", false, 0, 10);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("jason", response.getBody().get(0).getUserId());
    verify(userService).searchUsers("ja", 0, 10, false);
  }

  @Test
  public void getUserByUserIdShouldAllowConsumerTokenWithManageUsersPermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasManageUsersPermission()).thenReturn(true);
    UserInfo user = new UserInfo("jason");
    user.setName("Jason");
    when(userService.findByUserId("jason")).thenReturn(user);

    ResponseEntity<OpenUserInfoDTO> response = userController.getUserByUserId("jason");

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals("jason", response.getBody().getUserId());
    assertEquals("Jason", response.getBody().getName());
  }

  @Test
  public void getUserByUserIdShouldRejectUnknownUser() {
    when(userService.findByUserId("missing")).thenReturn(null);

    assertThrows(BadRequestException.class, () -> userController.getUserByUserId("missing"));
  }

  @Test
  public void createOrUpdateUserShouldAllowConsumerTokenWithManageUsersPermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    OpenUserDTO user = new OpenUserDTO();
    user.setUsername("created");
    user.setUserDisplayName("Created User");
    user.setPassword("password");
    user.setEmail("created@example.com");
    user.setEnabled(1);
    when(unifiedPermissionValidator.hasManageUsersPermission()).thenReturn(true);
    when(operatorResolver.resolve("operator")).thenReturn("operator");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(new CheckResult(true, ""));

    ResponseEntity<Void> response = userController.createOrUpdateUser(user, true, "operator");

    assertEquals(200, response.getStatusCode().value());
    ArgumentCaptor<UserPO> captor = ArgumentCaptor.forClass(UserPO.class);
    verify(userService).create(captor.capture());
    verify(operatorResolver).resolve("operator");
    assertEquals("created", captor.getValue().getUsername());
  }

  @Test
  public void createOrUpdateUserShouldRejectConsumerTokenWithoutManageUsersPermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    OpenUserDTO user = new OpenUserDTO();
    user.setUsername("created");
    user.setPassword("password");
    user.setEnabled(1);
    when(unifiedPermissionValidator.hasManageUsersPermission()).thenReturn(false);

    assertThrows(AccessDeniedException.class,
        () -> userController.createOrUpdateUser(user, true, "operator"));

    verify(unifiedPermissionValidator).hasManageUsersPermission();
    verifyNoMoreInteractions(unifiedPermissionValidator);
    verifyNoInteractions(operatorResolver, passwordChecker, userService);
  }

  @Test
  public void changeUserEnabledShouldAllowConsumerTokenWithManageUsersPermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    OpenUserDTO user = new OpenUserDTO();
    user.setUsername("created");
    user.setEnabled(0);
    when(unifiedPermissionValidator.hasManageUsersPermission()).thenReturn(true);
    when(operatorResolver.resolve("operator")).thenReturn("operator");

    ResponseEntity<Void> response = userController.changeUserEnabled(user, "operator");

    assertEquals(200, response.getStatusCode().value());
    ArgumentCaptor<UserPO> captor = ArgumentCaptor.forClass(UserPO.class);
    verify(userService).changeEnabled(captor.capture());
    verify(operatorResolver).resolve("operator");
    assertEquals("created", captor.getValue().getUsername());
    assertEquals(0, captor.getValue().getEnabled());
  }
}
