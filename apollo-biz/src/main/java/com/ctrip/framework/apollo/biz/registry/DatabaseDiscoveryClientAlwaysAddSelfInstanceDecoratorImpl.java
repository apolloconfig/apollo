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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * decorator pattern
 * <p>
 * when database crash, even cannot register self instance to database,
 * <p>
 * this decorator will ensure return's result contains self instance.
 */
public class DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl
  implements DatabaseDiscoveryClient {

  private final DatabaseDiscoveryClient delegate;

  private final ServiceInstance selfInstance;

  public DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl(
      DatabaseDiscoveryClient delegate,
      ServiceInstance selfInstance
  ) {
    this.delegate = delegate;
    this.selfInstance = selfInstance;
  }

  static boolean containSelf(List<ServiceInstance> serviceInstances, ServiceInstance selfInstance) {
    final String selfServiceName = selfInstance.getServiceName();
    final URI selfUri = selfInstance.getUri();
    final String cluster = selfInstance.getCluster();
    for (ServiceInstance serviceInstance : serviceInstances) {
      if (Objects.equals(selfServiceName, serviceInstance.getServiceName())) {
        if (Objects.equals(selfUri, serviceInstance.getUri())) {
          if (Objects.equals(cluster, serviceInstance.getCluster())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * if the serviceName is same with self, always return self's instance
   * @return never be empty list when serviceName is same with self
   */
  @Override
  public List<ServiceInstance> getInstances(String serviceName) {
    if (Objects.equals(serviceName, this.selfInstance.getServiceName())) {
      List<ServiceInstance> serviceInstances = this.delegate.getInstances(serviceName);
      if (containSelf(serviceInstances, this.selfInstance)) {
        // contains self instance already
        return serviceInstances;
      }

      // add self instance to result
      List<ServiceInstance> result = new ArrayList<>(serviceInstances.size() + 1);
      result.add(this.selfInstance);
      result.addAll(serviceInstances);
      return result;
    } else {
      return this.delegate.getInstances(serviceName);
    }
  }
}
