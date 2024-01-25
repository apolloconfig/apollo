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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.registry.DatabaseDiscoveryWithoutDecoratorIntegrationTest.ApolloServiceDiscoveryWithoutDecoratorAutoConfiguration;
import com.ctrip.framework.apollo.biz.registry.configuration.ApolloServiceDiscoveryAutoConfiguration;
import com.ctrip.framework.apollo.biz.registry.configuration.ApolloServiceRegistryAutoConfiguration;
import com.ctrip.framework.apollo.biz.registry.configuration.support.ApolloServiceDiscoveryProperties;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * test when {@link DatabaseDiscoveryClient} doesn't warp by decorator.
 */
@TestPropertySource(
    properties = {
        "apollo.service.registry.enabled=true",
        "apollo.service.registry.cluster=default",
        "apollo.service.discovery.enabled=true",
        "spring.application.name=for-test-service",
        "server.port=10000",
        // close decorator
        "ApolloServiceDiscoveryWithoutDecoratorAutoConfiguration.enabled=true",
    }
)
@ContextConfiguration(classes = {
    ApolloServiceRegistryAutoConfiguration.class,
    // notice that the order of classes is import
    // @AutoConfigureBefore(ApolloServiceDiscoveryAutoConfiguration.class) won't work when run test
    ApolloServiceDiscoveryWithoutDecoratorAutoConfiguration.class,
    ApolloServiceDiscoveryAutoConfiguration.class,
})
public class DatabaseDiscoveryWithoutDecoratorIntegrationTest extends AbstractIntegrationTest {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private DatabaseServiceRegistry serviceRegistry;

  @Autowired
  private DatabaseDiscoveryClient discoveryClient;

  /**
   * discover one after register, and delete it
   */
  @Test
  public void registerThenDiscoveryThenDelete() {
    // register it
    String serviceName = "a-service";
    String uri = "http://192.168.1.20:8080/";
    String cluster = "default";
    ServiceInstance instance = newServiceInstance(
        serviceName, uri, cluster
    );
    this.serviceRegistry.register(instance);

    // find it
    List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
    ServiceInstance actual = serviceInstances.get(0);
    assertEquals(serviceName, actual.getServiceName());
    assertEquals(uri, actual.getUri().toString());
    assertEquals(cluster, actual.getCluster());
    assertEquals(0, actual.getMetadata().size());

    // delete it
    this.serviceRegistry.deregister(instance);
    // find none
    assertEquals(0, this.discoveryClient.getInstances(serviceName).size());
  }

  /**
   * diff cluster so cannot be discover
   */
  @Test
  public void registerThenDiscoveryNone() {
    // register it
    String serviceName = "b-service";
    ServiceInstance instance = newServiceInstance(
        serviceName, "http://192.168.1.20:8080/", "cannot-be-discovery"
    );
    this.serviceRegistry.register(instance);

    // find none
    List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(serviceName);
    assertEquals(0, serviceInstances.size());
  }

  @Test
  public void registerTwice() {
    String serviceName = "c-service";
    ServiceInstance instance = newServiceInstance(
        serviceName, "http://192.168.1.20:8080/", "default"
    );

    // register it
    this.serviceRegistry.register(instance);
    // register again
    this.serviceRegistry.register(instance);

    // only discover one
    List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(serviceName);
    assertEquals(1, serviceInstances.size());
  }

  @Test
  public void registerTwoInstancesThenDeleteOne() {
    final String serviceName = "d-service";
    final String cluster = "default";

    this.serviceRegistry.register(
        newServiceInstance(
            serviceName, "http://192.168.1.20:8080/", cluster
        )
    );
    this.serviceRegistry.register(
        newServiceInstance(
            serviceName, "http://192.168.1.20:10000/", cluster
        )
    );

    final List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(serviceName);
    assertEquals(2, serviceInstances.size());

    for (ServiceInstance serviceInstance : serviceInstances) {
      assertEquals(serviceName, serviceInstance.getServiceName());
      assertEquals(cluster, serviceInstance.getCluster());
      assertEquals(0, serviceInstance.getMetadata().size());
    }

    // delete one
    this.serviceRegistry.deregister(
        newServiceInstance(
            serviceName, "http://192.168.1.20:10000/", cluster
        )
    );

    assertEquals(1, this.discoveryClient.getInstances(serviceName).size());
    assertEquals("http://192.168.1.20:8080/",
        this.discoveryClient.getInstances(serviceName).get(0).getUri().toString());
  }

  /**
   * only use in {@link DatabaseDiscoveryWithoutDecoratorIntegrationTest}
   */
  @Configuration
  @ConditionalOnProperty(prefix = "ApolloServiceDiscoveryWithoutDecoratorAutoConfiguration", value = "enabled")
  @ConditionalOnBean(ApolloServiceDiscoveryAutoConfiguration.class)
  @AutoConfigureBefore(ApolloServiceDiscoveryAutoConfiguration.class)
  @EnableConfigurationProperties({
      ApolloServiceDiscoveryProperties.class,
  })
  static class ApolloServiceDiscoveryWithoutDecoratorAutoConfiguration {
    @Bean
    public DatabaseDiscoveryClient databaseDiscoveryClient(
        ApolloServiceDiscoveryProperties discoveryProperties,
        ServiceInstance selfServiceInstance,
        ServiceRegistryService serviceRegistryService
    ) {
      return new DatabaseDiscoveryClientImpl(
          serviceRegistryService, discoveryProperties, selfServiceInstance.getCluster()
      );
    }
  }
}
