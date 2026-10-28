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

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Preserves Apollo's explicit Session store selection with Boot 4's modular auto-configuration.
 */
public class SessionStoreAutoConfigurationImportFilter
    implements AutoConfigurationImportFilter, EnvironmentAware {

  private Environment environment;

  @Override
  public boolean[] match(String[] autoConfigurationClasses,
      AutoConfigurationMetadata autoConfigurationMetadata) {
    String storeType = environment.getProperty("spring.session.store-type");
    boolean[] matches = new boolean[autoConfigurationClasses.length];
    for (int i = 0; i < autoConfigurationClasses.length; i++) {
      String candidate = autoConfigurationClasses[i];
      if (storeType == null || candidate == null) {
        matches[i] = true;
      } else if (candidate.equals(
          "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration")) {
        matches[i] = "jdbc".equalsIgnoreCase(storeType);
      } else if (candidate.equals(
          "org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration")) {
        matches[i] = "redis".equalsIgnoreCase(storeType);
      } else if (candidate
          .equals("org.springframework.boot.session.autoconfigure.SessionAutoConfiguration")) {
        matches[i] = !"none".equalsIgnoreCase(storeType);
      } else {
        matches[i] = true;
      }
    }
    return matches;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
