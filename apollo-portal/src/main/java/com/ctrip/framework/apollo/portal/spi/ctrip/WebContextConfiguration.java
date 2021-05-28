/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.portal.spi.ctrip;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.ctrip.filters.UserAccessFilter;
import com.google.common.base.Strings;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("ctrip")
public class WebContextConfiguration {

  private final PortalConfig portalConfig;
  private final UserInfoHolder userInfoHolder;

  public WebContextConfiguration(final PortalConfig portalConfig, final UserInfoHolder userInfoHolder) {
    this.portalConfig = portalConfig;
    this.userInfoHolder = userInfoHolder;
  }

  @Bean
  public ServletContextInitializer servletContextInitializer() {
    return servletContext -> {
      String loggingServerIP = portalConfig.cloggingUrl();
      String loggingServerPort = portalConfig.cloggingPort();
      String credisServiceUrl = portalConfig.credisServiceUrl();

      servletContext.setInitParameter("loggingServerIP",
          Strings.isNullOrEmpty(loggingServerIP) ? "" : loggingServerIP);
      servletContext.setInitParameter("loggingServerPort",
          Strings.isNullOrEmpty(loggingServerPort) ? "" : loggingServerPort);
      servletContext.setInitParameter("credisServiceUrl",
          Strings.isNullOrEmpty(credisServiceUrl) ? "" : credisServiceUrl);
    };
  }

  @Bean
  public FilterRegistrationBean userAccessFilter() {
    FilterRegistrationBean filter = new FilterRegistrationBean();
    filter.setFilter(new UserAccessFilter(userInfoHolder));
    filter.addUrlPatterns("/*");
    return filter;
  }

}
