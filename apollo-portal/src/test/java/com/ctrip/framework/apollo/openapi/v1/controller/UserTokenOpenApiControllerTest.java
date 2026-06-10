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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.openapi.model.OpenUserTokenCurrentCapability;
import com.ctrip.framework.apollo.openapi.model.OpenUserTokenNamespaceScope;
import com.ctrip.framework.apollo.openapi.model.OpenUserTokenOpenApiAction;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenNamespaceScope;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenScope;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for current user token OpenAPI capability responses.
 */
@ExtendWith(MockitoExtension.class)
public class UserTokenOpenApiControllerTest {

  @Mock
  private UserTokenService userTokenService;

  @Mock
  private UserTokenAuthUtil userTokenAuthUtil;

  private UserTokenOpenApiController controller;

  @BeforeEach
  public void setUp() {
    controller = new UserTokenOpenApiController(userTokenService, userTokenAuthUtil);
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
  }

  @AfterEach
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void currentShouldReturnUserTokenIdentityAndExplicitScope() {
    UserToken userToken = createUserToken();
    UserTokenScope scope = new UserTokenScope();
    scope.setOperations(new LinkedHashSet<>(
        Arrays.asList(UserTokenOperation.CONFIG_READ, UserTokenOperation.CONFIG_MODIFY)));
    scope.setAppIds(new LinkedHashSet<>(Collections.singletonList("app1")));
    scope.setEnvs(new LinkedHashSet<>(Collections.singletonList("DEV")));
    UserTokenNamespaceScope namespace = new UserTokenNamespaceScope();
    namespace.setAppId("app1");
    namespace.setEnv("DEV");
    namespace.setClusterName("default");
    namespace.setNamespaceName("application");
    scope.setNamespaces(Collections.singletonList(namespace));
    when(userTokenAuthUtil.retrieveUserTokenFromCtx()).thenReturn(userToken);
    when(userTokenService.parseScope(userToken)).thenReturn(scope);

    ResponseEntity<OpenUserTokenCurrentCapability> response = controller.getCurrentUserToken();

    assertEquals(200, response.getStatusCode().value());
    OpenUserTokenCurrentCapability body = response.getBody();
    assertNotNull(body);
    assertEquals(UserIdentityConstants.USER_TOKEN, body.getAuthType());
    assertEquals("apollo", body.getUserId());
    assertEquals(8L, body.getTokenId());
    assertEquals("agent", body.getTokenName());
    assertEquals("abc", body.getTokenPrefix());
    assertEquals(Integer.valueOf(90), body.getRateLimit());
    assertFalse(body.getDenyAll());
    assertFalse(body.getAllOperations());
    assertTrue(body.getOperations().contains(UserTokenOperation.CONFIG_READ));
    assertFalse(body.getAllApps());
    assertEquals(Collections.singleton("app1"), body.getAppIds());
    assertFalse(body.getAllEnvs());
    assertEquals(Collections.singleton("DEV"), body.getEnvs());
    assertFalse(body.getAllNamespaces());
    OpenUserTokenNamespaceScope openNamespace = body.getNamespaces().get(0);
    assertEquals(namespace.getAppId(), openNamespace.getAppId());
    assertEquals(namespace.getEnv(), openNamespace.getEnv());
    assertEquals(namespace.getClusterName(), openNamespace.getClusterName());
    assertEquals(namespace.getNamespaceName(), openNamespace.getNamespaceName());
    assertTrue(hasAction(body, "user-token.current"));
    assertTrue(hasAction(body, "user.current"));
    assertTrue(hasAction(body, "app.list"));
    assertTrue(hasAction(body, "item.list"));
    assertTrue(hasAction(body, "item.create"));
    assertFalse(hasAction(body, "release.create"));
    assertFalse(hasAction(body, "namespace.create"));
    assertFalse(hasAction(body, "user.search"));
    OpenUserTokenOpenApiAction appListAction = actionById(body, "app.list");
    assertNotNull(appListAction);
    assertEquals("GET", appListAction.getMethod());
    assertEquals("app", appListAction.getResourceScope());
    assertEquals("ANY", appListAction.getOperationMatch());
    assertTrue(appListAction.getRequiredOperations().contains(UserTokenOperation.CONFIG_READ));
    assertTrue(appListAction.getRequiredOperations().contains(UserTokenOperation.CONFIG_MODIFY));
    assertTrue(appListAction.getGrantedOperations().contains(UserTokenOperation.CONFIG_READ));
    assertTrue(appListAction.getGrantedOperations().contains(UserTokenOperation.CONFIG_MODIFY));
    assertFalse(appListAction.getGrantedOperations().contains(UserTokenOperation.CONFIG_RELEASE));
  }

  @Test
  public void currentShouldMakeUnboundedScopeExplicit() {
    UserToken userToken = createUserToken();
    when(userTokenAuthUtil.retrieveUserTokenFromCtx()).thenReturn(userToken);
    when(userTokenService.parseScope(userToken)).thenReturn(UserTokenScope.allowAll());

    ResponseEntity<OpenUserTokenCurrentCapability> response =
        controller.getCurrentUserTokenCapabilities();

    OpenUserTokenCurrentCapability body = response.getBody();
    assertNotNull(body);
    assertFalse(body.getDenyAll());
    assertTrue(body.getAllOperations());
    assertTrue(body.getAllApps());
    assertTrue(body.getAllEnvs());
    assertTrue(body.getAllNamespaces());
    assertTrue(body.getOperations().isEmpty());
    assertTrue(body.getAppIds().isEmpty());
    assertTrue(body.getEnvs().isEmpty());
    assertTrue(body.getNamespaces().isEmpty());
    assertTrue(hasAction(body, "app.create"));
    assertTrue(hasAction(body, "namespace.create"));
    assertTrue(hasAction(body, "release.create"));
    assertTrue(hasAction(body, "user.search"));
    assertTrue(hasAction(body, "system.root-permission"));
  }

  @Test
  public void currentShouldSeparateReleaseReadAndPublishActions() {
    UserToken userToken = createUserToken();
    UserTokenScope readScope = scopeWithOperations(UserTokenOperation.CONFIG_READ);
    UserTokenScope publishScope = scopeWithOperations(UserTokenOperation.CONFIG_RELEASE);
    when(userTokenAuthUtil.retrieveUserTokenFromCtx()).thenReturn(userToken);

    when(userTokenService.parseScope(userToken)).thenReturn(readScope);
    OpenUserTokenCurrentCapability readBody = controller.getCurrentUserToken().getBody();
    assertNotNull(readBody);
    assertTrue(hasAction(readBody, "release.latest"));
    assertTrue(hasAction(readBody, "release.active-list"));
    assertTrue(hasAction(readBody, "release.get"));
    assertTrue(hasAction(readBody, "release.compare"));
    assertFalse(hasAction(readBody, "release.create"));
    assertFalse(hasAction(readBody, "release.rollback"));
    assertEquals(Collections.singletonList(UserTokenOperation.CONFIG_READ),
        actionById(readBody, "release.latest").getRequiredOperations());
    assertEquals(Collections.singletonList(UserTokenOperation.CONFIG_READ),
        actionById(readBody, "release.latest").getGrantedOperations());

    when(userTokenService.parseScope(userToken)).thenReturn(publishScope);
    OpenUserTokenCurrentCapability publishBody = controller.getCurrentUserToken().getBody();
    assertNotNull(publishBody);
    assertFalse(hasAction(publishBody, "release.latest"));
    assertFalse(hasAction(publishBody, "release.active-list"));
    assertFalse(hasAction(publishBody, "release.get"));
    assertFalse(hasAction(publishBody, "release.compare"));
    assertTrue(hasAction(publishBody, "release.create"));
    assertTrue(hasAction(publishBody, "release.gray-create"));
    assertTrue(hasAction(publishBody, "release.gray-delete"));
    assertTrue(hasAction(publishBody, "release.rollback"));
    assertEquals(Collections.singletonList(UserTokenOperation.CONFIG_RELEASE),
        actionById(publishBody, "release.create").getRequiredOperations());
    assertEquals(Collections.singletonList(UserTokenOperation.CONFIG_RELEASE),
        actionById(publishBody, "release.create").getGrantedOperations());
  }

  @Test
  public void currentShouldExposeGrantedOperationsForAlternativePermissions() {
    UserToken userToken = createUserToken();
    UserTokenScope scope = scopeWithOperations(UserTokenOperation.APP_MANAGE_ROLE);
    when(userTokenAuthUtil.retrieveUserTokenFromCtx()).thenReturn(userToken);
    when(userTokenService.parseScope(userToken)).thenReturn(scope);

    OpenUserTokenCurrentCapability body = controller.getCurrentUserTokenWhoami().getBody();

    assertNotNull(body);
    OpenUserTokenOpenApiAction appUpdateAction = actionById(body, "app.update");
    assertNotNull(appUpdateAction);
    assertTrue(
        appUpdateAction.getRequiredOperations().contains(UserTokenOperation.APP_MANAGE_ROLE));
    assertTrue(appUpdateAction.getRequiredOperations().contains(UserTokenOperation.SYSTEM_ADMIN));
    assertEquals(Collections.singletonList(UserTokenOperation.APP_MANAGE_ROLE),
        appUpdateAction.getGrantedOperations());
  }

  @Test
  public void currentShouldRejectNonUserTokenIdentity() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    assertThrows(AccessDeniedException.class, () -> controller.getCurrentUserToken());
    verifyNoInteractions(userTokenAuthUtil, userTokenService);
  }

  private UserToken createUserToken() {
    UserToken userToken = new UserToken();
    userToken.setId(8L);
    userToken.setUserId("apollo");
    userToken.setName("agent");
    userToken.setTokenPrefix("abc");
    userToken.setRateLimit(90);
    userToken.setExpires(new Date(System.currentTimeMillis() + 60_000));
    userToken.setLastUsedTime(new Date());
    userToken.setDataChangeCreatedTime(new Date());
    return userToken;
  }

  private UserTokenScope scopeWithOperations(String... operations) {
    UserTokenScope scope = new UserTokenScope();
    scope.setOperations(new LinkedHashSet<>(Arrays.asList(operations)));
    return scope;
  }

  private boolean hasAction(OpenUserTokenCurrentCapability capability, String actionId) {
    return actionById(capability, actionId) != null;
  }

  private OpenUserTokenOpenApiAction actionById(OpenUserTokenCurrentCapability capability,
      String actionId) {
    for (OpenUserTokenOpenApiAction action : capability.getActions()) {
      if (actionId.equals(action.getId())) {
        return action;
      }
    }
    return null;
  }
}
