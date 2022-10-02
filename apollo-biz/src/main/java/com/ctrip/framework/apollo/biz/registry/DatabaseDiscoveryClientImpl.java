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

import com.ctrip.framework.apollo.biz.entity.Registry;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloRegistryDiscoveryProperties;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloRegistryClientProperties;
import com.ctrip.framework.apollo.biz.service.RegistryService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseDiscoveryClientImpl implements DatabaseDiscoveryClient {

  private static final Logger log = LoggerFactory.getLogger(DatabaseDiscoveryClientImpl.class);

  private final RegistryService registryService;

  private final ApolloRegistryDiscoveryProperties discoveryProperties;

  private final ServiceInstance self;

  public DatabaseDiscoveryClientImpl(
      RegistryService registryService,
      ApolloRegistryDiscoveryProperties discoveryProperties,
      ServiceInstance self) {
    this.registryService = registryService;
    this.discoveryProperties = discoveryProperties;
    this.self = self;
  }

  /**
   * find by {@link ApolloRegistryClientProperties#getServiceName()}
   */
  @Override
  public List<ServiceInstance> getInstances(String serviceName) {
    final List<Registry> filterByLabel;
    {
      List<Registry> all = this.registryService.findByServiceName(serviceName);
      if (this.discoveryProperties.isFilterByLabel()) {
        filterByLabel = filterByLabel(all, this.self.getLabel());
      } else {
        // get all
        filterByLabel = all;
      }
    }
    LocalDateTime healthCheckTime = this.registryService.getTimeBeforeSeconds(
        this.discoveryProperties.getHealthCheckInterval()
    );
    final List<Registry> filterByHealthCheck = filterByHealthCheck(filterByLabel, healthCheckTime, serviceName);

    // convert
    List<ServiceInstance> registrationList = new ArrayList<>(filterByHealthCheck.size());
    for (Registry registry : filterByHealthCheck) {
      ApolloRegistryClientProperties registration = convert(registry);
      registrationList.add(registration);
    }
    return registrationList;
  }

  static ApolloRegistryClientProperties convert(Registry registry) {
    ApolloRegistryClientProperties registration = new ApolloRegistryClientProperties();
    registration.setServiceName(registry.getServiceName());
    registration.setUri(registry.getUri());
    registration.setLabel(registry.getLabel());
    return registration;
  }

  static List<Registry> filterByLabel(List<Registry> list, String label) {
    if (list.isEmpty()) {
      return Collections.emptyList();
    }
    List<Registry> listAfterFilter = new ArrayList<>(8);
    for (Registry registry : list) {
      if (Objects.equals(label, registry.getLabel())) {
        listAfterFilter.add(registry);
      }
    }
    return listAfterFilter;
  }

  static List<Registry> filterByHealthCheck(
      List<Registry> list,
      LocalDateTime healthCheckTime,
      String serviceName
  ) {
    if (list.isEmpty()) {
      return Collections.emptyList();
    }
    List<Registry> listAfterFilter = new ArrayList<>(8);
    for (Registry registry : list) {
      LocalDateTime lastModifiedTime = registry.getDataChangeLastModifiedTime();
      if (lastModifiedTime.isAfter(healthCheckTime)) {
        listAfterFilter.add(registry);
      }
    }

    if (listAfterFilter.isEmpty()) {
      log.error(
          "there is no healthy instance of '{}'. And there are {} unhealthy instances",
          serviceName,
          list.size()
      );
    }

    return listAfterFilter;
  }

}
