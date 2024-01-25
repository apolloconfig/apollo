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
package com.ctrip.framework.apollo.metaservice.service;

import com.ctrip.framework.apollo.common.condition.ConditionalOnMissingProfile;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Default discovery service for Eureka
 */
@Service
@ConditionalOnMissingProfile({
    "kubernetes",
    "nacos-discovery",
    "consul-discovery",
    "zookeeper-discovery",
    "custom-defined-discovery",
    "database-discovery",
})
public class DefaultDiscoveryService implements DiscoveryService {

  private final EurekaClient eurekaClient;

  public DefaultDiscoveryService(final EurekaClient eurekaClient) {
    this.eurekaClient = eurekaClient;
  }

  @Override
  public List<ServiceDTO> getServiceInstances(String serviceId) {
    Application application = eurekaClient.getApplication(serviceId);
    if (application == null || CollectionUtils.isEmpty(application.getInstances())) {
      Tracer.logEvent("Apollo.Discovery.NotFound", serviceId);
      return Collections.emptyList();
    }
    return application.getInstances().stream().map(instanceInfoToServiceDTOFunc)
        .collect(Collectors.toList());
  }

  private static final Function<InstanceInfo, ServiceDTO> instanceInfoToServiceDTOFunc = instance -> {
    ServiceDTO service = new ServiceDTO();
    service.setAppName(instance.getAppName());
    service.setInstanceId(instance.getInstanceId());
    service.setHomepageUrl(instance.getHomePageUrl());
    return service;
  };
}
