package com.ctrip.framework.apollo.adminservice;

import com.ctrip.framework.apollo.adminservice.filter.AdminServiceAuthenticationFilter;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminServiceAutoConfiguration {

  private final BizConfig bizConfig;

  public AdminServiceAutoConfiguration(final BizConfig bizConfig) {
    this.bizConfig = bizConfig;
  }

  @Bean
  public FilterRegistrationBean<AdminServiceAuthenticationFilter> adminServiceAuthenticationFilter() {
    FilterRegistrationBean<AdminServiceAuthenticationFilter> filterRegistrationBean = new FilterRegistrationBean<>();

    filterRegistrationBean.setFilter(new AdminServiceAuthenticationFilter(bizConfig));
    filterRegistrationBean.addUrlPatterns("/apps/*");
    filterRegistrationBean.addUrlPatterns("/appnamespaces/*");
    filterRegistrationBean.addUrlPatterns("/instances/*");
    filterRegistrationBean.addUrlPatterns("/items/*");
    filterRegistrationBean.addUrlPatterns("/namespaces/*");
    filterRegistrationBean.addUrlPatterns("/releases/*");

    return filterRegistrationBean;
  }
}
