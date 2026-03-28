/*
 * Copyright 2025 Apollo Authors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Verifies the Boot 4 Nacos profile now uses the generic Spring Cloud discovery path.
 */
@RunWith(MockitoJUnitRunner.class)
public class NacosDiscoveryServiceTest {

  @Mock
  private DiscoveryClient discoveryClient;

  private SpringCloudInnerDiscoveryService nacosDiscoveryService;

  private String someServiceId;

  @Before
  public void setUp() {
    nacosDiscoveryService = new SpringCloudInnerDiscoveryService(discoveryClient);
    someServiceId = "someServiceId";
  }

  @Test
  public void testGetServiceInstancesWithEmptyInstances() {
    when(discoveryClient.getInstances(someServiceId)).thenReturn(null);

    assertTrue(nacosDiscoveryService.getServiceInstances(someServiceId).isEmpty());
  }

  @Test
  public void testGetServiceInstances() {
    String someIp = "1.2.3.4";
    int somePort = 8080;
    String someInstanceId = "someInstanceId";
    ServiceInstance someServiceInstance = mockServiceInstance(someInstanceId, someIp, somePort);

    when(discoveryClient.getInstances(someServiceId))
        .thenReturn(Lists.newArrayList(someServiceInstance));

    List<ServiceDTO> serviceDTOList = nacosDiscoveryService.getServiceInstances(someServiceId);
    ServiceDTO serviceDTO = serviceDTOList.get(0);
    assertEquals(1, serviceDTOList.size());
    assertEquals(someServiceId, serviceDTO.getAppName());
    assertEquals(someInstanceId, serviceDTO.getInstanceId());
    assertEquals("http://1.2.3.4:8080/", serviceDTO.getHomepageUrl());
  }

  private ServiceInstance mockServiceInstance(String instanceId, String ip, int port) {
    ServiceInstance serviceInstance = mock(ServiceInstance.class);
    when(serviceInstance.getInstanceId()).thenReturn(instanceId);
    when(serviceInstance.getHost()).thenReturn(ip);
    when(serviceInstance.getPort()).thenReturn(port);

    return serviceInstance;
  }
}
