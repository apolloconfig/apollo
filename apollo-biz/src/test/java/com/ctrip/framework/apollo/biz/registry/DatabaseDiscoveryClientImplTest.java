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
package com.ctrip.framework.apollo.biz.registry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseDiscoveryClientImplTest {

  static ServiceRegistry newRegistry(String serviceName, String uri, String cluster) {
    ServiceRegistry serviceRegistry = new ServiceRegistry();
    serviceRegistry.setServiceName(serviceName);
    serviceRegistry.setUri(uri);
    serviceRegistry.setCluster(cluster);
    serviceRegistry.setDataChangeCreatedTime(LocalDateTime.now());
    serviceRegistry.setDataChangeLastModifiedTime(LocalDateTime.now());
    return serviceRegistry;
  }

  @Test
  void getInstances() {
    final String serviceName = "a-service";
    ServiceRegistryService serviceRegistryService = Mockito.mock(ServiceRegistryService.class);
    {
      List<ServiceRegistry> serviceRegistryList = Arrays.asList(
          newRegistry(serviceName, "http://localhost:8081/", "1"),
          newRegistry("b-service", "http://localhost:8082/", "2"),
          newRegistry("c-service", "http://localhost:8082/", "3")
      );
      Mockito.when(
              serviceRegistryService.findByServiceNameDataChangeLastModifiedTimeGreaterThan(eq(serviceName),
                  any(LocalDateTime.class)))
          .thenReturn(serviceRegistryList);
    }

    ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);
    Mockito.when(serviceInstance.getCluster()).thenReturn("1");
    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        serviceRegistryService,
        new ApolloServiceDiscoveryProperties(),
        serviceInstance
    );

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
    assertEquals(serviceName, serviceInstances.get(0).getServiceName());
    assertEquals("1", serviceInstances.get(0).getCluster());
  }
}