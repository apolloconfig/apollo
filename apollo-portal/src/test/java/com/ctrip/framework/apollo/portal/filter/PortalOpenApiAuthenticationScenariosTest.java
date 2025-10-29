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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.spi.configuration.AuthFilterConfiguration;
import java.util.Date;
import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RunWith(org.springframework.test.context.junit4.SpringRunner.class)
@SpringBootTest(classes = PortalOpenApiAuthenticationScenariosTest.TestApplication.class)
@AutoConfigureMockMvc
// Restrict helper beans (controllers + security config) to a synthetic profile so other tests
// scanning the same base package do not accidentally pick them up.
@ActiveProfiles({"auth", "portal-scenarios-test"})
public class PortalOpenApiAuthenticationScenariosTest {

  private static final String PORTAL_URI = "/apps/test/envs/DEV/clusters/default";
  private static final String OPEN_API_URI = "/openapi/v1/envs/DEV/apps/test/clusters/default";

  @SpringBootApplication
  @Import({AuthFilterConfiguration.class, TestSecurityConfiguration.class,
      TestControllerConfiguration.class})
  static class TestApplication {

  }

  @Configuration
  @EnableWebSecurity
  @Order(200)
  // Keep this test-only WebSecurityConfigurer from leaking into other SpringBootTests.
  @Profile("portal-scenarios-test")
  static class TestSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable();
      http.authorizeRequests()
          .antMatchers("/signin").permitAll()
          .antMatchers("/openapi/**").permitAll()
          .anyRequest().hasRole("user");
      http.formLogin().loginPage("/signin");
      http.exceptionHandling()
          .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/signin"));
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.inMemoryAuthentication()
          .withUser("apollo").password("{noop}password").roles("user");
    }
  }

  @Configuration
  // Controllers under test live behind the same synthetic profile for the same reason as above.
  @Profile("portal-scenarios-test")
  static class TestControllerConfiguration {

    @RestController
    @Profile("portal-scenarios-test")
    static class PortalTestController {

      @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}")
      public ResponseEntity<String> loadPortalCluster(@PathVariable String appId,
          @PathVariable String env, @PathVariable String clusterName) {
        return ResponseEntity.ok("portal-ok");
      }
    }

    @RestController
    @Profile("portal-scenarios-test")
    static class OpenApiTestController {

      @GetMapping("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}")
      public ResponseEntity<String> loadOpenApiCluster(@PathVariable String env,
          @PathVariable String appId, @PathVariable String clusterName) {
        return ResponseEntity.ok("openapi-ok");
      }
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ConsumerAuthUtil consumerAuthUtil;

  @MockBean
  private ConsumerAuditUtil consumerAuditUtil;

  @After
  public void tearDown() {
    reset(consumerAuthUtil, consumerAuditUtil);
  }

  private MockHttpSession authenticatedPortalSession() {
    MockHttpSession session = new MockHttpSession();
    SecurityContextImpl securityContext = new SecurityContextImpl();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
        "apollo", "password", AuthorityUtils.createAuthorityList("ROLE_user")));
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        securityContext);
    return session;
  }

  // Scenario 2.1-1: Portal endpoint with valid session returns 200 OK.
  @Test
  public void portalRequestWithValidSession_shouldReturnOk() throws Exception {
    MockHttpSession session = authenticatedPortalSession();

    mockMvc.perform(get(PORTAL_URI).session(session))
        .andExpect(status().isOk());
  }

  // Scenario 2.1-2: Portal endpoint with expired session redirects to /signin (auth/ldap) or returns 401 (oidc).
  @Test
  public void portalRequestWithExpiredSession_shouldRedirectToSignin() throws Exception {
    // auth/ldap path is handled via Spring Security entry point
    mockMvc.perform(get(PORTAL_URI).cookie(new Cookie("SESSION", "expired")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("http://localhost/signin"));

    // oidc path is handled by PortalUserSessionFilter
    MockEnvironment oidcEnvironment = new MockEnvironment();
    oidcEnvironment.setActiveProfiles("oidc");
    PortalUserSessionFilter oidcFilter = new PortalUserSessionFilter(oidcEnvironment);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", PORTAL_URI);
    request.setCookies(new Cookie("SESSION", "expired"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = new MockFilterChain();

    oidcFilter.doFilter(request, response, chain);

    org.junit.Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }

  // Scenario 2.2-1: Portal user hitting OpenAPI with valid session returns 200 OK.
  @Test
  public void openApiRequestWithPortalSession_shouldReturnOk() throws Exception {
    MockHttpSession session = authenticatedPortalSession();

    mockMvc.perform(get(OPEN_API_URI).session(session))
        .andExpect(status().isOk());
  }

  // Scenario 2.2-2: OpenAPI with expired portal session redirects (auth/ldap) or returns 401 (oidc).
  @Test
  public void openApiRequestWithExpiredSession_shouldFollowProfileSpecificHandling()
      throws Exception {
    // auth/ldap
    mockMvc.perform(get(OPEN_API_URI).cookie(new Cookie("SESSION", "expired")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            "http://localhost/signin")); // should be aligned with 2.1-2 portal calling portal

    // oidc
    MockEnvironment oidcEnvironment = new MockEnvironment();
    oidcEnvironment.setActiveProfiles("oidc");
    PortalUserSessionFilter oidcFilter = new PortalUserSessionFilter(oidcEnvironment);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", OPEN_API_URI);
    request.setCookies(new Cookie("SESSION", "expired"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = new MockFilterChain();

    oidcFilter.doFilter(request, response, chain);
    org.junit.Assert.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }

  // Scenario 2.2-3: External system with valid token gets 200 OK.
  @Test
  public void openApiRequestWithValidToken_shouldReturnOk() throws Exception {
    ConsumerToken token = new ConsumerToken();
    token.setConsumerId(1L);
    token.setToken("valid-token");
    token.setRateLimit(0);
    token.setExpires(new Date(System.currentTimeMillis() + 60_000));

    when(consumerAuthUtil.getConsumerToken("valid-token")).thenReturn(token);
    when(consumerAuditUtil.audit(any(HttpServletRequest.class), eq(1L))).thenReturn(true);

    mockMvc.perform(get(OPEN_API_URI).header("Authorization", "valid-token"))
        .andExpect(status().isOk());
  }

  // Scenario 2.2-4: Unauthenticated call without token gets 401 Unauthorized.
  @Test
  public void openApiRequestWithoutLoginOrToken_shouldReturn401() throws Exception {
    when(consumerAuthUtil.getConsumerToken(null)).thenReturn(null);

    mockMvc.perform(get(OPEN_API_URI))
        .andExpect(status().isUnauthorized());
  }
}
