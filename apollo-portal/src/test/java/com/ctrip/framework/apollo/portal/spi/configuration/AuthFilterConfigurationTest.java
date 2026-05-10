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

import com.ctrip.framework.apollo.openapi.filter.ConsumerAuthenticationFilter;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.filter.PortalUserSessionFilter;
import com.ctrip.framework.apollo.portal.filter.UserTypeResolverFilter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;

public class AuthFilterConfigurationTest {

  @Test
  public void shouldRegisterOpenApiFiltersInAuthenticationOrder() {
    AuthFilterConfiguration configuration = new AuthFilterConfiguration();

    FilterRegistrationBean<PortalUserSessionFilter> portalUserSessionFilter =
        configuration.portalUserSessionFilter(mock(Environment.class));
    FilterRegistrationBean<ConsumerAuthenticationFilter> consumerAuthenticationFilter =
        configuration.openApiAuthenticationFilter(mock(ConsumerAuthUtil.class),
            mock(ConsumerAuditUtil.class));
    FilterRegistrationBean<UserTypeResolverFilter> userTypeResolverFilter =
        configuration.authTypeResolverFilter();

    assertTrue(portalUserSessionFilter.getOrder() < consumerAuthenticationFilter.getOrder());
    assertTrue(consumerAuthenticationFilter.getOrder() < userTypeResolverFilter.getOrder());
    assertEquals(Collections.singleton("/openapi/*"), portalUserSessionFilter.getUrlPatterns());
    assertEquals(Collections.singleton("/openapi/*"),
        consumerAuthenticationFilter.getUrlPatterns());
    assertEquals(Collections.singleton("/*"), userTypeResolverFilter.getUrlPatterns());
  }
}
