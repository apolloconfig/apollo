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
package com.ctrip.framework.apollo.biz.registry.configuration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.configuration.ApolloServiceDiscoveryAutoConfiguration;
import com.ctrip.framework.apollo.biz.registry.configuration.ApolloServiceRegistryAutoConfiguration;
import com.ctrip.framework.apollo.biz.repository.ServiceRegistryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
        "apollo.service.registry.enabled=true",
        "apollo.service.registry.cluster=default",
        "apollo.service.discovery.enabled=true",
        "spring.application.name=for-test-service",
        "server.port=10000",
    }
)
@ContextConfiguration(classes = {
    ApolloServiceRegistryAutoConfiguration.class,
    ApolloServiceDiscoveryAutoConfiguration.class,
})
public class ApolloServiceRegistryClearApplicationRunnerIntegrationTest
    extends AbstractIntegrationTest {

  @Autowired
  private ServiceRegistryRepository repository;

  @Autowired
  private ApolloServiceRegistryClearApplicationRunner runner;

  @Test
  public void clearUnhealthyInstances() {
    final String serviceName = "h-service";

    final String healthUri = "http://10.240.11.22:8080/";
    ServiceRegistry healthy = new ServiceRegistry();
    healthy.setServiceName(serviceName);
    healthy.setCluster("c-1");
    healthy.setUri(healthUri);
    healthy.setDataChangeCreatedTime(LocalDateTime.now());
    healthy.setDataChangeLastModifiedTime(LocalDateTime.now());
    this.repository.save(healthy);

    LocalDateTime unhealthyTime = LocalDateTime.now().minusDays(2L);
    ServiceRegistry unhealthy = new ServiceRegistry();
    unhealthy.setServiceName("h-service");
    unhealthy.setCluster("c-2");
    unhealthy.setUri("http://10.240.33.44:9090/");
    unhealthy.setDataChangeCreatedTime(unhealthyTime);
    unhealthy.setDataChangeLastModifiedTime(unhealthyTime);
    this.repository.save(unhealthy);

    {
      List<ServiceRegistry> serviceRegistryList = this.repository.findByServiceNameAndDataChangeLastModifiedTimeGreaterThan(
          serviceName,
          LocalDateTime.now().minusDays(3L)
      );
      assertEquals(2, serviceRegistryList.size());
    }

    runner.clearUnhealthyInstances();

    {
      List<ServiceRegistry> serviceRegistryList = this.repository.findByServiceNameAndDataChangeLastModifiedTimeGreaterThan(
          serviceName,
          LocalDateTime.now().minusDays(3L)
      );
      assertEquals(1, serviceRegistryList.size());
      ServiceRegistry registry = serviceRegistryList.get(0);
      assertEquals(serviceName, registry.getServiceName());
      assertEquals(healthUri, registry.getUri());
    }
  }

}