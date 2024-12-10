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
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;

public class DatabaseServiceRegistryImpl implements DatabaseServiceRegistry {

  private final ServiceRegistryService serviceRegistryService;

  public DatabaseServiceRegistryImpl(
      ServiceRegistryService serviceRegistryService) {
    this.serviceRegistryService = serviceRegistryService;
  }

  static ServiceRegistry convert(ServiceInstance instance) {
    ServiceRegistry serviceRegistry = new ServiceRegistry();
    serviceRegistry.setServiceName(instance.getServiceName());
    serviceRegistry.setUri(instance.getUri().toString());
    serviceRegistry.setCluster(instance.getCluster());
    serviceRegistry.setMetadata(instance.getMetadata());
    return serviceRegistry;
  }

  @Override
  public void register(ServiceInstance instance) {
    ServiceRegistry serviceRegistry = convert(instance);
    this.serviceRegistryService.saveIfNotExistByServiceNameAndUri(serviceRegistry);
  }

  @Override
  public void deregister(ServiceInstance instance) {
    ServiceRegistry serviceRegistry = convert(instance);
    this.serviceRegistryService.delete(serviceRegistry);
  }
}
