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
package com.ctrip.framework.apollo.portal.spi.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.auth.UserTokenAuthenticationToken;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.filter.UserTokenAuthenticationFilter;
import com.ctrip.framework.apollo.portal.repository.AuthorityRepository;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcLocalUserService;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserInfoHolder;
import com.ctrip.framework.apollo.portal.util.UserTokenAuditUtil;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.Filter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Verifies that Portal requires a configured identity provider before accepting requests.
 */
class PortalAuthenticationConfigurationTest {

  private final UserTokenService userTokenService = mock(UserTokenService.class);
  private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);

  @AfterEach
  void tearDown() {
    reset(this.userTokenService, this.jwtDecoder);
  }

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner().withUserConfiguration(AuthConfiguration.class)
          .withBean(DataSource.class, () -> mock(DataSource.class))
          .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
          .withBean(UserRepository.class, () -> mock(UserRepository.class))
          .withBean(AuthorityRepository.class, () -> mock(AuthorityRepository.class))
          .withBean(UserTokenService.class, () -> this.userTokenService)
          .withBean(JwtDecoder.class, () -> this.jwtDecoder)
          .withBean(UserTokenAuthenticationFilter.class,
              () -> new UserTokenAuthenticationFilter(this.userTokenService,
                  new UserTokenAuthUtil(), mock(UserTokenAuditUtil.class)))
          .withBean(LdapContextSource.class, () -> {
            LdapContextSource source = new LdapContextSource();
            source.setUrl("ldap://localhost:389");
            source.setBase("dc=example,dc=com");
            return source;
          })
          .withBean(InMemoryClientRegistrationRepository.class,
              () -> new InMemoryClientRegistrationRepository(ClientRegistration
                  .withRegistrationId("test").clientId("portal").clientSecret("unused")
                  .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                  .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("openid")
                  .authorizationUri("https://idp.example/authorize")
                  .tokenUri("https://idp.example/token").jwkSetUri("https://idp.example/keys")
                  .build()))
          .withPropertyValues("spring.ldap.urls=ldap://localhost:389",
              "spring.ldap.base=dc=example,dc=com", "spring.ldap.search-filter=(uid={0})");

  @ParameterizedTest
  @ValueSource(strings = {"", "github", "github,ath"})
  void shouldRejectMissingAuthentication(String profiles) {
    withProfiles(profiles).run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .hasStackTraceContaining("Portal authentication is not configured")
          .hasStackTraceContaining("auth, ldap or oidc");
    });
  }

  @Test
  void shouldRejectMissingAuthenticationWithLazyInitialization() {
    withProfiles("github")
        .withInitializer(context -> context
            .addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasStackTraceContaining("Portal authentication is not configured");
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"auth", "ldap", "oidc"})
  void shouldKeepConfiguredAuthenticationAndRejectAnonymousRequests(String profile) {
    withProfiles("github," + profile).run(context -> {
      assertThat(context).hasNotFailed();
      UserInfoHolder holder = context.getBean(UserInfoHolder.class);
      assertThat(holder).isInstanceOf(
          "oidc".equals(profile) ? OidcUserInfoHolder.class : SpringSecurityUserInfoHolder.class);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user");
      request.setServletPath("/user");
      MockHttpServletResponse response = new MockHttpServletResponse();
      AtomicBoolean reachedController = new AtomicBoolean();
      context.getBean("springSecurityFilterChain", Filter.class).doFilter(request, response,
          (req, res) -> reachedController.set(true));

      assertThat(reachedController).isFalse();
      assertThat(response.getStatus()).isEqualTo(302);
      assertThat(response.getRedirectedUrl()).isNotNull();
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"github,custom", "github,ctrip"})
  void shouldKeepCustomAuthentication(String profiles) {
    withProfiles(profiles + ",portal-custom-authentication-test")
        .withUserConfiguration(CustomAuthentication.class).run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(UserInfoHolder.class))
              .isSameAs(context.getBean("customUserInfoHolder"));
          assertThat(context.getBean(UserInfoHolder.class).getUser().getUserId())
              .isEqualTo("sso-user");
        });
  }

  @Test
  void shouldAllowUnusedDefaultIdentityWhenCustomAuthenticationIsSelected() {
    withProfiles("github,custom,portal-custom-authentication-test")
        .withUserConfiguration(CustomAuthentication.class).withBean("unusedDefaultUserInfoHolder",
            DefaultUserInfoHolder.class, DefaultUserInfoHolder::new)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(UserInfoHolder.class))
              .isSameAs(context.getBean("customUserInfoHolder"));
        });
  }

  @Test
  void shouldPassUserTokenThroughOidcSecurityFilterChain() {
    UserToken userToken = new UserToken();
    userToken.setId(1L);
    userToken.setUserId("apollo");
    userToken.setTokenPrefix("abc");
    userToken.setRateLimit(0);

    String validToken = UserTokenService.TOKEN_PREFIX + "abc_secret";

    withProfiles("github,oidc")
        .withPropertyValues(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://idp.example")
        .run(context -> {
          UserTokenService mockTokenService = context.getBean(UserTokenService.class);
          when(mockTokenService.authenticate(eq(validToken), any())).thenReturn(userToken);

          Filter securityChain = context.getBean("springSecurityFilterChain", Filter.class);

          // 1. Valid user token with mixed-case "bearer" and two spaces on OpenAPI reaches
          // downstream authenticated
          MockHttpServletRequest validRequest =
              new MockHttpServletRequest("GET", "/openapi/v1/apps");
          validRequest.setServletPath("/openapi/v1/apps");
          validRequest.addHeader(HttpHeaders.AUTHORIZATION, "bearer  " + validToken);
          MockHttpServletResponse validResponse = new MockHttpServletResponse();
          AtomicBoolean reachedController = new AtomicBoolean();

          securityChain.doFilter(validRequest, validResponse, (req, res) -> {
            reachedController.set(true);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(UserTokenAuthenticationToken.class);
          });
          assertThat(reachedController).isTrue();
          SecurityContextHolder.clearContext();

          // 2. Invalid user token on OpenAPI is rejected with 401 Unauthorized user token
          MockHttpServletRequest invalidRequest =
              new MockHttpServletRequest("GET", "/openapi/v1/apps");
          invalidRequest.setServletPath("/openapi/v1/apps");
          invalidRequest.addHeader(HttpHeaders.AUTHORIZATION,
              "Bearer  " + UserTokenService.TOKEN_PREFIX + "invalid");
          MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
          AtomicBoolean reachedWithInvalid = new AtomicBoolean();

          securityChain.doFilter(invalidRequest, invalidResponse,
              (req, res) -> reachedWithInvalid.set(true));
          assertThat(reachedWithInvalid).isFalse();
          assertThat(invalidResponse.getStatus()).isEqualTo(401);
          assertThat(invalidResponse.getContentAsString()).contains("Unauthorized user token");
          SecurityContextHolder.clearContext();
        });
  }

  @Test
  void shouldPreserveCustomBearerTokenResolverBeanInOidcChain() {
    BearerTokenResolver customResolver = mock(BearerTokenResolver.class);
    when(customResolver.resolve(any())).thenReturn("custom-resolved-token");

    withProfiles("github,oidc")
        .withPropertyValues(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://idp.example")
        .withBean(BearerTokenResolver.class, () -> customResolver).run(context -> {
          BearerTokenResolver resolver =
              context.getBean(AuthConfiguration.OidcWebSecurityConfigurerAdapter.class)
                  .resolveDelegateBearerTokenResolver();
          assertThat(resolver).isSameAs(customResolver);

          BearerTokenResolver wrapped = AuthConfiguration.OidcWebSecurityConfigurerAdapter
              .createBearerTokenResolver(resolver);

          // Non-OpenAPI request delegates to customResolver
          MockHttpServletRequest portalRequest = new MockHttpServletRequest("GET", "/user");
          assertThat(wrapped.resolve(portalRequest)).isEqualTo("custom-resolved-token");
          verify(customResolver).resolve(portalRequest);

          // OpenAPI request with user token returns null without calling customResolver
          MockHttpServletRequest patRequest = new MockHttpServletRequest("GET", "/openapi/v1/apps");
          patRequest.addHeader(HttpHeaders.AUTHORIZATION,
              "bearer  " + UserTokenService.TOKEN_PREFIX + "test");
          assertThat(wrapped.resolve(patRequest)).isNull();
        });
  }

  @Test
  void shouldAuthenticateJwtThroughCustomHeaderAndAuthenticationConverterInOidcChain() {
    Jwt mockJwt = Jwt.withTokenValue("mock-custom-jwt-token").header("alg", "none")
        .subject("custom-header-user").claim("custom_principal", "custom-header-user")
        .claim("scope", "read").build();

    when(this.jwtDecoder.decode("mock-custom-jwt-token")).thenReturn(mockJwt);

    BearerTokenResolver customResolver = request -> request.getHeader("X-Custom-Token");

    JwtAuthenticationConverter customConverter = new JwtAuthenticationConverter();
    customConverter.setPrincipalClaimName("custom_principal");

    OidcLocalUserService localUserService = mock(OidcLocalUserService.class);
    when(localUserService.findByUserId("custom-header-user"))
        .thenReturn(new UserInfo("custom-header-user"));

    withProfiles("github,oidc")
        .withPropertyValues(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://idp.example")
        .withBean(BearerTokenResolver.class, () -> customResolver)
        .withBean(JwtAuthenticationConverter.class, () -> customConverter)
        .withBean(OidcLocalUserService.class, () -> localUserService).run(context -> {
          Filter securityChain = context.getBean("springSecurityFilterChain", Filter.class);

          // 1. Verify custom header JWT on non-OpenAPI endpoint (/user) authenticates through chain
          MockHttpServletRequest portalRequest = new MockHttpServletRequest("GET", "/user");
          portalRequest.setServletPath("/user");
          portalRequest.addHeader("X-Custom-Token", "mock-custom-jwt-token");
          MockHttpServletResponse portalResponse = new MockHttpServletResponse();
          AtomicBoolean reachedPortal = new AtomicBoolean();

          securityChain.doFilter(portalRequest, portalResponse, (req, res) -> {
            reachedPortal.set(true);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);
            assertThat(auth.getName()).isEqualTo("custom-header-user");
          });
          assertThat(reachedPortal).isTrue();
          SecurityContextHolder.clearContext();

          // 2. Verify custom header JWT on OpenAPI endpoint (/openapi/v1/apps) also authenticates
          // through chain
          MockHttpServletRequest openApiRequest =
              new MockHttpServletRequest("GET", "/openapi/v1/apps");
          openApiRequest.setServletPath("/openapi/v1/apps");
          openApiRequest.addHeader("X-Custom-Token", "mock-custom-jwt-token");
          MockHttpServletResponse openApiResponse = new MockHttpServletResponse();
          AtomicBoolean reachedOpenApi = new AtomicBoolean();

          securityChain.doFilter(openApiRequest, openApiResponse, (req, res) -> {
            reachedOpenApi.set(true);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);
            assertThat(auth.getName()).isEqualTo("custom-header-user");
          });
          assertThat(reachedOpenApi).isTrue();
          SecurityContextHolder.clearContext();
        });
  }

  @Test
  void shouldNotBypassJwtForNonOpenApiRequestInOidcResolver() {
    DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
    BearerTokenResolver wrapped = AuthConfiguration.OidcWebSecurityConfigurerAdapter
        .createBearerTokenResolver(defaultResolver);

    // Non-OpenAPI request with user token does not bypass JWT resolution
    MockHttpServletRequest portalRequest = new MockHttpServletRequest("GET", "/user");
    portalRequest.addHeader(HttpHeaders.AUTHORIZATION,
        "Bearer " + UserTokenService.TOKEN_PREFIX + "test");
    assertThat(wrapped.resolve(portalRequest)).isEqualTo(UserTokenService.TOKEN_PREFIX + "test");
  }

  @Test
  void shouldRejectDefaultIdentityEvenWhenAuthenticationProfileIsPresent() {
    withProfiles("github,auth").withBean("customUserInfoHolder", UserInfoHolder.class,
        DefaultUserInfoHolder::new, definition -> definition.setPrimary(true)).run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasStackTraceContaining("Portal authentication is not configured");
        });
  }

  private WebApplicationContextRunner withProfiles(String profiles) {
    return contextRunner.withInitializer(context -> context.getEnvironment()
        .setActiveProfiles(profiles.isEmpty() ? new String[0] : profiles.split(",")));
  }

  @TestConfiguration
  @Profile("portal-custom-authentication-test")
  static class CustomAuthentication {

    @Bean
    @Primary
    UserInfoHolder customUserInfoHolder() {
      return () -> new UserInfo("sso-user");
    }

    @Bean
    UserService customUserService() {
      return mock(UserService.class);
    }
  }
}
