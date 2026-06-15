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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.openapi.model.OpenCreateUserTokenRequest;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenCreateRequest;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenInfo;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.CommitService;
import com.ctrip.framework.apollo.portal.service.ConfigsExportService;
import com.ctrip.framework.apollo.portal.service.ConfigsImportService;
import com.ctrip.framework.apollo.portal.service.FavoriteService;
import com.ctrip.framework.apollo.portal.service.GlobalSearchService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import com.ctrip.framework.apollo.portal.service.ServerConfigService;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Unit tests for Portal user-token management endpoints exposed through OpenAPI.
 */
@ExtendWith(MockitoExtension.class)
class PortalUserTokenManagementControllerTest {

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private UserTokenService userTokenService;

  @Mock
  private PortalConfig portalConfig;

  private PortalManagementController controller;

  @BeforeEach
  void setUp() {
    controller = new PortalManagementController(mock(ApolloAuditLogApi.class),
        mock(ApolloAuditProperties.class), mock(CommitService.class),
        mock(UnifiedPermissionValidator.class), portalConfig, mock(ConsumerService.class),
        mock(FavoriteService.class), mock(GlobalSearchService.class),
        mock(ReleaseHistoryService.class), mock(ServerConfigService.class), userInfoHolder,
        mock(ConfigsExportService.class), mock(ConfigsImportService.class),
        mock(NamespaceService.class), mock(AppService.class), mock(PortalSettings.class),
        mock(RestTemplateFactory.class), mock(PortalMetaDomainService.class), new ObjectMapper(),
        userTokenService);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  void listUserTokensDelegatesToCurrentUser() {
    usePortalUserSession("apollo");
    when(userTokenService.findUserTokens("apollo")).thenReturn(Collections.emptyList());

    controller.listUserTokens();

    verify(userTokenService).findUserTokens("apollo");
  }

  @Test
  void createUserTokenDelegatesToCurrentUser() {
    usePortalUserSession("apollo");
    OpenCreateUserTokenRequest request = new OpenCreateUserTokenRequest("ai-agent");
    request.setOperations(Collections.singleton("ConfigRead"));
    request.setRateLimit(10);
    when(userTokenService.createToken(any(UserTokenCreateRequest.class), eq("apollo")))
        .thenReturn(createUserTokenInfo());

    assertEquals("token-value", controller.createUserToken(request).getBody().getTokenValue());

    ArgumentCaptor<UserTokenCreateRequest> requestCaptor =
        ArgumentCaptor.forClass(UserTokenCreateRequest.class);
    verify(userTokenService).createToken(requestCaptor.capture(), eq("apollo"));
    Assertions.assertEquals("ai-agent", requestCaptor.getValue().getName());
    Assertions.assertEquals(Collections.singleton("ConfigRead"),
        requestCaptor.getValue().getOperations());
    Assertions.assertEquals(10, requestCaptor.getValue().getRateLimit());
  }

  @Test
  void adminListDelegatesToAdminService() {
    usePortalUserSession("root");
    when(userTokenService.findUserTokensForAdmin("ali", "active"))
        .thenReturn(Collections.emptyList());

    controller.adminListUserTokens("ali", "active");

    verify(userTokenService).findUserTokensForAdmin("ali", "active");
  }

  @Test
  void adminRevokeDelegatesWithCurrentOperator() {
    usePortalUserSession("root");

    controller.adminRevokeUserToken(1L);

    verify(userTokenService).revokeTokenForAdmin(1L, "root");
  }

  @Test
  void adminDeleteDelegatesWithCurrentOperator() {
    usePortalUserSession("root");

    controller.adminDeleteUserToken(1L);

    verify(userTokenService).deleteTokenForAdmin(1L, "root");
  }

  @Test
  void adminListRejectsNonPortalUserSession() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);

    assertThrows(AccessDeniedException.class, () -> controller.adminListUserTokens(null, "all"));
  }

  @Test
  void adminListRejectsMissingPortalUserContext() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    assertThrows(AccessDeniedException.class, () -> controller.adminListUserTokens(null, "all"));
  }

  @Test
  void adminEndpointsRequireSuperAdminPreAuthorize() throws NoSuchMethodException {
    assertSuperAdminPreAuthorize(PortalManagementController.class.getMethod("adminListUserTokens",
        String.class, String.class));
    assertSuperAdminPreAuthorize(
        PortalManagementController.class.getMethod("adminRevokeUserToken", Long.class));
    assertSuperAdminPreAuthorize(
        PortalManagementController.class.getMethod("adminDeleteUserToken", Long.class));
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

  private UserTokenInfo createUserTokenInfo() {
    UserTokenInfo userTokenInfo = new UserTokenInfo();
    userTokenInfo.setId(1L);
    userTokenInfo.setUserId("apollo");
    userTokenInfo.setName("ai-agent");
    userTokenInfo.setTokenPrefix("abc123");
    userTokenInfo.setTokenValue("token-value");
    userTokenInfo.setStatus("active");
    userTokenInfo.setOperations(Collections.singleton("ConfigRead"));
    userTokenInfo.setAppIds(Collections.emptySet());
    userTokenInfo.setEnvs(Collections.emptySet());
    userTokenInfo.setNamespaces(Collections.emptyList());
    userTokenInfo.setRateLimit(10);
    userTokenInfo.setExpires(new Date(System.currentTimeMillis() + 3600_000));
    userTokenInfo.setDataChangeCreatedTime(new Date());
    return userTokenInfo;
  }
}
