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
package com.ctrip.framework.apollo.openapi.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.service.ConsumerRolePermissionService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.service.SystemRoleManagerService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the ConsumerPermissionValidator#hasPermissions method
 */
@ExtendWith(MockitoExtension.class)
public class ConsumerPermissionValidatorTest {

  private static final long CONSUMER_ID = 123L;
  private static final String TARGET_ID = "targetId";
  private static final String PERMISSION_TYPE = "permissionType";
  private static final String APP_ID = "testAppId";
  @Mock
  private ConsumerRolePermissionService permissionService;
  @Mock
  private ConsumerAuthUtil consumerAuthUtil;
  private ConsumerPermissionValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new ConsumerPermissionValidator(permissionService, consumerAuthUtil);
    // Default mock behavior
    lenient().when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
  }

  /**
   * Test that hasCreateAppNamespacePermission method throws UnsupportedOperationException
   */
  @Test
  public void testHasCreateAppNamespacePermission_ThrowsUnsupportedOperationException() {
    // Arrange
    String appId = "testAppId";
    AppNamespace appNamespace = new AppNamespace();

    // Act & Assert
    assertThrows(UnsupportedOperationException.class, () -> {
      validator.hasCreateAppNamespacePermission(appId, appNamespace);
    });
  }

  /**
   * Test that isSuperAdmin method always returns false
   */
  @Test
  public void testIsSuperAdmin_ReturnsFalse() {
    // Act
    boolean result = validator.isSuperAdmin();

    // Assert
    assertFalse(result);
  }

  /**
   * Test that shouldHideConfigToCurrentUser method throws UnsupportedOperationException
   */
  @Test
  public void testShouldHideConfigToCurrentUser_ThrowsUnsupportedOperationException() {
    // Arrange
    String appId = "testAppId";
    String env = "DEV";
    String clusterName = "default";
    String namespaceName = "application";

    // Act & Assert
    assertThrows(UnsupportedOperationException.class, () -> {
      validator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName);
    });
  }

  /**
   * TC005: Consumer has create application permission, should return true
   */
  @Test
  public void testHasCreateApplicationPermission_UserHasPermission_ReturnsTrue() {
    // Arrange
    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
    when(permissionService.consumerHasPermission(
        CONSUMER_ID,
        PermissionType.CREATE_APPLICATION,
        SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID))
        .thenReturn(true);

    // Act
    boolean result = validator.hasCreateApplicationPermission();

    // Assert
    assertTrue(result);
    verify(consumerAuthUtil, times(1)).retrieveConsumerIdFromCtx();
    verify(permissionService, times(1))
        .consumerHasPermission(CONSUMER_ID, PermissionType.CREATE_APPLICATION,
            SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID);
  }

  /**
   * TC006: Consumer does not have create application permission, should return false
   */
  @Test
  public void testHasCreateApplicationPermission_UserHasNoPermission_ReturnsFalse() {
    // Arrange
    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
    when(permissionService.consumerHasPermission(
        CONSUMER_ID,
        PermissionType.CREATE_APPLICATION,
        SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID))
        .thenReturn(false);

    // Act
    boolean result = validator.hasCreateApplicationPermission();

    // Assert
    assertFalse(result);
    verify(consumerAuthUtil, times(1)).retrieveConsumerIdFromCtx();
    verify(permissionService, times(1))
        .consumerHasPermission(CONSUMER_ID, PermissionType.CREATE_APPLICATION,
            SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID);
  }

  /**
   * TC007: retrieveConsumerIdFromCtx throws exception, should throw exception
   */
  @Test
  public void testHasCreateApplicationPermission_RetrieveConsumerIdThrowsException_ThrowsException() {
    // Arrange
    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenThrow(
        new RuntimeException("Consumer ID not found"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      validator.hasCreateApplicationPermission();
    });

    verify(consumerAuthUtil, times(1)).retrieveConsumerIdFromCtx();
    verify(permissionService, never())
        .consumerHasPermission(anyLong(), anyString(), anyString());
  }

  /**
   * TC008: consumerHasPermission throws exception, should throw exception
   */
  @Test
  public void testHasCreateApplicationPermission_ConsumerHasPermissionThrowsException_ThrowsException() {
    // Arrange
    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
    when(permissionService.consumerHasPermission(
        CONSUMER_ID,
        PermissionType.CREATE_APPLICATION,
        SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID))
        .thenThrow(new RuntimeException("Permission check failed"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      validator.hasCreateApplicationPermission();
    });

    verify(consumerAuthUtil, times(1)).retrieveConsumerIdFromCtx();
    verify(permissionService, times(1))
        .consumerHasPermission(CONSUMER_ID, PermissionType.CREATE_APPLICATION,
            SystemRoleManagerService.SYSTEM_PERMISSION_TARGET_ID);
  }

  @Test
  public void testHasManageAppMasterPermission_NotSupported_ThrowsException() {
    // Act & Assert
    assertThrows(UnsupportedOperationException.class, () -> {
      validator.hasManageAppMasterPermission(APP_ID);
    });
  }


  /**
   * TC001: Consumer has at least one of the required permissions, should return true
   */
  @Test
  public void testHasPermissions_UserHasPermission_ReturnsTrue() {
    // 创建 Permission 对象列表
    List<Permission> requiredPerms = Arrays.asList(
        new Permission("a", "b"),
        new Permission("c", "d")
    );

    // 模拟 permissionService.hasAnyPermission 返回 true
    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
    when(permissionService.hasAnyPermission(CONSUMER_ID, requiredPerms)).thenReturn(true);

    boolean result = validator.hasPermissions(requiredPerms);

    assertTrue(result);
    verify(consumerAuthUtil, times(1)).retrieveConsumerIdFromCtx();
    verify(permissionService, times(1)).hasAnyPermission(CONSUMER_ID, requiredPerms);
  }

  /**
   * TC002: Consumer does not have any of the required permissions, should return false
   */
  @Test
  public void testHasPermissions_UserHasNoPermission_ReturnsFalse() {
    List<Permission> requiredPerms = Arrays.asList(
        new Permission("a", "b"),
        new Permission("c", "d")
    );

    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(CONSUMER_ID);
    when(permissionService.hasAnyPermission(CONSUMER_ID, requiredPerms)).thenReturn(false);

    boolean result = validator.hasPermissions(requiredPerms);

    assertFalse(result);
  }

  /**
   * TC003: requiredPerms is an empty list, should return false
   */
  @Test
  public void testHasPermissions_RequiredPermsIsEmpty_ReturnsFalse() {
    List<Permission> requiredPerms = Collections.emptyList();

    boolean result = validator.hasPermissions(requiredPerms);

    assertFalse(result);
    verify(permissionService, never()).hasAnyPermission(anyLong(), anyList());
  }

  /**
   * TC004: requiredPerms is null, should return false
   */
  @Test
  public void testHasPermissions_RequiredPermsIsNull_ReturnsFalse() {
    boolean result = validator.hasPermissions(null);

    assertFalse(result);
    verify(permissionService, never()).hasAnyPermission(anyLong(), anyList());
  }

}