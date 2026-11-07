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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

/**
 * Tests explicit Session store selection without filtering unrelated auto-configurations.
 */
class SessionStoreAutoConfigurationImportFilterTest {

  private static final String[] CANDIDATES = {
      "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration",
      "org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration",
      "org.springframework.boot.session.autoconfigure.SessionAutoConfiguration",
      "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
      "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration", null};

  @Test
  void shouldSelectJdbc() {
    assertThat(matches("jdbc")).containsExactly(true, false, true, true, false, true);
  }

  @Test
  void shouldSelectRedisCaseInsensitively() {
    assertThat(matches("REDIS")).containsExactly(false, true, true, true, true, true);
  }

  @Test
  void shouldDisableAllSessionAutoConfiguration() {
    assertThat(matches("none")).containsExactly(false, false, false, true, false, true);
  }

  @Test
  void shouldLeaveBootDefaultsWhenStoreTypeIsAbsent() {
    assertThat(matches(null)).containsOnly(true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"jbdc", "", "mongodb"})
  void shouldRejectUnsupportedStoreTypes(String storeType) {
    assertThatIllegalArgumentException().isThrownBy(() -> matches(storeType))
        .withMessage("spring.session.store-type must be jdbc, redis or none");
  }

  private boolean[] matches(String storeType) {
    SessionStoreAutoConfigurationImportFilter filter =
        new SessionStoreAutoConfigurationImportFilter();
    MockEnvironment environment = new MockEnvironment();
    if (storeType != null) {
      environment.setProperty("spring.session.store-type", storeType);
    }
    filter.setEnvironment(environment);
    return filter.match(CANDIDATES, null);
  }

  @Test
  void shouldRetainIndependentlyConfiguredRedisWithJdbcSessions() {
    SessionStoreAutoConfigurationImportFilter filter =
        new SessionStoreAutoConfigurationImportFilter();
    filter.setEnvironment(new MockEnvironment().withProperty("spring.session.store-type", "jdbc")
        .withProperty("spring.data.redis.host", "redis.example"));
    assertThat(filter.match(CANDIDATES, null)).containsExactly(true, false, true, true, true, true);
  }
}
