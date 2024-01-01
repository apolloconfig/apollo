/*
 * Copyright 2024 Apollo Authors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Start Eureka Server annotations according to configuration
 *
 * @author Zhiqiang Lin(linzhiqiang0514@163.com)
 */
@Configuration
@EnableEurekaServer
@ConditionalOnProperty(name = "apollo.eureka.server.enabled", havingValue = "true", matchIfMissing = true)
public class ConfigServerEurekaServerConfigure {

  @Order(99)
  @Configuration
  static class EurekaServerSecurityConfigurer extends WebSecurityConfigurerAdapter {

    private static final String EUREKA_ROLE = "EUREKA";

    @Value("${apollo.eureka.server.security.enabled:false}")
    private boolean eurekaSecurityEnabled;
    @Value("${apollo.eureka.server.security.username:}")
    private String username;
    @Value("${apollo.eureka.server.security.password:}")
    private String password;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable();
      http.httpBasic();
      if (eurekaSecurityEnabled) {
        http.authorizeRequests()
            .antMatchers("/eureka/apps/**", "/eureka/instances/**", "/eureka/peerreplication/**")
            .hasRole(EUREKA_ROLE)
            .antMatchers("/**").permitAll();
      }
    }

    @Autowired
    public void configureEurekaUser(AuthenticationManagerBuilder auth) throws Exception {
      if (!eurekaSecurityEnabled) {
        return;
      }
      InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> configurer = auth
          .getConfigurer(InMemoryUserDetailsManagerConfigurer.class);
      if (configurer == null) {
        configurer = auth.inMemoryAuthentication();
      }
      configurer.withUser(username).password(password).roles(EUREKA_ROLE);
    }
  }
}
