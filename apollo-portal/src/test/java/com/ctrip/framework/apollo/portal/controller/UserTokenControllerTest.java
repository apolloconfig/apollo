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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Unit tests for portal user token management endpoints.
 */
@ExtendWith(MockitoExtension.class)
class UserTokenControllerTest {

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private UserTokenService userTokenService;

  @Mock
  private PortalConfig portalConfig;

  private UserTokenController controller;

  @BeforeEach
  void setUp() {
    controller = new UserTokenController(userInfoHolder, userTokenService, portalConfig);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  void adminListDelegatesToAdminService() {
    usePortalUserSession("root");
    when(userTokenService.findUserTokensForAdmin("ali", "active"))
        .thenReturn(Collections.emptyList());

    controller.adminList("ali", "active");

    verify(userTokenService).findUserTokensForAdmin("ali", "active");
  }

  @Test
  void adminRevokeDelegatesWithCurrentOperator() {
    usePortalUserSession("root");

    controller.adminRevoke(1L);

    verify(userTokenService).revokeTokenForAdmin(1L, "root");
  }

  @Test
  void adminDeleteDelegatesWithCurrentOperator() {
    usePortalUserSession("root");

    controller.adminDelete(1L);

    verify(userTokenService).deleteTokenForAdmin(1L, "root");
  }

  @Test
  void adminListRejectsNonPortalUserSession() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);

    assertThrows(AccessDeniedException.class, () -> controller.adminList(null, "all"));
  }

  @Test
  void adminListRejectsMissingPortalUserContext() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    assertThrows(AccessDeniedException.class, () -> controller.adminList(null, "all"));
  }

  @Test
  void adminEndpointsRequireSuperAdminPreAuthorize() throws NoSuchMethodException {
    assertSuperAdminPreAuthorize(
        UserTokenController.class.getMethod("adminList", String.class, String.class));
    assertSuperAdminPreAuthorize(UserTokenController.class.getMethod("adminRevoke", long.class));
    assertSuperAdminPreAuthorize(UserTokenController.class.getMethod("adminDelete", long.class));
  }

  private void assertSuperAdminPreAuthorize(Method method) {
    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

    assertNotNull(preAuthorize);
    assertEquals("@unifiedPermissionValidator.isSuperAdmin()", preAuthorize.value());
  }

  private void usePortalUserSession(String userId) {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    when(userInfoHolder.getUser()).thenReturn(userInfo);
  }
}
