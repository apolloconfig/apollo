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
package com.ctrip.framework.apollo.portal.filter;

import com.ctrip.framework.apollo.portal.auth.UserTokenAuthenticationToken;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.util.UserTokenAuditUtil;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates OpenAPI requests carrying portal-managed user access tokens.
 */
public class UserTokenAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(UserTokenAuthenticationFilter.class);

  private static final String OPEN_API_PREFIX = "/openapi/";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String USER_ROLE = "ROLE_user";
  private static final int RATE_LIMITER_CACHE_MAX_SIZE = 20000;
  private static final int TOO_MANY_REQUESTS = 429;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Cache<String, RateLimiter> LIMITER = CacheBuilder.newBuilder()
      .expireAfterAccess(1, TimeUnit.HOURS).maximumSize(RATE_LIMITER_CACHE_MAX_SIZE).build();

  private final UserTokenService userTokenService;
  private final UserTokenAuthUtil userTokenAuthUtil;
  private final UserTokenAuditUtil userTokenAuditUtil;

  public UserTokenAuthenticationFilter(final UserTokenService userTokenService,
      final UserTokenAuthUtil userTokenAuthUtil, final UserTokenAuditUtil userTokenAuditUtil) {
    this.userTokenService = userTokenService;
    this.userTokenAuthUtil = userTokenAuthUtil;
    this.userTokenAuditUtil = userTokenAuditUtil;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
      requestUri = requestUri.substring(contextPath.length());
    }
    return !requestUri.startsWith(OPEN_API_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String token = resolveUserToken(request);
    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    UserToken userToken = userTokenService.authenticate(token, request);
    if (userToken == null) {
      writeOpenApiError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized user token");
      return;
    }

    Integer rateLimit = userToken.getRateLimit();
    if (rateLimit != null && rateLimit > 0) {
      try {
        RateLimiter rateLimiter = getOrCreateRateLimiter(userToken.getTokenPrefix(), rateLimit);
        if (!rateLimiter.tryAcquire()) {
          writeOpenApiError(response, TOO_MANY_REQUESTS, "Too Many Requests, the flow is limited");
          return;
        }
      } catch (Exception e) {
        logger.error("UserTokenAuthenticationFilter ratelimit error", e);
        writeOpenApiError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Rate limiting failed");
        return;
      }
    }

    userTokenAuthUtil.storeUserToken(request, userToken);
    userTokenAuditUtil.audit(request, userToken);
    SecurityContextHolder.getContext().setAuthentication(UserTokenAuthenticationToken.authenticated(
        userToken, Collections.singletonList(new SimpleGrantedAuthority(USER_ROLE))));
    filterChain.doFilter(request, response);
  }

  private String resolveUserToken(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }
    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (!token.startsWith(UserTokenService.TOKEN_PREFIX)) {
      return null;
    }
    return token;
  }

  private void writeOpenApiError(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    OBJECT_MAPPER.writeValue(response.getWriter(), Map.of("message", message));
  }

  private RateLimiter getOrCreateRateLimiter(String key, Integer limitCount) {
    try {
      return LIMITER.get(key, () -> RateLimiter.create(limitCount));
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to create rate limiter", e);
    }
  }
}
