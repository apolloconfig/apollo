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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.dto.OpenUserDTO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;
import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OpenAPI UserController
 *
 * @author dreamweaver
 */
@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

  @Mock
  private SpringSecurityUserService userService;

  @Mock
  private AuthUserPasswordChecker passwordChecker;

  private UserController userController;

  private final Gson gson = new Gson();

  @Before
  public void setUp() {
    userController = new UserController(userService, passwordChecker);
  }

  @Test
  public void testCreateUser_Success() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("testuser@example.com");
    openUserDTO.setUserDisplayName("Test User");
    openUserDTO.setEnabled(1);

    CheckResult checkResult = new CheckResult(true, "");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(checkResult);
    doNothing().when(userService).create(any(UserPO.class));

    UserInfo createdUser = new UserInfo();
    createdUser.setUserId("testuser");
    createdUser.setName("Test User");
    createdUser.setEmail("testuser@example.com");
    when(userService.findByUserId("testuser")).thenReturn(createdUser);

    // Act
    ResponseEntity<UserInfo> response = userController.createUser(openUserDTO);

    // Assert
    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals("testuser", response.getBody().getUserId());

    // Verify
    ArgumentCaptor<UserPO> userPOCaptor = ArgumentCaptor.forClass(UserPO.class);
    verify(userService, times(1)).create(userPOCaptor.capture());

    UserPO capturedUser = userPOCaptor.getValue();
    assertEquals("testuser", capturedUser.getUsername());
    assertEquals("testuser@example.com", capturedUser.getEmail());
    assertEquals("Test User", capturedUser.getUserDisplayName());
    assertEquals(1, capturedUser.getEnabled());
    assertNotNull(capturedUser.getPassword());
  }

  @Test
  public void testCreateUser_WithDefaultValues() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser2");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("testuser2@example.com");
    // Not setting userDisplayName and enabled

    CheckResult checkResult = new CheckResult(true, "");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(checkResult);
    doNothing().when(userService).create(any(UserPO.class));

    UserInfo createdUser = new UserInfo();
    createdUser.setUserId("testuser2");
    when(userService.findByUserId("testuser2")).thenReturn(createdUser);

    // Act
    ResponseEntity<UserInfo> response = userController.createUser(openUserDTO);

    // Assert
    assertEquals(200, response.getStatusCodeValue());

    // Verify default values are set
    ArgumentCaptor<UserPO> userPOCaptor = ArgumentCaptor.forClass(UserPO.class);
    verify(userService, times(1)).create(userPOCaptor.capture());

    UserPO capturedUser = userPOCaptor.getValue();
    assertEquals("testuser2", capturedUser.getUserDisplayName()); // Should default to username
    assertEquals(1, capturedUser.getEnabled()); // Should default to 1
  }

  @Test
  public void testCreateUser_EmptyUsername() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("test@example.com");

    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.createUser(openUserDTO));

    // Verify that create was never called
    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_EmptyPassword() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("");
    openUserDTO.setEmail("test@example.com");

    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.createUser(openUserDTO));

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_EmptyEmail() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("");

    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.createUser(openUserDTO));

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_WeakPassword() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("weak");
    openUserDTO.setEmail("test@example.com");

    CheckResult checkResult = new CheckResult(false, "Password is too weak");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(checkResult);

    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.createUser(openUserDTO));

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_UserAlreadyExists() {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("existinguser");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("test@example.com");

    CheckResult checkResult = new CheckResult(true, "");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(checkResult);
    doThrow(BadRequestException.userAlreadyExists("existinguser")).when(userService)
        .create(any(UserPO.class));

    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.createUser(openUserDTO));
  }


  @Test
  public void testGetUserByUserId_Success() {
    // Arrange
    String userId = "testuser";
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    userInfo.setName("Test User");
    userInfo.setEmail("testuser@example.com");

    when(userService.findByUserId(userId)).thenReturn(userInfo);

    // Act
    ResponseEntity<UserInfo> response = userController.getUserByUserId(userId);

    // Assert
    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(userId, response.getBody().getUserId());
    assertEquals("Test User", response.getBody().getName());
    assertEquals("testuser@example.com", response.getBody().getEmail());

    verify(userService, times(1)).findByUserId(userId);
  }

  @Test
  public void testGetUserByUserId_UserNotFound() {
    // Arrange
    String userId = "nonexistent";
    when(userService.findByUserId(userId)).thenReturn(null);

    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.getUserByUserId(userId));

    verify(userService, times(1)).findByUserId(userId);
  }


  @Test
  public void testSearchUsers_Success() {
    // Arrange
    UserInfo user1 = new UserInfo();
    user1.setUserId("user1");
    user1.setName("User One");

    UserInfo user2 = new UserInfo();
    user2.setUserId("user2");
    user2.setName("User Two");

    List<UserInfo> users = Arrays.asList(user1, user2);
    when(userService.searchUsers(anyString(), anyInt(), anyInt(), anyBoolean())).thenReturn(users);

    // Act
    ResponseEntity<List<UserInfo>> response = userController.searchUsers("user", false, 0, 10);

    // Assert
    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(2, response.getBody().size());
    assertEquals("user1", response.getBody().get(0).getUserId());
    assertEquals("user2", response.getBody().get(1).getUserId());

    verify(userService, times(1)).searchUsers("user", 0, 10, false);
  }

  @Test
  public void testSearchUsers_WithIncludeInactiveUsers() {
    // Arrange
    when(userService.searchUsers(anyString(), anyInt(), anyInt(), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // Act
    ResponseEntity<List<UserInfo>> response = userController.searchUsers("test", true, 0, 20);

    // Assert
    assertEquals(200, response.getStatusCodeValue());

    verify(userService, times(1)).searchUsers("test", 0, 20, true);
  }

  @Test
  public void testSearchUsers_DefaultParameters() {
    // Arrange
    when(userService.searchUsers(anyString(), anyInt(), anyInt(), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // Act
    ResponseEntity<List<UserInfo>> response = userController.searchUsers("", false, 0, 10);

    // Assert
    assertEquals(200, response.getStatusCodeValue());

    // Verify default values are used
    verify(userService, times(1)).searchUsers("", 0, 10, false);
  }

  @Test
  public void testSearchUsers_InvalidLimit_TooHigh() {
    // Act & Assert
    assertThrows(BadRequestException.class,
        () -> userController.searchUsers("test", false, 0, 101));

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }

  @Test
  public void testSearchUsers_InvalidLimit_Zero() {
    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.searchUsers("test", false, 0, 0));

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }

  @Test
  public void testSearchUsers_InvalidLimit_Negative() {
    // Act & Assert
    assertThrows(BadRequestException.class, () -> userController.searchUsers("test", false, 0, -1));

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }

  @Test
  public void testSearchUsers_InvalidOffset() {
    // Act & Assert
    assertThrows(BadRequestException.class,
        () -> userController.searchUsers("test", false, -1, 10));

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }
}
