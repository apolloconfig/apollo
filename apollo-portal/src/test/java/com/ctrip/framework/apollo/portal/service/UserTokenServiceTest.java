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
package com.ctrip.framework.apollo.portal.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenCreateRequest;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenInfo;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenScope;
import com.ctrip.framework.apollo.portal.repository.UserTokenAuditRepository;
import com.ctrip.framework.apollo.portal.repository.UserTokenRepository;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for user token lifecycle, validation, and authentication behavior.
 */
@ExtendWith(MockitoExtension.class)
class UserTokenServiceTest {

  @Mock
  private UserTokenRepository userTokenRepository;

  @Mock
  private UserTokenAuditRepository userTokenAuditRepository;

  @Mock
  private UserService userService;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private UserPermissionValidator userPermissionValidator;

  private UserTokenService userTokenService;

  @BeforeEach
  void setUp() {
    userTokenService = new UserTokenService(userTokenRepository, userTokenAuditRepository,
        userService, portalConfig, userPermissionValidator);
  }

  private void setUpCreateTokenStubs() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("apollo");
    userInfo.setEnabled(1);
    when(userService.findByUserId("apollo")).thenReturn(userInfo);
    when(portalConfig.userTokenDefaultExpireDays()).thenReturn(90);
    when(portalConfig.userTokenMaxExpireDays()).thenReturn(365);
    when(userTokenRepository.save(any(UserToken.class))).thenAnswer(invocation -> {
      UserToken token = invocation.getArgument(0);
      if (token.getId() == 0) {
        token.setId(1L);
      }
      return token;
    });
  }

  @Test
  void createTokenStoresHashOnlyAndReturnsTokenValueOnce() {
    setUpCreateTokenStubs();
    UserTokenCreateRequest request = createRequest();

    UserTokenInfo result = userTokenService.createToken(request, "apollo");

    ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
    verify(userTokenRepository).save(captor.capture());
    UserToken saved = captor.getValue();
    assertNotNull(result.getTokenValue());
    assertTrue(result.getTokenValue().startsWith(UserTokenService.TOKEN_PREFIX));
    assertFalse(result.getTokenPrefix().contains("_"));
    assertFalse(saved.getTokenHash().contains(result.getTokenValue()));
    assertNull(saved.getRevokedAt());
  }

  @Test
  void authenticateReturnsTokenForValidHashAndEnabledUser() {
    setUpCreateTokenStubs();
    UserTokenInfo created = userTokenService.createToken(createRequest(), "apollo");
    ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
    verify(userTokenRepository).save(captor.capture());
    UserToken saved = captor.getValue();
    when(userTokenRepository.findTopByTokenPrefixAndExpiresAfterAndRevokedAtIsNull(
        eq(created.getTokenPrefix()), any(Date.class))).thenReturn(saved);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    UserToken authenticated = userTokenService.authenticate(created.getTokenValue(), request);

    assertNotNull(authenticated);
    assertNotNull(authenticated.getLastUsedTime());
  }

  @Test
  void authenticateReturnsNullForInvalidToken() {
    when(userTokenRepository.findTopByTokenPrefixAndExpiresAfterAndRevokedAtIsNull(anyString(),
        any(Date.class))).thenReturn(null);

    MockHttpServletRequest request = new MockHttpServletRequest();

    assertNull(userTokenService.authenticate(UserTokenService.TOKEN_PREFIX + "abc_secret",
        request));
  }

  @Test
  void parseScopeAllowsAllForEmptyJsonScope() {
    UserToken token = new UserToken();
    token.setScopes("{}");

    UserTokenScope scope = userTokenService.parseScope(token);

    assertTrue(scope.allowsOperation(UserTokenOperation.CONFIG_READ));
  }

  @Test
  void parseScopeDeniesAllForInvalidScope() {
    UserToken token = new UserToken();
    token.setScopes("invalid-json");

    UserTokenScope scope = userTokenService.parseScope(token);

    assertFalse(scope.allowsOperation(UserTokenOperation.CONFIG_READ));
  }

  @Test
  void findAvailableOperationsHidesPrivilegedOperationsFromRegularUser() {
    when(userPermissionValidator.hasCreateApplicationPermission()).thenReturn(false);
    when(userPermissionValidator.hasManageUsersPermission()).thenReturn(false);
    when(userPermissionValidator.isSuperAdmin()).thenReturn(false);

    List<String> operations = userTokenService.findAvailableOperations();

    assertTrue(operations.contains(UserTokenOperation.CONFIG_READ));
    assertTrue(operations.contains(UserTokenOperation.APP_MANAGE_ROLE));
    assertFalse(operations.contains(UserTokenOperation.APP_CREATE));
    assertFalse(operations.contains(UserTokenOperation.USER_MANAGE));
    assertFalse(operations.contains(UserTokenOperation.SYSTEM_ADMIN));
  }

  @Test
  void findAvailableOperationsIncludesPrivilegedOperationsForAuthorizedUser() {
    when(userPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    when(userPermissionValidator.hasManageUsersPermission()).thenReturn(true);
    when(userPermissionValidator.isSuperAdmin()).thenReturn(true);

    List<String> operations = userTokenService.findAvailableOperations();

    assertTrue(operations.contains(UserTokenOperation.APP_CREATE));
    assertTrue(operations.contains(UserTokenOperation.USER_MANAGE));
    assertTrue(operations.contains(UserTokenOperation.SYSTEM_ADMIN));
  }

  @Test
  void createTokenRejectsPrivilegedOperationWhenUserDoesNotHavePermission() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("apollo");
    userInfo.setEnabled(1);
    when(userService.findByUserId("apollo")).thenReturn(userInfo);
    when(portalConfig.userTokenDefaultExpireDays()).thenReturn(90);
    when(portalConfig.userTokenMaxExpireDays()).thenReturn(365);
    UserTokenCreateRequest request = createRequest();
    request.setOperations(Collections.singleton(UserTokenOperation.SYSTEM_ADMIN));
    when(userPermissionValidator.isSuperAdmin()).thenReturn(false);

    assertThrows(BadRequestException.class, () -> userTokenService.createToken(request, "apollo"));
  }

  @Test
  void createTokenAllowsPrivilegedOperationWhenUserHasPermission() {
    setUpCreateTokenStubs();
    UserTokenCreateRequest request = createRequest();
    Set<String> operations = new HashSet<>();
    operations.add(UserTokenOperation.CONFIG_READ);
    operations.add(UserTokenOperation.SYSTEM_ADMIN);
    request.setOperations(operations);
    when(userPermissionValidator.isSuperAdmin()).thenReturn(true);

    UserTokenInfo result = userTokenService.createToken(request, "apollo");

    assertNotNull(result.getTokenValue());
  }

  @Test
  void deleteTokenRevokesActiveTokenAndDeletesRecord() {
    UserToken token = new UserToken();
    token.setId(1L);
    token.setUserId("apollo");
    when(userTokenRepository.findByIdAndUserId(1L, "apollo")).thenReturn(token);

    userTokenService.deleteToken(1L, "apollo");

    ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
    verify(userTokenRepository).saveAndFlush(captor.capture());
    UserToken saved = captor.getValue();
    assertNotNull(saved.getRevokedAt());
    assertEquals("apollo", saved.getRevokedBy());
    verify(userTokenRepository).delete(token);
  }

  @Test
  void findUserTokensForAdminFiltersByUserAndActiveStatus() {
    UserToken activeToken = createUserToken(1L, "alice", futureDate());
    activeToken.setLastUsedIp("127.0.0.1");
    activeToken.setLastUsedUserAgent("agent");
    UserToken expiredToken = createUserToken(2L, "alice2", pastDate());
    when(
        userTokenRepository.findByUserIdContainingIgnoreCaseOrderByDataChangeCreatedTimeDesc("ali"))
        .thenReturn(Arrays.asList(activeToken, expiredToken));

    List<UserTokenInfo> tokens = userTokenService.findUserTokensForAdmin(" ali ", "active");

    assertEquals(1, tokens.size());
    assertEquals("alice", tokens.get(0).getUserId());
    assertEquals(UserTokenService.TOKEN_STATUS_ACTIVE, tokens.get(0).getStatus());
    assertEquals("127.0.0.1", tokens.get(0).getLastUsedIp());
    assertEquals("agent", tokens.get(0).getLastUsedUserAgent());
    assertNull(tokens.get(0).getTokenValue());
  }

  @Test
  void findUserTokensForAdminFiltersRevokedStatusAcrossAllUsers() {
    UserToken activeToken = createUserToken(1L, "alice", futureDate());
    UserToken revokedToken = createUserToken(2L, "bob", futureDate());
    revokedToken.setRevokedAt(new Date());
    revokedToken.setRevokedBy("root");
    when(userTokenRepository.findAllByOrderByDataChangeCreatedTimeDesc())
        .thenReturn(Arrays.asList(activeToken, revokedToken));

    List<UserTokenInfo> tokens = userTokenService.findUserTokensForAdmin(null, "revoked");

    assertEquals(1, tokens.size());
    assertEquals("bob", tokens.get(0).getUserId());
    assertEquals(UserTokenService.TOKEN_STATUS_REVOKED, tokens.get(0).getStatus());
    assertEquals("root", tokens.get(0).getRevokedBy());
  }

  @Test
  void findUserTokensForAdminRejectsInvalidStatus() {
    assertThrows(BadRequestException.class,
        () -> userTokenService.findUserTokensForAdmin(null, "invalid"));
  }

  @Test
  void revokeTokenForAdminAllowsOtherUserTokenAndRecordsOperator() {
    UserToken token = createUserToken(1L, "alice", futureDate());
    when(userTokenRepository.findById(1L)).thenReturn(Optional.of(token));

    userTokenService.revokeTokenForAdmin(1L, "root");

    ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
    verify(userTokenRepository).save(captor.capture());
    UserToken saved = captor.getValue();
    assertNotNull(saved.getRevokedAt());
    assertEquals("root", saved.getRevokedBy());
    assertEquals("root", saved.getDataChangeLastModifiedBy());
  }

  @Test
  void deleteTokenForAdminAllowsOtherUserTokenAndDeletesRecord() {
    UserToken token = createUserToken(1L, "alice", futureDate());
    when(userTokenRepository.findById(1L)).thenReturn(Optional.of(token));

    userTokenService.deleteTokenForAdmin(1L, "root");

    ArgumentCaptor<UserToken> captor = ArgumentCaptor.forClass(UserToken.class);
    verify(userTokenRepository).saveAndFlush(captor.capture());
    UserToken saved = captor.getValue();
    assertNotNull(saved.getRevokedAt());
    assertEquals("root", saved.getRevokedBy());
    verify(userTokenRepository).delete(token);
  }

  @Test
  void deleteTokenStillRequiresOwner() {
    when(userTokenRepository.findByIdAndUserId(1L, "root")).thenReturn(null);

    assertThrows(NotFoundException.class, () -> userTokenService.deleteToken(1L, "root"));
  }

  private UserTokenCreateRequest createRequest() {
    UserTokenCreateRequest request = new UserTokenCreateRequest();
    request.setName("ai-agent");
    request.setOperations(Collections.singleton(UserTokenOperation.CONFIG_READ));
    return request;
  }

  private UserToken createUserToken(long id, String userId, Date expires) {
    UserToken token = new UserToken();
    token.setId(id);
    token.setUserId(userId);
    token.setName("token-" + id);
    token.setTokenPrefix("prefix" + id);
    token.setTokenHash("hash" + id);
    token.setScopes("{}");
    token.setRateLimit(0);
    token.setExpires(expires);
    token.setDataChangeCreatedBy(userId);
    token.setDataChangeCreatedTime(new Date());
    token.setDataChangeLastModifiedBy(userId);
    token.setDataChangeLastModifiedTime(new Date());
    return token;
  }

  private Date futureDate() {
    return new Date(System.currentTimeMillis() + 60 * 60 * 1000);
  }

  private Date pastDate() {
    return new Date(System.currentTimeMillis() - 60 * 60 * 1000);
  }
}
