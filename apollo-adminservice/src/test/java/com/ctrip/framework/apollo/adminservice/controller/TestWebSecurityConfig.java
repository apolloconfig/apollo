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
package com.ctrip.framework.apollo.adminservice.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(0)
public class TestWebSecurityConfig {

  @Bean
  @Order(0)
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/", "/console/**");
    http.httpBasic(Customizer.withDefaults());
    http.csrf(csrf -> csrf.disable());
    http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers("/")
        .permitAll().requestMatchers("/console/**").permitAll());
    http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
    return http.build();
  }
}
