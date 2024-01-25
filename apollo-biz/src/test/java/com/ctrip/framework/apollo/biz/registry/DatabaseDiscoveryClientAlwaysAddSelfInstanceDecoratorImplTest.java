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

import static com.ctrip.framework.apollo.biz.registry.ServiceInstanceFactory.newServiceInstance;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImplTest {

  @Test
  void getInstances_other_service_name() {
    final String otherServiceName = "other-service";
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances(otherServiceName))
        .thenReturn(
            Collections.singletonList(
                newServiceInstance(otherServiceName, "http://10.240.34.56:8081/", "beijing")
            )
        );

    final String selfServiceName = "self-service";
    ServiceInstance selfInstance = newServiceInstance(
        selfServiceName, "http://10.240.34.56:8081/", "beijing"
    );

    DatabaseDiscoveryClient decorator = new DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl(
        client, selfInstance
    );

    List<ServiceInstance> serviceInstances = decorator.getInstances(otherServiceName);
    assertEquals(1, serviceInstances.size());
    ServiceInstance otherServiceNameInstance = serviceInstances.get(0);
    assertEquals(otherServiceName, otherServiceNameInstance.getServiceName());

    Mockito.verify(client, Mockito.times(1))
        .getInstances(Mockito.eq(otherServiceName));

    Mockito.verify(client, Mockito.never())
        .getInstances(Mockito.eq(selfServiceName));
  }

  @Test
  void getInstances_contain_self() {
    final String otherServiceName = "other-service";
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances(otherServiceName))
        .thenReturn(
            Collections.singletonList(
                newServiceInstance(otherServiceName, "http://10.240.34.56:8081/", "beijing")
            )
        );

    final String selfServiceName = "self-service";
    ServiceInstance selfInstance = newServiceInstance(
        selfServiceName, "http://10.240.34.56:8081/", "beijing"
    );
    Mockito.when(client.getInstances(selfServiceName))
        .thenReturn(
            Arrays.asList(
                selfInstance,
                // same service name but different service instance
                newServiceInstance(selfServiceName, "http://10.240.34.56:8082/", "beijing"),
                newServiceInstance(selfServiceName, "http://10.240.34.56:8083/", "beijing")
            )
        );

    DatabaseDiscoveryClient decorator = new DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl(
        client, selfInstance
    );

    List<ServiceInstance> serviceInstances = decorator.getInstances(selfServiceName);
    assertEquals(3, serviceInstances.size());

    Mockito.verify(client, Mockito.times(1))
        .getInstances(Mockito.eq(selfServiceName));

    Mockito.verify(client, Mockito.never())
        .getInstances(Mockito.eq(otherServiceName));
  }

  /**
   * will add self
   */
  @Test
  void getInstances_same_service_name_without_self() {
    final String otherServiceName = "other-service";
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances(otherServiceName))
        .thenReturn(
            Collections.singletonList(
                newServiceInstance(otherServiceName, "http://10.240.34.56:8081/", "beijing")
            )
        );

    final String selfServiceName = "self-service";
    ServiceInstance selfInstance = newServiceInstance(
        selfServiceName, "http://10.240.34.56:8081/", "beijing"
    );
    Mockito.when(client.getInstances(selfServiceName))
        .thenReturn(
            Arrays.asList(
                // same service name but different service instance
                newServiceInstance(selfServiceName, "http://10.240.34.56:8082/", "beijing"),
                newServiceInstance(selfServiceName, "http://10.240.34.56:8083/", "beijing")
            )
        );

    DatabaseDiscoveryClient decorator = new DatabaseDiscoveryClientAlwaysAddSelfInstanceDecoratorImpl(
        client, selfInstance
    );

    List<ServiceInstance> serviceInstances = decorator.getInstances(selfServiceName);
    // because mocked data don't contain self instance
    // after add self instance, there are 3 instances now
    assertEquals(3, serviceInstances.size());

    Mockito.verify(client, Mockito.times(1))
        .getInstances(Mockito.eq(selfServiceName));

    Mockito.verify(client, Mockito.never())
        .getInstances(Mockito.eq(otherServiceName));
  }
}