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

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceDiscoveryProperties;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceRegistryProperties;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DatabaseDiscoveryClientImpl implements DatabaseDiscoveryClient {
  private final ServiceRegistryService serviceRegistryService;

  private final ApolloServiceDiscoveryProperties discoveryProperties;

  private final String cluster;

  public DatabaseDiscoveryClientImpl(
      ServiceRegistryService serviceRegistryService,
      ApolloServiceDiscoveryProperties discoveryProperties,
      String cluster) {
    this.serviceRegistryService = serviceRegistryService;
    this.discoveryProperties = discoveryProperties;
    this.cluster = cluster;
  }

  /**
   * find by {@link ApolloServiceRegistryProperties#getServiceName()}
   */
  @Override
  public List<ServiceInstance> getInstances(String serviceName) {
    final List<ServiceRegistry> serviceRegistryListFiltered;
    {
      LocalDateTime healthTime = LocalDateTime.now()
          .minusSeconds(this.discoveryProperties.getHealthCheckIntervalInSecond());
      List<ServiceRegistry> filterByHealthCheck =
          this.serviceRegistryService.findByServiceNameDataChangeLastModifiedTimeGreaterThan(
              serviceName, healthTime
          );
      serviceRegistryListFiltered = filterByCluster(filterByHealthCheck, this.cluster);
    }

    return serviceRegistryListFiltered.stream()
        .map(DatabaseDiscoveryClientImpl::convert)
        .collect(Collectors.toList());
  }

  static ApolloServiceRegistryProperties convert(ServiceRegistry serviceRegistry) {
    ApolloServiceRegistryProperties registration = new ApolloServiceRegistryProperties();
    registration.setServiceName(serviceRegistry.getServiceName());
    registration.setUri(serviceRegistry.getUri());
    registration.setCluster(serviceRegistry.getCluster());
    return registration;
  }

  static List<ServiceRegistry> filterByCluster(List<ServiceRegistry> list, String cluster) {
    return list.stream()
        .filter(serviceRegistry -> Objects.equals(cluster, serviceRegistry.getCluster()))
        .collect(Collectors.toList());
  }

}
