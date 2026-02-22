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
package com.ctrip.framework.apollo.biz.auth;

import com.ctrip.framework.apollo.common.condition.ConditionalOnMissingProfile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@ConditionalOnMissingProfile({"auth", "assembly"})
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.httpBasic(Customizer.withDefaults());
    http.csrf(csrf -> csrf.disable());
    http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
    return http.build();
  }

  /**
   * Although the authentication below is useless, we may not remove them for backward compatibility.
   * Because if we remove them and the old clients(before 0.9.0) still send the authentication
   * information, the server will return 401, which should cause big problems.
   *
   * We may remove the following once we remove spring security from Apollo.
   */
  @Bean
  public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager(
        User.withUsername("user").password("{noop}").roles("USER").build(),
        User.withUsername("apollo").password("{noop}").roles("USER", "ADMIN").build());
  }

}
