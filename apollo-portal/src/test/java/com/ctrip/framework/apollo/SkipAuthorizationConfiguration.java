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
package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kezhenxu at 2019/1/8 20:19.
 *
 * Configuration class that will disable authorization.
 *
 * @author kezhenxu (kezhenxu94@163.com)
 */
@Profile("skipAuthorization")
@Configuration
public class SkipAuthorizationConfiguration {
  @Primary
  @Bean
  public ConsumerPermissionValidator consumerPermissionValidator() {
    final ConsumerPermissionValidator mock = mock(ConsumerPermissionValidator.class);
    when(mock.hasCreateNamespacePermission(any())).thenReturn(true);
    return mock;
  }

  @Primary
  @Bean
  public ConsumerAuthUtil consumerAuthUtil() {
    final ConsumerAuthUtil mock = mock(ConsumerAuthUtil.class);
    when(mock.getConsumerId(any())).thenReturn(1L);

    ConsumerToken someConsumerToken = new ConsumerToken();
    someConsumerToken.setConsumerId(1L);
    someConsumerToken.setToken("some-token");
    someConsumerToken.setRateLimit(20);
    when(mock.getConsumerToken(any())).thenReturn(someConsumerToken);
    doAnswer(invocation -> {
      HttpServletRequest request = invocation.getArgument(0);
      Long consumerId = invocation.getArgument(1);
      request.setAttribute(ConsumerAuthUtil.CONSUMER_ID, consumerId);
      return null;
    }).when(mock).storeConsumerId(any(HttpServletRequest.class), any());
    return mock;
  }

  @Primary
  @Bean("userPermissionValidator")
  public UserPermissionValidator permissionValidator() {
    final UserPermissionValidator mock = mock(UserPermissionValidator.class);
    when(mock.isSuperAdmin()).thenReturn(true);
    when(mock.hasAssignRolePermission(any())).thenReturn(true);
    when(mock.hasCreateNamespacePermission(any())).thenReturn(true);
    return mock;
  }

  @Bean
  public FilterRegistrationBean<Filter> skipAuthorizationUserAuthenticationFilter() {
    FilterRegistrationBean<Filter> filter = new FilterRegistrationBean<>();
    filter.setFilter((request, response, chain) -> {
      try {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getRequestURI().startsWith("/openapi/")) {
          chain.doFilter(request, response);
          return;
        }
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("apollo", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
        chain.doFilter(request, response);
      } finally {
        SecurityContextHolder.clearContext();
      }
    });
    filter.addUrlPatterns("/*");
    filter.setOrder(-98);
    return filter;
  }
}
