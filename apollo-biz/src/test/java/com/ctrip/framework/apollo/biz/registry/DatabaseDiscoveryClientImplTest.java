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
import static org.mockito.ArgumentMatchers.anyLong;

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseDiscoveryClientImplTest {

  static ServiceRegistry newRegistry(String serviceName, String uri, String label) {
    ServiceRegistry serviceRegistry = new ServiceRegistry();
    serviceRegistry.setServiceName(serviceName);
    serviceRegistry.setUri(uri);
    serviceRegistry.setCluster(label);
    serviceRegistry.setDataChangeCreatedTime(LocalDateTime.now());
    serviceRegistry.setDataChangeLastModifiedTime(LocalDateTime.now());
    return serviceRegistry;
  }

  @Test
  void getInstancesWithoutLabel() {
    final String serviceName = "a-service";
    ServiceRegistryService serviceRegistryService = Mockito.mock(ServiceRegistryService.class);
    {
      List<ServiceRegistry> serviceRegistryList = Arrays.asList(
          newRegistry(serviceName, "http://localhost:8081/", "label1"),
          newRegistry(serviceName, "http://localhost:8082/", "label2")
      );
      Mockito.when(serviceRegistryService.findByServiceName(serviceName))
          .thenReturn(serviceRegistryList);
      Mockito.when(serviceRegistryService.getTimeBeforeSeconds(anyLong()))
          .thenReturn(LocalDateTime.now().minusMinutes(1));
    }

    ApolloServiceDiscoveryProperties properties = new ApolloServiceDiscoveryProperties();
    properties.setFilterByLabel(false);

    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        serviceRegistryService,
        properties,
        null
    );

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
    assertEquals(2, serviceInstances.size());
    serviceInstances.forEach(
        serviceInstance -> assertEquals(serviceName, serviceInstance.getServiceName())
    );
  }


  @Test
  void getInstancesWithLabel() {
    final String serviceName = "a-service";
    ServiceRegistryService serviceRegistryService = Mockito.mock(ServiceRegistryService.class);
    {
      List<ServiceRegistry> serviceRegistryList = Arrays.asList(
          newRegistry(serviceName, "http://localhost:8081/", "label1"),
          newRegistry("b-service", "http://localhost:8082/", "label2"),
          newRegistry("c-service", "http://localhost:8082/", "label3")
      );
      Mockito.when(serviceRegistryService.findByServiceName(serviceName))
          .thenReturn(serviceRegistryList);
      Mockito.when(serviceRegistryService.getTimeBeforeSeconds(anyLong()))
          .thenReturn(LocalDateTime.now().minusMinutes(1));
    }

    ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);
    Mockito.when(serviceInstance.getCluster()).thenReturn("label1");
    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        serviceRegistryService,
        new ApolloServiceDiscoveryProperties(),
        serviceInstance
    );

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
    assertEquals(serviceName, serviceInstances.get(0).getServiceName());
    assertEquals("label1", serviceInstances.get(0).getCluster());
  }
}