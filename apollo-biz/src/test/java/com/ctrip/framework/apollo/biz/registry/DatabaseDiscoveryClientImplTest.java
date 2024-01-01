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
package com.ctrip.framework.apollo.biz.registry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseDiscoveryClientImplTest {

  private static ServiceRegistry newServiceRegistry(
      String serviceName, String uri, String cluster, LocalDateTime dataChangeLastModifiedTime
  ) {
    ServiceRegistry serviceRegistry = new ServiceRegistry();
    serviceRegistry.setServiceName(serviceName);
    serviceRegistry.setUri(uri);
    serviceRegistry.setCluster(cluster);
    serviceRegistry.setMetadata(new HashMap<>());
    serviceRegistry.setDataChangeCreatedTime(LocalDateTime.now());
    serviceRegistry.setDataChangeLastModifiedTime(dataChangeLastModifiedTime);
    return serviceRegistry;
  }

  private static ServiceRegistry newServiceRegistry(String serviceName, String uri,
      String cluster) {
    return newServiceRegistry(serviceName, uri, cluster, LocalDateTime.now());
  }

  @Test
  void getInstances_filterByCluster() {
    final String serviceName = "a-service";
    ServiceRegistryService serviceRegistryService = Mockito.mock(ServiceRegistryService.class);
    {
      List<ServiceRegistry> serviceRegistryList = Arrays.asList(
          newServiceRegistry(serviceName, "http://localhost:8081/", "1"),
          newServiceRegistry("b-service", "http://localhost:8082/", "2"),
          newServiceRegistry("c-service", "http://localhost:8082/", "3")
      );
      Mockito.when(
              serviceRegistryService.findByServiceNameDataChangeLastModifiedTimeGreaterThan(
                  eq(serviceName),
                  any(LocalDateTime.class)))
          .thenReturn(serviceRegistryList);
    }

    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        serviceRegistryService,
        new ApolloServiceDiscoveryProperties(),
        "1"
    );

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
    assertEquals(serviceName, serviceInstances.get(0).getServiceName());
    assertEquals("1", serviceInstances.get(0).getCluster());
  }

  @Test
  void getInstances_filterByHealthCheck() {
    final String serviceName = "a-service";
    ServiceRegistryService serviceRegistryService = Mockito.mock(ServiceRegistryService.class);

    ServiceRegistry healthy = newServiceRegistry(serviceName, "http://localhost:8081/", "1",
        LocalDateTime.now());
    Mockito.when(
            serviceRegistryService.findByServiceNameDataChangeLastModifiedTimeGreaterThan(
                eq(serviceName),
                any(LocalDateTime.class)))
        .thenReturn(Collections.singletonList(healthy));

    DatabaseDiscoveryClient discoveryClient = new DatabaseDiscoveryClientImpl(
        serviceRegistryService,
        new ApolloServiceDiscoveryProperties(),
        "1"
    );

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
    assertEquals(serviceName, serviceInstances.get(0).getServiceName());
    assertEquals("http://localhost:8081/", serviceInstances.get(0).getUri().toString());
    assertEquals("1", serviceInstances.get(0).getCluster());
  }
}