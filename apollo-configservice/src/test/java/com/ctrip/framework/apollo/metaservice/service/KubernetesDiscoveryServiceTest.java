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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesDiscoveryServiceTest {

  private String configServiceConfigName = "apollo.config-service.url";
  private String adminServiceConfigName = "apollo.admin-service.url";

  @Mock
  private BizConfig bizConfig;

  private KubernetesDiscoveryService kubernetesDiscoveryService;

  @Before
  public void setUp() throws Exception {
    kubernetesDiscoveryService = new KubernetesDiscoveryService(bizConfig);
  }

  @Test
  public void testGetServiceInstancesWithInvalidServiceId() {
    String someInvalidServiceId = "someInvalidServiceId";

    assertTrue(kubernetesDiscoveryService.getServiceInstances(someInvalidServiceId).isEmpty());
  }

  @Test
  public void testGetServiceInstancesWithNullConfig() {
    when(bizConfig.getValue(configServiceConfigName)).thenReturn(null);

    assertTrue(
        kubernetesDiscoveryService.getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE)
            .isEmpty());

    verify(bizConfig, times(1)).getValue(configServiceConfigName);
  }

  @Test
  public void testGetConfigServiceInstances() {
    String someUrl = "http://some-host/some-path";
    when(bizConfig.getValue(configServiceConfigName)).thenReturn(someUrl);

    List<ServiceDTO> serviceDTOList = kubernetesDiscoveryService
        .getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE);

    assertEquals(1, serviceDTOList.size());
    ServiceDTO serviceDTO = serviceDTOList.get(0);

    assertEquals(ServiceNameConsts.APOLLO_CONFIGSERVICE, serviceDTO.getAppName());
    assertEquals(String.format("%s:%s", ServiceNameConsts.APOLLO_CONFIGSERVICE, someUrl),
        serviceDTO.getInstanceId());
    assertEquals(someUrl, serviceDTO.getHomepageUrl());
  }

  @Test
  public void testGetAdminServiceInstances() {
    String someUrl = "http://some-host/some-path";
    String anotherUrl = "http://another-host/another-path";
    when(bizConfig.getValue(adminServiceConfigName))
        .thenReturn(String.format("%s,%s", someUrl, anotherUrl));

    List<ServiceDTO> serviceDTOList = kubernetesDiscoveryService
        .getServiceInstances(ServiceNameConsts.APOLLO_ADMINSERVICE);

    assertEquals(2, serviceDTOList.size());
    ServiceDTO serviceDTO = serviceDTOList.get(0);

    assertEquals(ServiceNameConsts.APOLLO_ADMINSERVICE, serviceDTO.getAppName());
    assertEquals(String.format("%s:%s", ServiceNameConsts.APOLLO_ADMINSERVICE, someUrl),
        serviceDTO.getInstanceId());
    assertEquals(someUrl, serviceDTO.getHomepageUrl());

    ServiceDTO anotherServiceDTO = serviceDTOList.get(1);

    assertEquals(ServiceNameConsts.APOLLO_ADMINSERVICE, anotherServiceDTO.getAppName());
    assertEquals(String.format("%s:%s", ServiceNameConsts.APOLLO_ADMINSERVICE, anotherUrl),
        anotherServiceDTO.getInstanceId());
    assertEquals(anotherUrl, anotherServiceDTO.getHomepageUrl());

  }

}