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

import com.ctrip.framework.apollo.openapi.filter.ConsumerAuthenticationFilter;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.filter.PortalUserSessionFilter;
import com.ctrip.framework.apollo.portal.filter.UserTypeResolverFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AuthFilterConfiguration {

  private static final int OPEN_API_AUTH_ORDER = -98;

  @Bean
  public FilterRegistrationBean<ConsumerAuthenticationFilter> openApiAuthenticationFilter(
      ConsumerAuthUtil consumerAuthUtil, ConsumerAuditUtil consumerAuditUtil) {

    FilterRegistrationBean<ConsumerAuthenticationFilter> openApiFilter =
        new FilterRegistrationBean<>();

    openApiFilter.setFilter(new ConsumerAuthenticationFilter(consumerAuthUtil, consumerAuditUtil));
    openApiFilter.addUrlPatterns("/openapi/*");
    openApiFilter.setOrder(OPEN_API_AUTH_ORDER);

    return openApiFilter;
  }

  @Bean
  public FilterRegistrationBean<UserTypeResolverFilter> authTypeResolverFilter() {
    FilterRegistrationBean<UserTypeResolverFilter> authTypeResolverFilter =
        new FilterRegistrationBean<>();
    authTypeResolverFilter.setFilter(new UserTypeResolverFilter());
    authTypeResolverFilter.addUrlPatterns("/*");
    authTypeResolverFilter.setOrder(OPEN_API_AUTH_ORDER + 1);
    return authTypeResolverFilter;
  }

  /**
   * Portal user session filter for OpenAPI requests. This filter runs BEFORE
   * ConsumerAuthenticationFilter to: 1. Allow authenticated Portal users to access OpenAPI 2.
   * Redirect expired Portal sessions to login page (consistent with Portal endpoints)
   * <p>
   * Order: OPEN_API_AUTH_ORDER - 1 (runs first)
   */
  @Bean
  public FilterRegistrationBean<PortalUserSessionFilter> portalUserSessionFilter(
      Environment environment) {
    FilterRegistrationBean<PortalUserSessionFilter> filter = new FilterRegistrationBean<>();

    filter.setFilter(new PortalUserSessionFilter(environment));
    filter.addUrlPatterns("/openapi/*");
    filter.setOrder(OPEN_API_AUTH_ORDER - 1); // Run before ConsumerAuthenticationFilter after
                                              // springSecurityFilterChain

    return filter;
  }
}
