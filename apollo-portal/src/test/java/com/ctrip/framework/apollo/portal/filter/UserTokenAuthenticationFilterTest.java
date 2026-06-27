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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.auth.UserTokenAuthenticationToken;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.util.UserTokenAuditUtil;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserTokenAuthenticationFilterTest {

  @Mock
  private UserTokenService userTokenService;

  @Mock
  private UserTokenAuthUtil userTokenAuthUtil;

  @Mock
  private UserTokenAuditUtil userTokenAuditUtil;

  @Mock
  private FilterChain filterChain;

  private UserTokenAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter =
        new UserTokenAuthenticationFilter(userTokenService, userTokenAuthUtil, userTokenAuditUtil);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void validUserTokenAuthenticatesRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/openapi/v1/apps");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String token = UserTokenService.TOKEN_PREFIX + "abc_secret";
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    UserToken userToken = new UserToken();
    userToken.setId(1L);
    userToken.setUserId("apollo");
    userToken.setTokenPrefix("abc");
    userToken.setRateLimit(0);
    when(userTokenService.authenticate(token, request)).thenReturn(userToken);

    filter.doFilter(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertTrue(SecurityContextHolder.getContext()
        .getAuthentication() instanceof UserTokenAuthenticationToken);
    verify(userTokenAuthUtil).storeUserToken(request, userToken);
    verify(userTokenAuditUtil).audit(request, userToken);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void invalidApolloUserTokenReturnsUnauthorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/openapi/v1/apps");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String token = UserTokenService.TOKEN_PREFIX + "abc_secret";
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    when(userTokenService.authenticate(token, request)).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain, never()).doFilter(request, response);
    org.junit.jupiter.api.Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED,
        response.getStatus());
  }

  @Test
  void rateLimitedUserTokenReturnsTooManyRequestsWithoutErrorDispatch() throws Exception {
    String token = UserTokenService.TOKEN_PREFIX + "rate429_secret";
    UserToken userToken = new UserToken();
    userToken.setId(1L);
    userToken.setUserId("apollo");
    userToken.setTokenPrefix("rate429");
    userToken.setRateLimit(1);
    when(userTokenService.authenticate(org.mockito.ArgumentMatchers.eq(token),
        org.mockito.ArgumentMatchers.any())).thenReturn(userToken);

    MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/openapi/v1/apps");
    MockHttpServletResponse firstResponse = new MockHttpServletResponse();
    firstRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    filter.doFilter(firstRequest, firstResponse, filterChain);
    SecurityContextHolder.clearContext();

    MockHttpServletRequest limitedRequest =
        new MockHttpServletRequest("GET", "/openapi/v1/apps");
    MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
    limitedRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    filter.doFilter(limitedRequest, limitedResponse, filterChain);

    assertEquals(429, limitedResponse.getStatus());
    assertNull(limitedResponse.getErrorMessage());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(firstRequest, firstResponse);
    verify(filterChain, never()).doFilter(limitedRequest, limitedResponse);
  }

  @Test
  void nonApolloBearerTokenPassesThrough() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/openapi/v1/apps");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");

    filter.doFilter(request, response, filterChain);

    verify(userTokenService, never()).authenticate(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void apolloUserTokenDoesNotAuthenticateNonOpenApiRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/apps/test");
    MockHttpServletResponse response = new MockHttpServletResponse();
    String token = UserTokenService.TOKEN_PREFIX + "abc_secret";
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    filter.doFilter(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userTokenService, never()).authenticate(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any());
    verify(userTokenAuthUtil, never()).storeUserToken(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
    verify(userTokenAuditUtil, never()).audit(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
    verify(filterChain).doFilter(request, response);
  }
}
