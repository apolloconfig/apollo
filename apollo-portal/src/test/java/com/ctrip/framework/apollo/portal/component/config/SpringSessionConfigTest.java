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
package com.ctrip.framework.apollo.portal.component.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

/**
 * Verifies Boot discovers the Session store and shares OAuth2 state between Portal instances.
 */
class SpringSessionConfigTest {

  private WebApplicationContextRunner contextRunner(String database) {
    return contextRunner(database, new WebApplicationContextRunner());
  }

  private WebApplicationContextRunner contextRunner(String database,
      WebApplicationContextRunner runner) {
    return runner.withUserConfiguration(SessionApplication.class).withPropertyValues(
        "server.address=127.0.0.1", "server.port=0",
        "spring.datasource.url=jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.session.store-type=jdbc",
        "spring.session.jdbc.initialize-schema=never", "spring.session.jdbc.cleanup-cron=-",
        "spring.jpa.hibernate.ddl-auto=none", "spring.cloud.discovery.enabled=false",
        "spring.cloud.consul.enabled=false", "spring.cloud.zookeeper.enabled=false");
  }

  @Test
  void shouldAutoConfigureJdbcWithoutCreatingTablesOrUsingRedis() {
    contextRunner(UUID.randomUUID().toString())
        .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
        .run(context -> {
          assertThat(context).hasNotFailed().hasSingleBean(JdbcIndexedSessionRepository.class)
              .hasSingleBean(SessionRepositoryFilter.class)
              .doesNotHaveBean(RedisSessionRepository.class);
          Integer tableCount = new JdbcTemplate(context.getBean(DataSource.class)).queryForObject(
              "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'SPRING_SESSION'",
              Integer.class);
          assertThat(tableCount).isZero();
        });
  }

  @Test
  void shouldDisableSpringSessionWhenStoreTypeIsNone() {
    contextRunner(UUID.randomUUID().toString()).withPropertyValues("spring.session.store-type=none")
        .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
        .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(SessionRepository.class)
            .doesNotHaveBean(SessionRepositoryFilter.class));
  }

  @Test
  void shouldSelectRedisEvenWhenDataSourceIsAvailable() {
    contextRunner(UUID.randomUUID().toString())
        .withPropertyValues("spring.session.store-type=redis")
        .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
        .run(context -> assertThat(context).hasNotFailed()
            .hasSingleBean(RedisSessionRepository.class)
            .hasSingleBean(SessionRepositoryFilter.class)
            .doesNotHaveBean(JdbcIndexedSessionRepository.class));
  }

  @Test
  void shouldBackOffWhenSessionRepositoryIsProvided() {
    SessionRepository<?> repository = mock(SessionRepository.class);
    contextRunner(UUID.randomUUID().toString()).withBean(SessionRepository.class, () -> repository)
        .run(context -> {
          assertThat(context).hasNotFailed().hasSingleBean(SessionRepository.class)
              .doesNotHaveBean(JdbcIndexedSessionRepository.class);
          assertThat(context.getBean(SessionRepository.class)).isSameAs(repository);
        });
  }

  @Test
  void shouldBindJdbcTableTimeoutAndCookieSettings() {
    contextRunner(UUID.randomUUID().toString(),
        new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new))
        .withPropertyValues("spring.session.jdbc.table-name=APOLLO_SESSION",
            "spring.session.timeout=42m", "server.servlet.session.cookie.name=APOLLO_SESSION_ID",
            "server.servlet.session.cookie.same-site=strict")
        .run(context -> {
          assertThat(context).hasNotFailed();
          DataSource dataSource = context.getBean(DataSource.class);
          initializeSchema(dataSource);
          JdbcTemplate jdbc = new JdbcTemplate(dataSource);
          jdbc.execute("ALTER TABLE SPRING_SESSION RENAME TO APOLLO_SESSION");
          jdbc.execute("ALTER TABLE SPRING_SESSION_ATTRIBUTES RENAME TO APOLLO_SESSION_ATTRIBUTES");
          MockHttpServletResponse response = new MockHttpServletResponse();
          context.getBean(SessionRepositoryFilter.class).doFilter(new MockHttpServletRequest(),
              response, (request, servletResponse) -> {
                assertThat(((HttpServletRequest) request).getSession().getMaxInactiveInterval())
                    .isEqualTo((int) Duration.ofMinutes(42).getSeconds());
                ((HttpServletRequest) request).getSession().setAttribute("marker", "persisted");
              });
          assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM APOLLO_SESSION", Integer.class))
              .isEqualTo(1);
          assertThat(response.getCookie("APOLLO_SESSION_ID")).isNotNull();
          assertThat(response.getHeader("Set-Cookie")).contains("SameSite=Strict");
        });
  }

  @Test
  void shouldRestoreOAuth2AuthorizationRequestOnAnotherInstance() {
    String database = UUID.randomUUID().toString();
    contextRunner(database).run(first -> {
      assertThat(first).hasNotFailed();
      initializeSchema(first.getBean(DataSource.class));
      contextRunner(database).run(second -> {
        assertThat(second).hasNotFailed();
        assertThat(first.getBean(SessionRepository.class))
            .isNotSameAs(second.getBean(SessionRepository.class));
        HttpSessionOAuth2AuthorizationRequestRepository authorizationRequests =
            new HttpSessionOAuth2AuthorizationRequestRepository();
        OAuth2AuthorizationRequest authorization = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://identity.example/authorize").clientId("portal")
            .redirectUri("https://portal.example/login/oauth2/code/test").state("shared-state")
            .build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        first.getBean(SessionRepositoryFilter.class).doFilter(new MockHttpServletRequest(),
            response,
            (request, servletResponse) -> authorizationRequests.saveAuthorizationRequest(
                authorization, (HttpServletRequest) request,
                (HttpServletResponse) servletResponse));
        assertThat(response.getCookie("SESSION")).isNotNull();

        MockHttpServletRequest callback = new MockHttpServletRequest();
        callback.setCookies(response.getCookie("SESSION"));
        callback.setParameter("state", authorization.getState());
        second.getBean(SessionRepositoryFilter.class).doFilter(callback,
            new MockHttpServletResponse(), (request, servletResponse) -> {
              OAuth2AuthorizationRequest restored =
                  authorizationRequests.removeAuthorizationRequest((HttpServletRequest) request,
                      (HttpServletResponse) servletResponse);
              assertThat(restored).isNotNull();
              assertThat(restored.getState()).isEqualTo(authorization.getState());
              assertThat(restored.getAuthorizationRequestUri())
                  .isEqualTo(authorization.getAuthorizationRequestUri());
            });
      });
    });
  }

  private void initializeSchema(DataSource dataSource) {
    new ResourceDatabasePopulator(
        new ClassPathResource("org/springframework/session/jdbc/schema-h2.sql"))
        .execute(dataSource);
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @Import(SpringSessionConfig.class)
  static class SessionApplication {
  }
}
