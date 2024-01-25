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
package com.ctrip.framework.apollo.common.controller;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.DispatcherType;

@Configuration
public class CharacterEncodingFilterConfiguration {

  @Bean
  public FilterRegistrationBean encodingFilter() {
    FilterRegistrationBean bean = new FilterRegistrationBean();
    bean.setFilter(new CharacterEncodingFilter());
    bean.addInitParameter("encoding", "UTF-8");
    //FIXME: https://github.com/Netflix/eureka/issues/702
//    bean.addInitParameter("forceEncoding", "true");
    bean.setName("encodingFilter");
    bean.addUrlPatterns("/*");
    bean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
    return bean;
  }
}
