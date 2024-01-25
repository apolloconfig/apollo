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
import com.ctrip.framework.apollo.biz.registry.DatabaseServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryClearApplicationRunner;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryDeregisterApplicationListener;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryHeartbeatApplicationRunner;
import com.ctrip.framework.apollo.biz.repository.ServiceRegistryRepository;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * ensure that this feature, i.e. database discovery won't cause configservice or adminservice
 * startup fail when it doesn't enable.
 */
@SpringBootTest
@ContextConfiguration(classes = {
    ApolloServiceRegistryAutoConfiguration.class,
    ApolloServiceDiscoveryAutoConfiguration.class
})
class ApolloServiceRegistryAutoConfigurationNotEnabledTest {

  @Autowired
  private ApplicationContext context;


  private void assertNoSuchBean(Class<?> requiredType) {
    Assertions.assertThrows(
        NoSuchBeanDefinitionException.class,
        () -> context.getBean(requiredType)
    );
  }

  @Test
  void ensureNoSuchBeans() {
    assertNoSuchBean(ServiceRegistryRepository.class);
    assertNoSuchBean(ServiceRegistryService.class);
    assertNoSuchBean(DatabaseServiceRegistry.class);
    assertNoSuchBean(ApolloServiceRegistryHeartbeatApplicationRunner.class);
    assertNoSuchBean(ApolloServiceRegistryDeregisterApplicationListener.class);

    assertNoSuchBean(DatabaseDiscoveryClient.class);
    assertNoSuchBean(ApolloServiceRegistryClearApplicationRunner.class);
  }
}