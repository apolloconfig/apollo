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

import com.ctrip.framework.apollo.biz.registry.DatabaseServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.DatabaseServiceRegistryImpl;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryDeregisterApplicationListener;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryHeartbeatApplicationRunner;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryProperties;
import com.ctrip.framework.apollo.biz.repository.ServiceRegistryRepository;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = ApolloServiceRegistryProperties.PREFIX, value = "enabled")
@EnableConfigurationProperties(ApolloServiceRegistryProperties.class)
public class ApolloServiceRegistryAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ServiceRegistryService registryService(ServiceRegistryRepository repository) {
    return new ServiceRegistryService(repository);
  }

  @Bean
  @ConditionalOnMissingBean
  public DatabaseServiceRegistry databaseServiceRegistry(
      ServiceRegistryService serviceRegistryService
  ) {
    return new DatabaseServiceRegistryImpl(serviceRegistryService);
  }

  @Bean
  @ConditionalOnMissingBean
  public ApolloServiceRegistryHeartbeatApplicationRunner apolloServiceRegistryHeartbeatApplicationRunner(
      ApolloServiceRegistryProperties registration,
      DatabaseServiceRegistry serviceRegistry
  ) {
    return new ApolloServiceRegistryHeartbeatApplicationRunner(registration, serviceRegistry);
  }

  @Bean
  @ConditionalOnMissingBean
  public ApolloServiceRegistryDeregisterApplicationListener apolloServiceRegistryDeregisterApplicationListener(
      ApolloServiceRegistryProperties registration,
      DatabaseServiceRegistry serviceRegistry
  ) {
    return new ApolloServiceRegistryDeregisterApplicationListener(registration, serviceRegistry);
  }

}
