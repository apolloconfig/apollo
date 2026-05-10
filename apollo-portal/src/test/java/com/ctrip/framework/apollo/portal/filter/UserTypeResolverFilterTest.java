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

import static com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil.CONSUMER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserTypeResolverFilterTest {

  private UserTypeResolverFilter filter;

  @BeforeEach
  public void setUp() {
    filter = new UserTypeResolverFilter();
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  @Test
  public void shouldResolveConsumerWhenConsumerIdExists() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/openapi/v1/envs");
    request.setAttribute(CONSUMER_ID, 1L);

    CapturingFilterChain chain = doFilter(request);

    assertEquals(UserIdentityConstants.CONSUMER, chain.authType);
    assertNull(UserIdentityContextHolder.getAuthType());
  }

  @Test
  public void shouldResolveUserWhenSecurityContextHasAuthenticatedUser() throws Exception {
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("apollo", "password", "ROLE_user");
    authentication.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    CapturingFilterChain chain = doFilter(new MockHttpServletRequest("GET", "/openapi/v1/envs"));

    assertEquals(UserIdentityConstants.USER, chain.authType);
    assertNull(UserIdentityContextHolder.getAuthType());
  }

  @Test
  public void shouldResolveAnonymousWhenOnlyAnonymousAuthenticationExists() throws Exception {
    AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken("key",
        "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    CapturingFilterChain chain = doFilter(new MockHttpServletRequest("GET", "/openapi/v1/envs"));

    assertEquals(UserIdentityConstants.ANONYMOUS, chain.authType);
    assertNull(UserIdentityContextHolder.getAuthType());
  }

  @Test
  public void shouldResolveAnonymousWhenRequestIsUnauthenticated() throws Exception {
    CapturingFilterChain chain = doFilter(new MockHttpServletRequest("GET", "/openapi/v1/envs"));

    assertEquals(UserIdentityConstants.ANONYMOUS, chain.authType);
    assertNull(UserIdentityContextHolder.getAuthType());
  }

  private CapturingFilterChain doFilter(MockHttpServletRequest request)
      throws ServletException, IOException {
    CapturingFilterChain chain = new CapturingFilterChain();
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    return chain;
  }

  private static class CapturingFilterChain implements FilterChain {

    private String authType;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) {
      authType = UserIdentityContextHolder.getAuthType();
    }
  }
}
