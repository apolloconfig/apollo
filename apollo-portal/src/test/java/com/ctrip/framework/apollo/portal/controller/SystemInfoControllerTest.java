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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.entity.vo.EnvironmentInfo;
import com.ctrip.framework.apollo.portal.entity.vo.SystemInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class SystemInfoControllerTest {

  @Mock
  private PortalSettings portalSettings;

  @Mock
  private RestTemplateFactory restTemplateFactory;

  @Mock
  private PortalMetaDomainService portalMetaDomainService;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private SystemInfoController systemInfoController;

  @Before
  public void setUp() {
    when(restTemplateFactory.getObject()).thenReturn(restTemplate);
    ReflectionTestUtils.invokeMethod(systemInfoController, "init");
  }

  @Test
  public void shouldBuildSystemInfoWithEnvironmentDetails() {
    when(portalSettings.getAllEnvs()).thenReturn(Collections.singletonList(Env.DEV));
    when(portalSettings.isEnvActive(Env.DEV)).thenReturn(true);
    when(portalMetaDomainService.getMetaServerAddress(Env.DEV)).thenReturn("http://meta");
    when(portalMetaDomainService.getDomain(Env.DEV)).thenReturn("http://meta");

    ServiceDTO configService = service("config-1", "http://config-service");
    ServiceDTO adminService = service("admin-1", "http://admin-service");
    when(restTemplate.getForObject("http://meta/services/config", ServiceDTO[].class))
        .thenReturn(new ServiceDTO[] {configService});
    when(restTemplate.getForObject("http://meta/services/admin", ServiceDTO[].class))
        .thenReturn(new ServiceDTO[] {adminService});

    SystemInfo systemInfo = systemInfoController.getSystemInfo();

    assertEquals(1, systemInfo.getEnvironments().size());
    EnvironmentInfo envInfo = systemInfo.getEnvironments().get(0);
    assertEquals(Env.DEV, envInfo.getEnv());
    assertTrue(envInfo.isActive());
    assertEquals("http://meta", envInfo.getMetaServerAddress());
    assertEquals(1, envInfo.getConfigServices().length);
    assertEquals(1, envInfo.getAdminServices().length);
  }

  @Test
  public void shouldRecordErrorMessageWhenLoadingServicesFails() {
    when(portalSettings.getAllEnvs()).thenReturn(Collections.singletonList(Env.DEV));
    when(portalSettings.isEnvActive(Env.DEV)).thenReturn(false);
    when(portalMetaDomainService.getMetaServerAddress(Env.DEV)).thenReturn("http://meta");
    when(portalMetaDomainService.getDomain(Env.DEV)).thenReturn("http://meta");

    when(restTemplate.getForObject("http://meta/services/config", ServiceDTO[].class))
        .thenThrow(new RuntimeException("boom"));

    SystemInfo systemInfo = systemInfoController.getSystemInfo();

    EnvironmentInfo envInfo = systemInfo.getEnvironments().get(0);
    assertNotNull(envInfo.getErrorMessage());
    assertTrue(envInfo.getErrorMessage().contains("failed"));
    assertTrue(envInfo.getErrorMessage().contains("boom"));
  }

  @Test
  public void shouldCheckHealthForMatchedInstance() {
    when(portalSettings.getAllEnvs()).thenReturn(Collections.singletonList(Env.DEV));
    when(portalSettings.isEnvActive(Env.DEV)).thenReturn(true);
    when(portalMetaDomainService.getMetaServerAddress(Env.DEV)).thenReturn("http://meta");
    when(portalMetaDomainService.getDomain(Env.DEV)).thenReturn("http://meta");

    ServiceDTO configService = service("config-1", "http://config-service");
    when(restTemplate.getForObject("http://meta/services/config", ServiceDTO[].class))
        .thenReturn(new ServiceDTO[] {configService});
    when(restTemplate.getForObject("http://meta/services/admin", ServiceDTO[].class))
        .thenReturn(new ServiceDTO[0]);

    Health expected = Health.up().withDetail("status", "UP").build();
    when(restTemplate.getForObject("http://config-service/health", Health.class)).thenReturn(expected);

    Health result = systemInfoController.checkHealth("config-1");

    assertSame(expected, result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowWhenInstanceIdDoesNotExist() {
    when(portalSettings.getAllEnvs()).thenReturn(Collections.singletonList(Env.DEV));
    when(portalSettings.isEnvActive(Env.DEV)).thenReturn(true);
    when(portalMetaDomainService.getMetaServerAddress(Env.DEV)).thenReturn("http://meta");
    when(portalMetaDomainService.getDomain(Env.DEV)).thenReturn("http://meta");
    when(restTemplate.getForObject("http://meta/services/config", ServiceDTO[].class))
        .thenReturn(new ServiceDTO[0]);
    when(restTemplate.getForObject("http://meta/services/admin", ServiceDTO[].class))
        .thenReturn(new ServiceDTO[0]);

    systemInfoController.checkHealth("missing-instance");
  }

  private ServiceDTO service(String instanceId, String homepageUrl) {
    ServiceDTO dto = new ServiceDTO();
    dto.setInstanceId(instanceId);
    dto.setHomepageUrl(homepageUrl);
    return dto;
  }
}
