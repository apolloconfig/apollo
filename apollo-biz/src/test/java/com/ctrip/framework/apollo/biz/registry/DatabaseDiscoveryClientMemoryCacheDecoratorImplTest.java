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

class DatabaseDiscoveryClientMemoryCacheDecoratorImplTest {

  @Test
  void init() {
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(client);
    decorator.init();
  }

  @Test
  void updateCacheTask_empty() {
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(client);
    decorator.updateCacheTask();

    Mockito.verify(client, Mockito.never()).getInstances(Mockito.any());
  }

  @Test
  void updateCacheTask_exception() {
    final String serviceName = "a-service";
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances(serviceName))
        .thenReturn(
            Arrays.asList(
                newServiceInstance(serviceName, "http://10.240.34.56:8080/", "beijing"),
                newServiceInstance(serviceName, "http://10.240.34.56:8081/", "beijing"),
                newServiceInstance(serviceName, "http://10.240.34.56:8082/", "beijing")
            )
        );
    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(client);
    List<ServiceInstance> list = decorator.getInstances(serviceName);
    assertEquals(3, list.size());

    // if database error
    Mockito.when(client.getInstances(serviceName))
            .thenThrow(OutOfMemoryError.class);
    assertThrows(OutOfMemoryError.class, () -> decorator.readFromDatabase(serviceName));

    // task won't be interrupted by Throwable
    decorator.updateCacheTask();

    Mockito.verify(client, Mockito.times(3)).getInstances(serviceName);
  }

  @Test
  void getInstances_from_cache() {
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances("a-service"))
        .thenReturn(
            Arrays.asList(
                newServiceInstance("a-service", "http://10.240.34.56:8080/", "beijing"),
                newServiceInstance("a-service", "http://10.240.34.56:8081/", "beijing")
            )
        );
    Mockito.when(client.getInstances("b-service"))
        .thenReturn(
            Arrays.asList(
                newServiceInstance("b-service", "http://10.240.56.78:8080/", "shanghai"),
                newServiceInstance("b-service", "http://10.240.56.78:8081/", "shanghai"),
                newServiceInstance("b-service", "http://10.240.56.78:8082/", "shanghai")
            )
        );

    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(client);
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());

    // only invoke 1 times because always read from cache
    Mockito.verify(client, Mockito.times(1)).getInstances("a-service");
    Mockito.verify(client, Mockito.times(1)).getInstances("b-service");
  }

  @Test
  void getInstances_from_cache_when_database_updated() {
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances("a-service"))
        .thenReturn(
            Arrays.asList(
                newServiceInstance("a-service", "http://10.240.34.56:8080/", "beijing"),
                newServiceInstance("a-service", "http://10.240.34.56:8081/", "beijing")
            )
        );
    Mockito.when(client.getInstances("b-service"))
        .thenReturn(
            Arrays.asList(
                newServiceInstance("b-service", "http://10.240.56.78:8080/", "shanghai"),
                newServiceInstance("b-service", "http://10.240.56.78:8081/", "shanghai"),
                newServiceInstance("b-service", "http://10.240.56.78:8082/", "shanghai")
            )
        );

    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(client);
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());

    // only invoke 1 times because always read from cache
    Mockito.verify(client, Mockito.times(1)).getInstances("a-service");
    Mockito.verify(client, Mockito.times(1)).getInstances("b-service");

    // instances in database are changed
    Mockito.when(client.getInstances("b-service"))
        .thenReturn(
            Collections.singletonList(
                newServiceInstance("b-service", "http://10.240.56.78:8080/", "shanghai")
            )
        );

    // read again
    assertEquals(2, decorator.getInstances("a-service").size());
    // cache doesn't update yet, so we still get 3 instances
    assertEquals(3, decorator.getInstances("b-service").size());

    // only invoke 1 times because always read from cache
    Mockito.verify(client, Mockito.times(1)).getInstances("a-service");
    Mockito.verify(client, Mockito.times(1)).getInstances("b-service");

    decorator.updateCacheTask();

    // read again
    assertEquals(2, decorator.getInstances("a-service").size());
    // cache updated already, so we still get 1 instances
    assertEquals(1, decorator.getInstances("b-service").size());

    // invoke 2 times because always read from database again by task
    Mockito.verify(client, Mockito.times(2)).getInstances("a-service");
    Mockito.verify(client, Mockito.times(2)).getInstances("b-service");
  }


  @Test
  void getInstances_from_cache_when_database_crash() {
    DatabaseDiscoveryClient client = Mockito.mock(DatabaseDiscoveryClient.class);
    Mockito.when(client.getInstances("a-service"))
        .thenReturn(
            Arrays.asList(
                newServiceInstance("a-service", "http://10.240.34.56:8080/", "beijing"),
                newServiceInstance("a-service", "http://10.240.34.56:8081/", "beijing")
            )
        );
    Mockito.when(client.getInstances("b-service"))
        .thenReturn(
            Arrays.asList(
                newServiceInstance("b-service", "http://10.240.56.78:8080/", "shanghai"),
                newServiceInstance("b-service", "http://10.240.56.78:8081/", "shanghai"),
                newServiceInstance("b-service", "http://10.240.56.78:8082/", "shanghai")
            )
        );

    DatabaseDiscoveryClientMemoryCacheDecoratorImpl decorator
        = new DatabaseDiscoveryClientMemoryCacheDecoratorImpl(client);
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());

    // only invoke 1 times because always read from cache
    Mockito.verify(client, Mockito.times(1)).getInstances("a-service");
    Mockito.verify(client, Mockito.times(1)).getInstances("b-service");

    // database crash
    Mockito.when(client.getInstances(Mockito.any()))
        .thenThrow(OutOfMemoryError.class);
    assertThrows(OutOfMemoryError.class, () -> decorator.readFromDatabase("a-service"));
    assertThrows(OutOfMemoryError.class, () -> decorator.readFromDatabase("b-service"));
    
    // read again
    assertEquals(2, decorator.getInstances("a-service").size());
    assertEquals(3, decorator.getInstances("b-service").size());

    Mockito.verify(client, Mockito.times(2)).getInstances("a-service");
    Mockito.verify(client, Mockito.times(2)).getInstances("b-service");
  }
}