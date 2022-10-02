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

import com.ctrip.framework.apollo.biz.entity.Registry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloRegistryDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.RegistryService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseDiscoveryClientImplTest {

  static Registry newRegistry(String serviceName, String uri, String label) {
    Registry registry = new Registry();
    registry.setServiceName(serviceName);
    registry.setUri(uri);
    registry.setLabel(label);
    registry.setDataChangeCreatedTime(LocalDateTime.now());
    registry.setDataChangeLastModifiedTime(LocalDateTime.now());
    return registry;
  }

  @Test
  void getInstancesWithoutLabel() {
    final String serviceName = "a-service";
    RegistryService registryService = Mockito.mock(RegistryService.class);
    {
      List<Registry> registryList = Arrays.asList(
          newRegistry(serviceName, "http://localhost:8081/", "label1"),
          newRegistry(serviceName, "http://localhost:8082/", "label2")
      );
      Mockito.when(registryService.findByServiceName(serviceName))
          .thenReturn(registryList);
      Mockito.when(registryService.getTimeBeforeSeconds(anyLong()))
          .thenReturn(LocalDateTime.now().minusMinutes(1));
    }

    ApolloRegistryDiscoveryProperties properties = new ApolloRegistryDiscoveryProperties();
    properties.setFilterByLabel(false);

    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        registryService,
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
    RegistryService registryService = Mockito.mock(RegistryService.class);
    {
      List<Registry> registryList = Arrays.asList(
          newRegistry(serviceName, "http://localhost:8081/", "label1"),
          newRegistry("b-service", "http://localhost:8082/", "label2"),
          newRegistry("c-service", "http://localhost:8082/", "label3")
      );
      Mockito.when(registryService.findByServiceName(serviceName))
          .thenReturn(registryList);
      Mockito.when(registryService.getTimeBeforeSeconds(anyLong()))
          .thenReturn(LocalDateTime.now().minusMinutes(1));
    }

    ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);
    Mockito.when(serviceInstance.getLabel()).thenReturn("label1");
    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        registryService,
        new ApolloRegistryDiscoveryProperties(),
        serviceInstance
    );

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
    assertEquals(serviceName, serviceInstances.get(0).getServiceName());
    assertEquals("label1", serviceInstances.get(0).getLabel());
  }
}