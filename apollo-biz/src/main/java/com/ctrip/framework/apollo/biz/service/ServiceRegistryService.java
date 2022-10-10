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
package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.repository.ServiceRegistryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ServiceRegistryService {

  private final ServiceRegistryRepository repository;

  public ServiceRegistryService(ServiceRegistryRepository repository) {
    this.repository = repository;
  }

  public ServiceRegistry saveIfNotExistByServiceNameAndUri(ServiceRegistry serviceRegistry) {
    ServiceRegistry serviceRegistrySaved = this.repository.findByServiceNameAndUri(serviceRegistry.getServiceName(), serviceRegistry.getUri());
    if (null == serviceRegistrySaved) {
      serviceRegistrySaved = serviceRegistry;
    } else {
      // update
      serviceRegistrySaved.setCluster(serviceRegistry.getCluster());
      serviceRegistrySaved.setDataChangeLastModifiedTime(LocalDateTime.now());
    }
    return this.repository.save(serviceRegistrySaved);
  }

  public void delete(ServiceRegistry serviceRegistry) {
    this.repository.deleteByServiceNameAndUri(
        serviceRegistry.getServiceName(), serviceRegistry.getUri()
    );
  }

  public List<ServiceRegistry> findByServiceName(String serviceName) {
    return this.repository.findByServiceName(serviceName);
  }

  public LocalDateTime getTimeBeforeSeconds(long seconds) {
    return this.repository.currentTimestamp().minusSeconds(seconds);
  }

  public List<ServiceRegistry> deleteTimeBefore(Duration duration) {
    LocalDateTime time = this.repository.currentTimestamp().minus(duration);
    return this.repository.deleteByDataChangeLastModifiedTimeLessThan(time);
  }
}
