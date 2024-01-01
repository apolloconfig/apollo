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
package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.repository.ServiceRegistryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ServiceRegistryService {

  private final ServiceRegistryRepository repository;

  public ServiceRegistryService(ServiceRegistryRepository repository) {
    this.repository = repository;
  }

  public ServiceRegistry saveIfNotExistByServiceNameAndUri(ServiceRegistry serviceRegistry) {
    ServiceRegistry serviceRegistrySaved = this.repository.findByServiceNameAndUri(serviceRegistry.getServiceName(), serviceRegistry.getUri());
    final LocalDateTime now = LocalDateTime.now();
    if (null == serviceRegistrySaved) {
      serviceRegistrySaved = serviceRegistry;
      serviceRegistrySaved.setDataChangeCreatedTime(now);
      serviceRegistrySaved.setDataChangeLastModifiedTime(now);
    } else {
      // update
      serviceRegistrySaved.setCluster(serviceRegistry.getCluster());
      serviceRegistrySaved.setMetadata(serviceRegistry.getMetadata());
      serviceRegistrySaved.setDataChangeLastModifiedTime(now);
    }
    return this.repository.save(serviceRegistrySaved);
  }

  @Transactional
  public void delete(ServiceRegistry serviceRegistry) {
    this.repository.deleteByServiceNameAndUri(
        serviceRegistry.getServiceName(), serviceRegistry.getUri()
    );
  }

  public List<ServiceRegistry> findByServiceNameDataChangeLastModifiedTimeGreaterThan(
      String serviceName,
      LocalDateTime localDateTime
  ) {
    return this.repository.findByServiceNameAndDataChangeLastModifiedTimeGreaterThan(serviceName, localDateTime);
  }

  @Transactional
  public List<ServiceRegistry> deleteTimeBefore(Duration duration) {
    LocalDateTime time = LocalDateTime.now().minus(duration);
    return this.repository.deleteByDataChangeLastModifiedTimeLessThan(time);
  }
}
