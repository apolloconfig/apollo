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
