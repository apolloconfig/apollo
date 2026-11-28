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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.openapi.v1.controller.PortalManagementController;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
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
import com.ctrip.framework.apollo.portal.util.UserTokenAuditUtil;
import com.ctrip.framework.apollo.portal.util.UserTokenAuthUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies expired OIDC sessions through Tomcat, including the production error dispatch security.
 */
@SpringBootTest(classes = PortalOidcExpiredSessionHttpTest.TestConfiguration.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"oidc", "oidc-expired-session-http-test"})
class PortalOidcExpiredSessionHttpTest {

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private ConsumerService consumerService;

  @MockitoBean
  private ConsumerAuthUtil consumerAuthUtil;

  @MockitoBean
  private ConsumerAuditUtil consumerAuditUtil;

  @MockitoBean
  private UserTokenService userTokenService;

  @MockitoBean
  private UserTokenAuditUtil userTokenAuditUtil;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();

  @ParameterizedTest
  @CsvSource({"GET, application/json", "GET, text/html", "GET, */*", "DELETE, application/json",
      "DELETE, text/html", "DELETE, */*"})
  void expiredSessionShouldReturnUnauthorizedWithoutLoginRedirect(String method, String accept)
      throws Exception {
    String path = "/openapi/v1/consumers/1/roles";
    if ("DELETE".equals(method)) {
      path += "?type=AppRole&appId=test";
    }
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .timeout(Duration.ofSeconds(10)).header(HttpHeaders.COOKIE, "SESSION=expired")
        .header(HttpHeaders.ACCEPT, accept).method(method, HttpRequest.BodyPublishers.noBody())
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(401, response.statusCode());
    assertTrue(response.headers().firstValue(HttpHeaders.LOCATION).isEmpty());
    verifyNoInteractions(consumerService, consumerAuthUtil, consumerAuditUtil, userTokenService,
        userTokenAuditUtil);
  }

  @Test
  void unauthenticatedPortalRequestShouldStillStartOidcLogin() throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/apps"))
        .timeout(Duration.ofSeconds(10)).header(HttpHeaders.ACCEPT, "text/html").GET().build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(302, response.statusCode());
    assertTrue(response.headers().firstValue(HttpHeaders.LOCATION).orElse("")
        .endsWith("/oauth2/authorization/test"));
  }

  @Configuration(proxyBeanMethods = false)
  @Profile("oidc-expired-session-http-test")
  @ImportAutoConfiguration({TomcatServletWebServerAutoConfiguration.class,
      DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
      ErrorMvcAutoConfiguration.class, SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class})
  @Import({AuthConfiguration.OidcWebSecurityConfigurerAdapter.class, AuthFilterConfiguration.class})
  static class TestConfiguration {

    @Bean
    InMemoryClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("test").clientId("portal").clientSecret("unused")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").scope("openid")
              .authorizationUri("https://idp.example/authorize")
              .tokenUri("https://idp.example/token").jwkSetUri("https://idp.example/keys").build());
    }

    @Bean
    OAuth2ResourceServerProperties oauth2ResourceServerProperties() {
      return new OAuth2ResourceServerProperties();
    }

    @Bean
    UserTokenAuthUtil userTokenAuthUtil() {
      return new UserTokenAuthUtil();
    }

    @Bean
    ConsumerService consumerService() {
      return mock(ConsumerService.class);
    }

    @Bean
    PortalManagementController portalManagementController(ConsumerService consumerService,
        UserTokenService userTokenService) {
      return new PortalManagementController(mock(ApolloAuditLogApi.class),
          new ApolloAuditProperties(), mock(CommitService.class),
          mock(UnifiedPermissionValidator.class), mock(PortalConfig.class), consumerService,
          mock(FavoriteService.class), mock(GlobalSearchService.class),
          mock(ReleaseHistoryService.class), mock(ServerConfigService.class),
          mock(UserInfoHolder.class), mock(ConfigsExportService.class),
          mock(ConfigsImportService.class), mock(NamespaceService.class), mock(AppService.class),
          mock(PortalSettings.class), mock(RestTemplateFactory.class),
          mock(PortalMetaDomainService.class), new ObjectMapper(), userTokenService);
    }
  }
}
