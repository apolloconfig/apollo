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
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for OpenAPI UserController
 *
 * @author dreamweaver
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"spring.profiles.active=github,auth"})
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SpringSecurityUserService userService;

  @MockBean
  private AuthUserPasswordChecker passwordChecker;

  @MockBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  private final Gson gson = new Gson();

  @Before
  public void setUp() {
    // Mock super admin permission by default
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);
  }

  @Test
  public void testCreateUser_Success() throws Exception {
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

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isOk());

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
  public void testCreateUser_WithDefaultValues() throws Exception {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser2");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("testuser2@example.com");
    // Not setting userDisplayName and enabled

    CheckResult checkResult = new CheckResult(true, "");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(checkResult);
    doNothing().when(userService).create(any(UserPO.class));

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isOk());

    // Verify default values are set
    ArgumentCaptor<UserPO> userPOCaptor = ArgumentCaptor.forClass(UserPO.class);
    verify(userService, times(1)).create(userPOCaptor.capture());

    UserPO capturedUser = userPOCaptor.getValue();
    assertEquals("testuser2", capturedUser.getUserDisplayName()); // Should default to username
    assertEquals(1, capturedUser.getEnabled()); // Should default to 1
  }

  @Test
  public void testCreateUser_EmptyUsername() throws Exception {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("test@example.com");

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isBadRequest());

    // Verify that create was never called
    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_EmptyPassword() throws Exception {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("");
    openUserDTO.setEmail("test@example.com");

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isBadRequest());

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_EmptyEmail() throws Exception {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("");

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isBadRequest());

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_WeakPassword() throws Exception {
    // Arrange
    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("weak");
    openUserDTO.setEmail("test@example.com");

    CheckResult checkResult = new CheckResult(false, "Password is too weak");
    when(passwordChecker.checkWeakPassword(anyString())).thenReturn(checkResult);

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isBadRequest());

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testCreateUser_UserAlreadyExists() throws Exception {
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
    mockMvc.perform(post("/openapi/v1/users").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(openUserDTO))).andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateUser_NoSuperAdminPermission() throws Exception {
    // Arrange
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    OpenUserDTO openUserDTO = new OpenUserDTO();
    openUserDTO.setUsername("testuser");
    openUserDTO.setPassword("StrongP@ssw0rd");
    openUserDTO.setEmail("test@example.com");

    // Act & Assert
    mockMvc.perform(post("/openapi/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gson.toJson(openUserDTO)))
        .andExpect(status().isForbidden());

    verify(userService, times(0)).create(any(UserPO.class));
  }

  @Test
  public void testGetUserByUserId_Success() throws Exception {
    // Arrange
    String userId = "testuser";
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    userInfo.setName("Test User");
    userInfo.setEmail("testuser@example.com");

    when(userService.findByUserId(userId)).thenReturn(userInfo);

    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users/" + userId)).andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.name").value("Test User"))
        .andExpect(jsonPath("$.email").value("testuser@example.com"));

    verify(userService, times(1)).findByUserId(userId);
  }

  @Test
  public void testGetUserByUserId_UserNotFound() throws Exception {
    // Arrange
    String userId = "nonexistent";
    when(userService.findByUserId(userId)).thenReturn(null);

    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users/" + userId)).andExpect(status().isBadRequest());

    verify(userService, times(1)).findByUserId(userId);
  }

  @Test
  public void testGetUserByUserId_NoSuperAdminPermission() throws Exception {
    // Arrange
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users/testuser"))
        .andExpect(status().isForbidden());

    verify(userService, times(0)).findByUserId(anyString());
  }

  @Test
  public void testSearchUsers_Success() throws Exception {
    // Arrange
    UserInfo user1 = new UserInfo();
    user1.setUserId("user1");
    user1.setName("User One");

    UserInfo user2 = new UserInfo();
    user2.setUserId("user2");
    user2.setName("User Two");

    List<UserInfo> users = Arrays.asList(user1, user2);
    when(userService.searchUsers(anyString(), anyInt(), anyInt(), anyBoolean())).thenReturn(users);

    // Act & Assert
    mockMvc
        .perform(get("/openapi/v1/users").param("keyword", "user").param("offset", "0")
            .param("limit", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].userId").value("user1"))
        .andExpect(jsonPath("$[1].userId").value("user2"));

    verify(userService, times(1)).searchUsers("user", 0, 10, false);
  }

  @Test
  public void testSearchUsers_WithIncludeInactiveUsers() throws Exception {
    // Arrange
    when(userService.searchUsers(anyString(), anyInt(), anyInt(), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users")
            .param("keyword", "test")
            .param("includeInactiveUsers", "true")
            .param("offset", "0")
            .param("limit", "20"))
        .andExpect(status().isOk());

    verify(userService, times(1)).searchUsers("test", 0, 20, true);
  }

  @Test
  public void testSearchUsers_DefaultParameters() throws Exception {
    // Arrange
    when(userService.searchUsers(anyString(), anyInt(), anyInt(), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users"))
        .andExpect(status().isOk());

    // Verify default values are used
    verify(userService, times(1)).searchUsers("", 0, 10, false);
  }

  @Test
  public void testSearchUsers_InvalidLimit() throws Exception {
    // Act & Assert - limit too high
    mockMvc.perform(get("/openapi/v1/users").param("limit", "101"))
        .andExpect(status().isBadRequest());

    // Act & Assert - limit zero
    mockMvc.perform(get("/openapi/v1/users").param("limit", "0"))
        .andExpect(status().isBadRequest());

    // Act & Assert - limit negative
    mockMvc.perform(get("/openapi/v1/users").param("limit", "-1"))
        .andExpect(status().isBadRequest());

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }

  @Test
  public void testSearchUsers_InvalidOffset() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users").param("offset", "-1"))
        .andExpect(status().isBadRequest());

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }

  @Test
  public void testSearchUsers_NoSuperAdminPermission() throws Exception {
    // Arrange
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    // Act & Assert
    mockMvc.perform(get("/openapi/v1/users"))
        .andExpect(status().isForbidden());

    verify(userService, times(0)).searchUsers(anyString(), anyInt(), anyInt(), anyBoolean());
  }
}
