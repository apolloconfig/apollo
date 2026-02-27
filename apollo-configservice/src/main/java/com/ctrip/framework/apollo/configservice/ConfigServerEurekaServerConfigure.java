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
package com.ctrip.framework.apollo.configservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * Start Eureka Server annotations according to configuration
 *
 * @author Zhiqiang Lin(linzhiqiang0514@163.com)
 */
@Configuration
@EnableEurekaServer
@ConditionalOnProperty(name = "apollo.eureka.server.enabled", havingValue = "true",
    matchIfMissing = true)
public class ConfigServerEurekaServerConfigure {

  @Order(99)
  @Configuration
  static class EurekaServerSecurityConfigurer {

    private static final String EUREKA_ROLE = "EUREKA";

    @Value("${apollo.eureka.server.security.enabled:false}")
    private boolean eurekaSecurityEnabled;
    @Value("${apollo.eureka.server.security.username:}")
    private String username;
    @Value("${apollo.eureka.server.security.password:}")
    private String password;

    @Bean
    @Order(99)
    public SecurityFilterChain eurekaServerSecurityFilterChain(HttpSecurity http) throws Exception {
      http.securityMatcher("/eureka/**");
      http.csrf(csrf -> csrf.disable());
      http.httpBasic(Customizer.withDefaults());
      if (eurekaSecurityEnabled) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider
            .setUserDetailsService(new InMemoryUserDetailsManager(User.withUsername(username)
                .password(toDelegatingPassword(password)).roles(EUREKA_ROLE).build()));
        http.authenticationProvider(authenticationProvider);
        http.authorizeHttpRequests(
            authorizeHttpRequests -> authorizeHttpRequests.requestMatchers("/eureka/apps/**",
                "/eureka/instances/**", "/eureka/peerreplication/**").hasRole(EUREKA_ROLE)
                .anyRequest().permitAll());
      }
      return http.build();
    }

    private String toDelegatingPassword(String configuredPassword) {
      if (!StringUtils.hasText(configuredPassword)) {
        return "{noop}";
      }
      if (configuredPassword.startsWith("{")) {
        return configuredPassword;
      }
      return "{noop}" + configuredPassword;
    }
  }
}
