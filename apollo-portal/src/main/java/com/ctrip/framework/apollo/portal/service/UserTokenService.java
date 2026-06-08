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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.WebUtils;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.po.UserTokenAudit;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenCreateRequest;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenInfo;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenNamespaceScope;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenScope;
import com.ctrip.framework.apollo.portal.repository.UserTokenAuditRepository;
import com.ctrip.framework.apollo.portal.repository.UserTokenRepository;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserTokenService {

  public static final String TOKEN_PREFIX = "apollo_pat_";
  public static final String TOKEN_STATUS_ACTIVE = "active";
  public static final String TOKEN_STATUS_EXPIRED = "expired";
  public static final String TOKEN_STATUS_REVOKED = "revoked";
  public static final String TOKEN_STATUS_ALL = "all";

  private static final int USER_ENABLED = 1;
  private static final int TOKEN_PREFIX_BYTES = 6;
  private static final int TOKEN_SECRET_BYTES = 32;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final Gson GSON = new Gson();

  private final UserTokenRepository userTokenRepository;
  private final UserTokenAuditRepository userTokenAuditRepository;
  private final UserService userService;
  private final PortalConfig portalConfig;
  private final UserPermissionValidator userPermissionValidator;

  public UserTokenService(final UserTokenRepository userTokenRepository,
      final UserTokenAuditRepository userTokenAuditRepository, final UserService userService,
      final PortalConfig portalConfig, final UserPermissionValidator userPermissionValidator) {
    this.userTokenRepository = userTokenRepository;
    this.userTokenAuditRepository = userTokenAuditRepository;
    this.userService = userService;
    this.portalConfig = portalConfig;
    this.userPermissionValidator = userPermissionValidator;
  }

  @Transactional
  public UserTokenInfo createToken(UserTokenCreateRequest request, String operator) {
    validateOperator(operator);
    validateUserEnabled(operator);
    validateCreateRequest(request);

    Date now = new Date();
    Date expires = resolveExpires(request.getExpires(), now);
    UserTokenScope scope = buildScope(request);
    GeneratedToken generatedToken = generateToken();

    UserToken userToken = new UserToken();
    userToken.setId(0L);
    userToken.setUserId(operator);
    userToken.setName(request.getName().trim());
    userToken.setTokenPrefix(generatedToken.tokenPrefix);
    userToken.setTokenHash(hash(generatedToken.tokenValue));
    userToken.setScopes(GSON.toJson(scope));
    userToken.setRateLimit(resolveRateLimit(request.getRateLimit()));
    userToken.setExpires(expires);
    userToken.setDataChangeCreatedBy(operator);
    userToken.setDataChangeCreatedTime(now);
    userToken.setDataChangeLastModifiedBy(operator);
    userToken.setDataChangeLastModifiedTime(now);

    UserToken saved = userTokenRepository.save(userToken);
    UserTokenInfo result = toInfo(saved);
    result.setTokenValue(generatedToken.tokenValue);
    return result;
  }

  public List<UserTokenInfo> findUserTokens(String userId) {
    validateOperator(userId);
    Date now = new Date();
    return userTokenRepository.findByUserIdOrderByDataChangeCreatedTimeDesc(userId).stream()
        .map(userToken -> toInfo(userToken, now)).collect(Collectors.toList());
  }

  public List<UserTokenInfo> findUserTokensForAdmin(String userId, String status) {
    String normalizedStatus = normalizeStatus(status);
    Date now = new Date();
    List<UserToken> userTokens = StringUtils.isBlank(userId)
        ? userTokenRepository.findAllByOrderByDataChangeCreatedTimeDesc()
        : userTokenRepository
            .findByUserIdContainingIgnoreCaseOrderByDataChangeCreatedTimeDesc(userId.trim());
    return userTokens.stream().filter(userToken -> matchesStatus(userToken, normalizedStatus, now))
        .map(userToken -> toInfo(userToken, now)).collect(Collectors.toList());
  }

  @Transactional
  public void revokeToken(long tokenId, String operator) {
    UserToken userToken = findOwnedToken(tokenId, operator);
    revokeToken(userToken, operator);
  }

  @Transactional
  public void revokeTokenForAdmin(long tokenId, String operator) {
    validateOperator(operator);
    UserToken userToken = findToken(tokenId);
    revokeToken(userToken, operator);
  }

  @Transactional
  public void deleteToken(long tokenId, String operator) {
    UserToken userToken = findOwnedToken(tokenId, operator);
    deleteToken(userToken, operator);
  }

  @Transactional
  public void deleteTokenForAdmin(long tokenId, String operator) {
    validateOperator(operator);
    UserToken userToken = findToken(tokenId);
    deleteToken(userToken, operator);
  }

  private void revokeToken(UserToken userToken, String operator) {
    validateOperator(operator);
    if (userToken.getRevokedAt() != null) {
      return;
    }
    Date now = new Date();
    userToken.setRevokedAt(now);
    userToken.setRevokedBy(operator);
    userToken.setDataChangeLastModifiedBy(operator);
    userToken.setDataChangeLastModifiedTime(now);
    userTokenRepository.save(userToken);
  }

  private void deleteToken(UserToken userToken, String operator) {
    validateOperator(operator);
    if (userToken.getRevokedAt() == null) {
      Date now = new Date();
      userToken.setRevokedAt(now);
      userToken.setRevokedBy(operator);
      userToken.setDataChangeLastModifiedBy(operator);
      userToken.setDataChangeLastModifiedTime(now);
      userTokenRepository.saveAndFlush(userToken);
    }
    userTokenRepository.delete(userToken);
  }

  @Transactional
  public UserTokenInfo rotateToken(long tokenId, String operator) {
    UserToken userToken = findOwnedToken(tokenId, operator);
    if (userToken.getRevokedAt() != null || userToken.getExpires().before(new Date())) {
      throw new BadRequestException("Token is not active");
    }

    UserTokenCreateRequest request = new UserTokenCreateRequest();
    request.setName(userToken.getName());
    UserTokenScope scope = parseScope(userToken);
    request.setOperations(scope.getOperations());
    request.setAppIds(scope.getAppIds());
    request.setEnvs(scope.getEnvs());
    request.setNamespaces(scope.getNamespaces());
    request.setRateLimit(userToken.getRateLimit());
    request.setExpires(userToken.getExpires());

    revokeToken(tokenId, operator);
    return createToken(request, operator);
  }

  @Transactional
  public UserToken authenticate(String token, HttpServletRequest request) {
    String tokenPrefix = parseTokenPrefix(token);
    if (tokenPrefix == null) {
      return null;
    }
    UserToken userToken = userTokenRepository
        .findTopByTokenPrefixAndExpiresAfterAndRevokedAtIsNull(tokenPrefix, new Date());
    if (userToken == null || !secureEquals(userToken.getTokenHash(), hash(token))) {
      return null;
    }
    UserInfo userInfo = userService.findByUserId(userToken.getUserId());
    if (userInfo == null || userInfo.getEnabled() != USER_ENABLED) {
      return null;
    }

    Date now = new Date();
    userToken.setLastUsedTime(now);
    userToken.setLastUsedIp(WebUtils.tryToGetClientIp(request));
    userToken.setLastUsedUserAgent(request.getHeader("User-Agent"));
    userToken.setDataChangeLastModifiedBy(userToken.getUserId());
    userToken.setDataChangeLastModifiedTime(now);
    return userTokenRepository.save(userToken);
  }

  public UserTokenScope parseScope(UserToken userToken) {
    if (userToken == null || Strings.isNullOrEmpty(userToken.getScopes())) {
      return UserTokenScope.denyAll();
    }
    try {
      UserTokenScope scope = GSON.fromJson(userToken.getScopes(), UserTokenScope.class);
      return scope == null ? UserTokenScope.denyAll() : scope;
    } catch (Exception ignored) {
      return UserTokenScope.denyAll();
    }
  }

  public List<String> findAvailableOperations() {
    List<String> operations = new ArrayList<>(UserTokenOperation.RESOURCE_SCOPED);
    if (userPermissionValidator.hasCreateApplicationPermission()) {
      operations.add(UserTokenOperation.APP_CREATE);
    }
    if (userPermissionValidator.hasManageUsersPermission()) {
      operations.add(UserTokenOperation.USER_MANAGE);
    }
    if (userPermissionValidator.isSuperAdmin()) {
      operations.add(UserTokenOperation.SYSTEM_ADMIN);
    }
    return operations;
  }

  @Transactional
  public void createUserTokenAudits(Iterable<UserTokenAudit> userTokenAudits) {
    userTokenAuditRepository.saveAll(userTokenAudits);
  }

  private UserToken findOwnedToken(long tokenId, String operator) {
    validateOperator(operator);
    UserToken userToken = userTokenRepository.findByIdAndUserId(tokenId, operator);
    if (userToken == null) {
      throw new NotFoundException("user token not found for id:%s", tokenId);
    }
    return userToken;
  }

  private UserToken findToken(long tokenId) {
    return userTokenRepository.findById(tokenId)
        .orElseThrow(() -> new NotFoundException("user token not found for id:%s", tokenId));
  }

  private String normalizeStatus(String status) {
    if (StringUtils.isBlank(status)) {
      return TOKEN_STATUS_ALL;
    }
    String normalizedStatus = status.trim().toLowerCase(Locale.ROOT);
    if (TOKEN_STATUS_ALL.equals(normalizedStatus) || TOKEN_STATUS_ACTIVE.equals(normalizedStatus)
        || TOKEN_STATUS_EXPIRED.equals(normalizedStatus)
        || TOKEN_STATUS_REVOKED.equals(normalizedStatus)) {
      return normalizedStatus;
    }
    throw new BadRequestException("Invalid user token status:%s", status);
  }

  private boolean matchesStatus(UserToken userToken, String status, Date now) {
    return TOKEN_STATUS_ALL.equals(status) || status.equals(resolveStatus(userToken, now));
  }

  private String resolveStatus(UserToken userToken, Date now) {
    if (userToken.getRevokedAt() != null) {
      return TOKEN_STATUS_REVOKED;
    }
    if (userToken.getExpires() != null && !userToken.getExpires().after(now)) {
      return TOKEN_STATUS_EXPIRED;
    }
    return TOKEN_STATUS_ACTIVE;
  }

  private UserTokenScope buildScope(UserTokenCreateRequest request) {
    UserTokenScope scope = new UserTokenScope();
    scope.setOperations(normalizeOperations(request.getOperations()));
    scope.setAppIds(emptyToNull(request.getAppIds()));
    scope.setEnvs(emptyToNull(request.getEnvs()));
    scope.setNamespaces(request.getNamespaces());
    return scope;
  }

  private Set<String> normalizeOperations(Set<String> operations) {
    if (operations == null || operations.isEmpty()) {
      return null;
    }
    Set<String> normalized = new HashSet<>();
    for (String operation : operations) {
      if (StringUtils.isBlank(operation)) {
        continue;
      }
      if (!UserTokenOperation.ALL.contains(operation)) {
        throw new BadRequestException("Invalid user token operation:%s", operation);
      }
      if (!isOperationAvailable(operation)) {
        throw new BadRequestException("User token operation is not allowed:%s", operation);
      }
      normalized.add(operation);
    }
    return normalized.isEmpty() ? null : normalized;
  }

  private boolean isOperationAvailable(String operation) {
    if (UserTokenOperation.RESOURCE_SCOPED.contains(operation)) {
      return true;
    }
    if (UserTokenOperation.APP_CREATE.equals(operation)) {
      return userPermissionValidator.hasCreateApplicationPermission();
    }
    if (UserTokenOperation.USER_MANAGE.equals(operation)) {
      return userPermissionValidator.hasManageUsersPermission();
    }
    if (UserTokenOperation.SYSTEM_ADMIN.equals(operation)) {
      return userPermissionValidator.isSuperAdmin();
    }
    return false;
  }

  private Set<String> emptyToNull(Set<String> values) {
    return values == null || values.isEmpty() ? null : values;
  }

  private void validateCreateRequest(UserTokenCreateRequest request) {
    if (request == null || StringUtils.isBlank(request.getName())) {
      throw new BadRequestException("Token name can not be blank");
    }
    if (request.getRateLimit() != null && request.getRateLimit() < 0) {
      throw BadRequestException.rateLimitIsInvalid();
    }
    if (request.getNamespaces() == null) {
      return;
    }
    for (UserTokenNamespaceScope namespaceScope : request.getNamespaces()) {
      if (namespaceScope == null) {
        throw new BadRequestException("Token namespace scope can not be null");
      }
    }
  }

  private void validateUserEnabled(String userId) {
    UserInfo userInfo = userService.findByUserId(userId);
    if (userInfo == null) {
      throw BadRequestException.userNotExists(userId);
    }
    if (userInfo.getEnabled() != USER_ENABLED) {
      throw new BadRequestException("User is disabled");
    }
  }

  private Date resolveExpires(Date requestedExpires, Date now) {
    Date expires = requestedExpires;
    if (expires == null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(now);
      calendar.add(Calendar.DAY_OF_YEAR, portalConfig.userTokenDefaultExpireDays());
      expires = calendar.getTime();
    }
    if (!expires.after(now)) {
      throw new BadRequestException("Token expires must be in the future");
    }

    Calendar maxCalendar = Calendar.getInstance();
    maxCalendar.setTime(now);
    maxCalendar.add(Calendar.DAY_OF_YEAR, portalConfig.userTokenMaxExpireDays());
    if (expires.after(maxCalendar.getTime())) {
      throw new BadRequestException("Token expires exceeds max allowed days:%s",
          portalConfig.userTokenMaxExpireDays());
    }
    return expires;
  }

  private int resolveRateLimit(Integer rateLimit) {
    return rateLimit == null ? 0 : rateLimit;
  }

  private void validateOperator(String operator) {
    if (StringUtils.isBlank(operator)) {
      throw new BadRequestException("operator should not be null or empty");
    }
  }

  private UserTokenInfo toInfo(UserToken userToken) {
    return toInfo(userToken, new Date());
  }

  private UserTokenInfo toInfo(UserToken userToken, Date now) {
    UserTokenScope scope = parseScope(userToken);
    UserTokenInfo info = new UserTokenInfo();
    info.setId(userToken.getId());
    info.setUserId(userToken.getUserId());
    info.setName(userToken.getName());
    info.setTokenPrefix(userToken.getTokenPrefix());
    info.setStatus(resolveStatus(userToken, now));
    info.setOperations(scope.getOperations());
    info.setAppIds(scope.getAppIds());
    info.setEnvs(scope.getEnvs());
    info.setNamespaces(scope.getNamespaces());
    info.setRateLimit(userToken.getRateLimit());
    info.setExpires(userToken.getExpires());
    info.setLastUsedTime(userToken.getLastUsedTime());
    info.setLastUsedIp(userToken.getLastUsedIp());
    info.setLastUsedUserAgent(userToken.getLastUsedUserAgent());
    info.setRevokedAt(userToken.getRevokedAt());
    info.setRevokedBy(userToken.getRevokedBy());
    info.setDataChangeCreatedTime(userToken.getDataChangeCreatedTime());
    return info;
  }

  private GeneratedToken generateToken() {
    String tokenPrefix = randomUrlSafeString(TOKEN_PREFIX_BYTES);
    String tokenSecret = randomUrlSafeString(TOKEN_SECRET_BYTES);
    return new GeneratedToken(tokenPrefix, TOKEN_PREFIX + tokenPrefix + "_" + tokenSecret);
  }

  private String randomUrlSafeString(int bytes) {
    String tokenPart;
    do {
      byte[] randomBytes = new byte[bytes];
      SECURE_RANDOM.nextBytes(randomBytes);
      tokenPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    } while (tokenPart.contains("_"));
    return tokenPart;
  }

  @SuppressWarnings("UnstableApiUsage")
  private String hash(String token) {
    return Hashing.sha256().hashString(token, Charsets.UTF_8).toString();
  }

  private boolean secureEquals(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
        actual.getBytes(StandardCharsets.UTF_8));
  }

  private String parseTokenPrefix(String token) {
    if (Strings.isNullOrEmpty(token) || !token.startsWith(TOKEN_PREFIX)) {
      return null;
    }
    String remaining = token.substring(TOKEN_PREFIX.length());
    int splitIndex = remaining.indexOf('_');
    if (splitIndex <= 0) {
      return null;
    }
    return remaining.substring(0, splitIndex);
  }

  private static class GeneratedToken {
    private final String tokenPrefix;
    private final String tokenValue;

    private GeneratedToken(String tokenPrefix, String tokenValue) {
      this.tokenPrefix = tokenPrefix;
      this.tokenValue = tokenValue;
    }
  }
}
