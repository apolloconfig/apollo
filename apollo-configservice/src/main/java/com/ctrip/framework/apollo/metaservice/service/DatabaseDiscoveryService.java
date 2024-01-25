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

import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryClient;
import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * use database as a registry
 */
@Service
@Profile("database-discovery")
public class DatabaseDiscoveryService implements DiscoveryService {

  private final DatabaseDiscoveryClient discoveryClient;

  public DatabaseDiscoveryService(
      DatabaseDiscoveryClient discoveryClient) {
    this.discoveryClient = discoveryClient;
  }

  @Override
  public List<ServiceDTO> getServiceInstances(String serviceId) {
    List<ServiceInstance> serviceInstanceList = this.discoveryClient.getInstances(serviceId);
    return convert(serviceInstanceList);
  }

  static List<ServiceDTO> convert(List<ServiceInstance> list) {
    List<ServiceDTO> serviceDTOList = new ArrayList<>(list.size());
    for (ServiceInstance serviceInstance : list) {
      ServiceDTO serviceDTO = convert(serviceInstance);
      serviceDTOList.add(serviceDTO);
    }
    return serviceDTOList;
  }

  static ServiceDTO convert(ServiceInstance serviceInstance) {
    ServiceDTO serviceDTO = new ServiceDTO();
    serviceDTO.setAppName(serviceInstance.getServiceName());
    String homePageUrl = serviceInstance.getUri().toString();
    serviceDTO.setInstanceId(homePageUrl);
    serviceDTO.setHomepageUrl(homePageUrl);
    return serviceDTO;
  }
}
