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

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/3/1
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsulDiscoveryServiceTest {

    @Mock
    private ConsulDiscoveryClient consulDiscoveryClient;

    private SpringCloudInnerDiscoveryService consulDiscoveryService;

    private String someServiceId;

    @Before
    public void setUp() throws Exception {
        consulDiscoveryService = new SpringCloudInnerDiscoveryService(consulDiscoveryClient);
        someServiceId = "someServiceId";
    }

    @Test
    public void testGetServiceInstancesWithNullInstances() {
        when(consulDiscoveryClient.getInstances(someServiceId)).thenReturn(null);
        assertTrue(consulDiscoveryService.getServiceInstances(someServiceId).isEmpty());
    }


    @Test
    public void testGetServiceInstances() {
        String someIp = "1.2.3.4";
        int somePort = 8080;
        String someInstanceId = "someInstanceId";
        ServiceInstance someServiceInstance = mockServiceInstance(someInstanceId, someIp, somePort);

        when(consulDiscoveryClient.getInstances(someServiceId)).thenReturn(
                Lists.newArrayList(someServiceInstance));

        List<ServiceDTO> serviceDTOList = consulDiscoveryService.getServiceInstances(someServiceId);
        ServiceDTO serviceDTO = serviceDTOList.get(0);
        assertEquals(1, serviceDTOList.size());
        assertEquals(someServiceId, serviceDTO.getAppName());
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
