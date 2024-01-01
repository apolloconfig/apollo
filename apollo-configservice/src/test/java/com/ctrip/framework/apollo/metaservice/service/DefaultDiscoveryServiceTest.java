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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDiscoveryServiceTest {

  @Mock
  private EurekaClient eurekaClient;

  @Mock
  private Application someApplication;

  private DefaultDiscoveryService defaultDiscoveryService;

  private String someServiceId;

  @Before
  public void setUp() throws Exception {
    defaultDiscoveryService = new DefaultDiscoveryService(eurekaClient);

    someServiceId = "someServiceId";
  }

  @Test
  public void testGetServiceInstancesWithNullInstances() {
    when(eurekaClient.getApplication(someServiceId)).thenReturn(null);

    assertTrue(defaultDiscoveryService.getServiceInstances(someServiceId).isEmpty());
  }

  @Test
  public void testGetServiceInstancesWithEmptyInstances() {
    when(eurekaClient.getApplication(someServiceId)).thenReturn(someApplication);
    when(someApplication.getInstances()).thenReturn(new ArrayList<>());

    assertTrue(defaultDiscoveryService.getServiceInstances(someServiceId).isEmpty());
  }

  @Test
  public void testGetServiceInstances() throws URISyntaxException {
    String someUri = "http://1.2.3.4:8080/some-path/";
    String someInstanceId = "someInstanceId";
    InstanceInfo someServiceInstance = mockServiceInstance(someServiceId, someInstanceId,
        someUri);

    String anotherUri = "http://2.3.4.5:9090/anotherPath";
    String anotherInstanceId = "anotherInstanceId";
    InstanceInfo anotherServiceInstance = mockServiceInstance(someServiceId, anotherInstanceId,
        anotherUri);

    when(eurekaClient.getApplication(someServiceId)).thenReturn(someApplication);
    when(someApplication.getInstances())
        .thenReturn(Lists.newArrayList(someServiceInstance, anotherServiceInstance));

    List<ServiceDTO> serviceDTOList = defaultDiscoveryService.getServiceInstances(someServiceId);

    assertEquals(2, serviceDTOList.size());
    check(someServiceInstance, serviceDTOList.get(0));
    check(anotherServiceInstance, serviceDTOList.get(1));
  }

  private void check(InstanceInfo serviceInstance, ServiceDTO serviceDTO) {
    assertEquals(serviceInstance.getAppName(), serviceDTO.getAppName());
    assertEquals(serviceInstance.getInstanceId(), serviceDTO.getInstanceId());
    assertEquals(serviceInstance.getHomePageUrl(), serviceDTO.getHomepageUrl());
  }

  private InstanceInfo mockServiceInstance(String serviceId, String instanceId, String homePageUrl) {
    InstanceInfo serviceInstance = mock(InstanceInfo.class);
    when(serviceInstance.getAppName()).thenReturn(serviceId);
    when(serviceInstance.getInstanceId()).thenReturn(instanceId);
    when(serviceInstance.getHomePageUrl()).thenReturn(homePageUrl);

    return serviceInstance;
  }
}