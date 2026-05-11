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
package com.ctrip.framework.apollo.openapi.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenMissEnvDTO;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class ServerAppOpenApiServiceTest {

  @Mock
  private PortalSettings portalSettings;
  @Mock
  private ClusterService clusterService;
  @Mock
  private AppService appService;
  @Mock
  private ApplicationEventPublisher publisher;
  @Mock
  private RoleInitializationService roleInitializationService;

  private ServerAppOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerAppOpenApiService(portalSettings, clusterService, appService, publisher,
        roleInitializationService);
  }

  @Test
  void findMissEnvsShouldReturnLatestSpecDtosForMissingAndFailedEnvs() {
    String appId = "someApp";
    when(portalSettings.getActiveEnvs()).thenReturn(Lists.newArrayList(Env.DEV, Env.FAT, Env.PRO));
    when(appService.load(Env.DEV, appId)).thenReturn(new AppDTO());
    when(appService.load(Env.FAT, appId)).thenThrow(HttpClientErrorException.create(
        HttpStatus.NOT_FOUND, "not found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));
    when(appService.load(Env.PRO, appId)).thenThrow(new IllegalStateException("boom"));

    List<OpenMissEnvDTO> result = service.findMissEnvs(appId);

    assertEquals(2, result.size());
    assertEquals(HttpStatus.OK.value(), result.get(0).getCode());
    assertEquals("FAT", result.get(0).getMessage());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.get(1).getCode());
    assertEquals("load appId:someApp from env PRO error.", result.get(1).getMessage());
    assertFalse(result.get(1).getMessage().contains("boom"));
  }

  @Test
  void getEnvClusterInfoShouldWrapNavTreeDataWithStatus() {
    String appId = "someApp";
    ClusterDTO cluster = new ClusterDTO();
    cluster.setAppId(appId);
    cluster.setName("default");
    EnvClusterInfo navNode = new EnvClusterInfo(Env.DEV);
    navNode.setClusters(Lists.newArrayList(cluster));

    when(portalSettings.getActiveEnvs()).thenReturn(Lists.newArrayList(Env.DEV));
    when(appService.createEnvNavNode(Env.DEV, appId)).thenReturn(navNode);

    List<OpenEnvClusterInfo> result = service.getEnvClusterInfo(appId);

    assertEquals(1, result.size());
    assertEquals(HttpStatus.OK.value(), result.get(0).getCode());
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.get(0).getMessage());
    assertEquals("DEV", result.get(0).getEnv());
    assertEquals("default", result.get(0).getClusters().get(0).getName());
  }

  @Test
  void getEnvClusterInfoShouldPreservePerEnvFailures() {
    String appId = "someApp";
    when(portalSettings.getActiveEnvs()).thenReturn(Lists.newArrayList(Env.DEV));
    when(appService.createEnvNavNode(Env.DEV, appId)).thenThrow(new IllegalStateException("boom"));

    List<OpenEnvClusterInfo> result = service.getEnvClusterInfo(appId);

    assertEquals(1, result.size());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.get(0).getCode());
    assertEquals("DEV", result.get(0).getEnv());
    assertEquals(0, result.get(0).getClusters().size());
    assertEquals("load env:DEV cluster error.", result.get(0).getMessage());
    assertFalse(result.get(0).getMessage().contains("boom"));
  }
}
