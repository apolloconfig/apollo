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
import com.ctrip.framework.apollo.biz.service.RegistryService;

public class DatabaseServiceRegistryImpl implements DatabaseServiceRegistry {

  private final RegistryService registryService;

  public DatabaseServiceRegistryImpl(
      RegistryService registryService) {
    this.registryService = registryService;
  }

  public void register(ServiceInstance instance) {
    Registry registry = new Registry();
    registry.setServiceName(instance.getServiceName());
    registry.setUri(instance.getUri().toString());
    registry.setLabel(instance.getLabel());
    this.registryService.saveIfNotExistByServiceNameAndUri(registry);
  }

  public void deregister(ServiceInstance instance) {
    Registry registry = new Registry();
    registry.setServiceName(instance.getServiceName());
    registry.setUri(instance.getUri().toString());
    registry.setLabel(instance.getLabel());
    this.registryService.delete(registry);
  }
}
