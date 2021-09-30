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
package com.ctrip.framework.apollo.config.data;

import com.ctrip.framework.apollo.config.data.extension.initialize.ApolloClientPropertiesFactory;
import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientProperties;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourcesProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
@Configuration(proxyBeanMethods = false)
public class ApolloClientConfigDataAutoConfiguration {

  @ConditionalOnMissingBean(ApolloClientProperties.class)
  @ConfigurationProperties(ApolloClientPropertiesFactory.PROPERTIES_PREFIX)
  @Bean
  public static ApolloClientProperties apolloWebClientSecurityProperties() {
    return new ApolloClientProperties();
  }

  @ConditionalOnMissingBean(PropertySourcesProcessor.class)
  @Bean
  public static ConfigPropertySourcesProcessor configPropertySourcesProcessor() {
    return new ConfigPropertySourcesProcessor();
  }
}
