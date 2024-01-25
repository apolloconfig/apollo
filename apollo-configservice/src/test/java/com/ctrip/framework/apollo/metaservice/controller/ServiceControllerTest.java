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
package com.ctrip.framework.apollo.metaservice.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.metaservice.service.DiscoveryService;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceControllerTest {

  @Mock
  private DiscoveryService discoveryService;

  @Mock
  private List<ServiceDTO> someServices;

  private ServiceController serviceController;

  @Before
  public void setUp() throws Exception {
    serviceController = new ServiceController(discoveryService);
  }

  @Test
  public void testGetMetaService() {
    assertTrue(serviceController.getMetaService().isEmpty());
  }

  @Test
  public void testGetConfigService() {
    String someAppId = "someAppId";
    String someClientIp = "someClientIp";

    when(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE))
        .thenReturn(someServices);

    assertEquals(someServices, serviceController.getConfigService(someAppId, someClientIp));
  }

  @Test
  public void testGetAdminService() {
    when(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_ADMINSERVICE))
        .thenReturn(someServices);

    assertEquals(someServices, serviceController.getAdminService());

  }
}