/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.biz.registry.configuration;

import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClient;
import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClientImpl;
import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloRegistryClearApplicationRunner;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloRegistryDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.RegistryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = ApolloRegistryDiscoveryProperties.PREFIX,
    value = "enabled"
)
@EnableConfigurationProperties({
    ApolloRegistryDiscoveryProperties.class,
})
public class ApolloRegistryDiscoveryAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public DatabaseDiscoveryClient databaseDiscoveryClient(
      ApolloRegistryDiscoveryProperties discoveryProperties,
      ServiceInstance selfServiceInstance,
      RegistryService registryService
  ) {
    return new DatabaseDiscoveryClientImpl(
        registryService, discoveryProperties, selfServiceInstance
    );
  }

  @Bean
  @ConditionalOnMissingBean
  public ApolloRegistryClearApplicationRunner apolloRegistryClearApplicationRunner(
      RegistryService registryService
  ) {
    return new ApolloRegistryClearApplicationRunner(registryService);
  }
}
