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
import static org.mockito.Mockito.mock;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.filter.UserTokenAuthenticationFilter;
import com.ctrip.framework.apollo.portal.repository.AuthorityRepository;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.oidc.OidcUserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserInfoHolder;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.Filter;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Verifies that Portal requires a configured identity provider before accepting requests.
 */
class PortalAuthenticationConfigurationTest {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner().withUserConfiguration(AuthConfiguration.class)
          .withBean(DataSource.class, () -> mock(DataSource.class))
          .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
          .withBean(UserRepository.class, () -> mock(UserRepository.class))
          .withBean(AuthorityRepository.class, () -> mock(AuthorityRepository.class))
          .withBean(UserTokenAuthenticationFilter.class,
              () -> new UserTokenAuthenticationFilter(null, null, null))
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
