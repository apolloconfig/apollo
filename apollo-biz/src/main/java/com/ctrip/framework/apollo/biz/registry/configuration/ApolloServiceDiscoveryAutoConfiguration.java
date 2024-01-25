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
package com.ctrip.framework.apollo.biz.registry.configuration;

import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClient;
import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl;
import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClientImpl;
import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClientMemoryCacheDecoratorImpl;
import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryClearApplicationRunner;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = ApolloServiceDiscoveryProperties.PREFIX,
    value = "enabled"
)
@EnableConfigurationProperties({
    ApolloServiceDiscoveryProperties.class,
})
public class ApolloServiceDiscoveryAutoConfiguration {


  private static DatabaseDiscoveryClient wrapMemoryCache(DatabaseDiscoveryClient discoveryClient) {
    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(discoveryClient);
    decorator.init();
    return decorator;
  }

  private static DatabaseDiscoveryClient wrapAlwaysAddSelfInstance(
      DatabaseDiscoveryClient discoveryClient,
      ServiceInstance selfInstance
  ) {
    return new DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl(
            discoveryClient, selfInstance
    );
  }

  @Bean
  @ConditionalOnMissingBean
  public DatabaseDiscoveryClient databaseDiscoveryClient(
      ApolloServiceDiscoveryProperties discoveryProperties,
      ServiceInstance selfServiceInstance,
      ServiceRegistryService serviceRegistryService
  ) {
    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        serviceRegistryService, discoveryProperties, selfServiceInstance.getCluster()
    );
    return wrapMemoryCache(
        wrapAlwaysAddSelfInstance(discoveryClient, selfServiceInstance)
    );
  }

  @Bean
  @ConditionalOnMissingBean
  public ApolloServiceRegistryClearApplicationRunner apolloServiceRegistryClearApplicationRunner(
      ServiceRegistryService serviceRegistryService
  ) {
    return new ApolloServiceRegistryClearApplicationRunner(serviceRegistryService);
  }
}
