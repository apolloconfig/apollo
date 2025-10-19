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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.MultiResponseEntity;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.repository.ConsumerAuditRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerTokenRepository;
import com.ctrip.framework.apollo.openapi.server.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RolePermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** @author wxq */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"api.pool.max.total=100", "api.pool.max.per.route=100",
    "api.connectionTimeToLive=30000", "api.connectTimeout=5000", "api.readTimeout=5000"})
public class AppControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  @MockBean
  private PortalSettings portalSettings;

  @MockBean
  private AppService appService;

  @MockBean
  private ClusterService clusterService;

  @MockBean
  private ConsumerAuthUtil consumerAuthUtil;

  @MockBean
  private PermissionRepository permissionRepository;

  @MockBean
  private AppOpenApiService appOpenApiService;

  @MockBean
  private ConsumerService consumerService;

  @MockBean
  private RolePermissionRepository rolePermissionRepository;

  @MockBean
  private UserInfoHolder userInfoHolder;
  @MockBean
  private ConsumerTokenRepository consumerTokenRepository;
  @MockBean
  private ConsumerRepository consumerRepository;
  @MockBean
  private ConsumerAuditRepository consumerAuditRepository;
  @MockBean
  private ConsumerRoleRepository consumerRoleRepository;
  @MockBean
  private RolePermissionService rolePermissionService;
  @MockBean
  private UserService userService;
  @MockBean
  private RoleRepository roleRepository;
  @MockBean
  private RoleInitializationService roleInitializationService;
  @MockBean
  private ApplicationEventPublisher applicationEventPublisher;

  private final Gson gson = new Gson();

  @Before
  public void setUpSecurityMocks() {
    when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    when(consumerPermissionValidator.hasCreateNamespacePermission(Mockito.any())).thenReturn(true);
    when(consumerPermissionValidator.isAppAdmin(Mockito.anyString())).thenReturn(true);

    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("test");
    when(userService.findByUserId(Mockito.anyString())).thenReturn(userInfo);
  }

  @Test
  public void testFindAppsAuthorized() throws Exception {
    final long consumerId = 123456;
    when(this.consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(consumerId);

    Set<String> authorizedAppIds = Sets.newHashSet("app1", "app2");
    when(this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId))
        .thenReturn(authorizedAppIds);

    when(this.appOpenApiService.getAppsInfo(Mockito.anyList())).thenReturn(Collections.emptyList());

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/authorized"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

    Mockito.verify(this.consumerService, Mockito.times(1))
        .findAppIdsAuthorizedByConsumerId(consumerId);

    ArgumentCaptor<List> appIdsCaptor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(this.appOpenApiService).getAppsInfo(appIdsCaptor.capture());
    @SuppressWarnings("unchecked")
    List<String> appIds = appIdsCaptor.getValue();
    assertEquals(authorizedAppIds, Sets.newHashSet(appIds));
  }

  @Test
  public void testGetEnvClusterInfo() throws Exception {
    String appId = "someAppId";

    OpenEnvClusterDTO devCluster = new OpenEnvClusterDTO();
    devCluster.setEnv("DEV");
    devCluster.setClusters(Lists.newArrayList("default"));
    OpenEnvClusterDTO fatCluster = new OpenEnvClusterDTO();
    fatCluster.setEnv("FAT");
    fatCluster.setClusters(Lists.newArrayList("default", "feature"));

    when(appOpenApiService.getEnvClusterInfo(appId))
        .thenReturn(Lists.newArrayList(devCluster, fatCluster));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId + "/envclusters"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].env").value("DEV"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].clusters[0]").value("default"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].env").value("FAT"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].clusters[0]").value("default"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].clusters[1]").value("feature"));

    Mockito.verify(appOpenApiService).getEnvClusterInfo(appId);
  }

  @Test
  public void testFindAppsByIds() throws Exception {
    String appId1 = "app1";
    String appId2 = "app2";
    Set<String> appIds = Sets.newHashSet(appId1, appId2);

    OpenAppDTO app1 = new OpenAppDTO();
    app1.setAppId(appId1);
    OpenAppDTO app2 = new OpenAppDTO();
    app2.setAppId(appId2);
    List<OpenAppDTO> apps = Lists.newArrayList(app1, app2);

    when(appOpenApiService.getAppsInfo(Mockito.anyList())).thenReturn(apps);

    mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/apps").param("appIds",
            String.join(",", appIds)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value(appId1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].appId").value(appId2));

    ArgumentCaptor<List> requestIdsCaptor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(appOpenApiService).getAppsInfo(requestIdsCaptor.capture());
    @SuppressWarnings("unchecked")
    List<String> requestedIds = requestIdsCaptor.getValue();
    assertEquals(appIds, Sets.newHashSet(requestedIds));
  }

  @Test
  public void testFindAllApps() throws Exception {
    OpenAppDTO app1 = new OpenAppDTO();
    app1.setAppId("app1");
    OpenAppDTO app2 = new OpenAppDTO();
    app2.setAppId("app2");
    List<OpenAppDTO> apps = Lists.newArrayList(app1, app2);

    when(appOpenApiService.getAllApps()).thenReturn(apps);

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value("app1"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].appId").value("app2"));

    Mockito.verify(appOpenApiService).getAllApps();
  }

  @Test
  public void testGetApp() throws Exception {
    String appId = "someAppId";
    OpenAppDTO app = new OpenAppDTO();
    app.setAppId(appId);

    when(appOpenApiService.getAppsInfo(Collections.singletonList(appId)))
        .thenReturn(Collections.singletonList(app));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.appId").value(appId));

    Mockito.verify(appOpenApiService).getAppsInfo(Collections.singletonList(appId));
  }

  @Test
  public void testGetAppNotFound() throws Exception {
    String appId = "someAppId";

    when(appOpenApiService.getAppsInfo(Collections.singletonList(appId)))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());

    Mockito.verify(appOpenApiService).getAppsInfo(Collections.singletonList(appId));
  }

  @Test
  public void testGetAppsBySelf() throws Exception {
    long consumerId = 1L;
    int page = 0;
    int size = 10;
    String app1Id = "app1";
    String app2Id = "app2";
    Set<String> authorizedAppIds = Sets.newHashSet(app1Id, app2Id);

    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(consumerId);
    when(this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId))
        .thenReturn(authorizedAppIds);

    OpenAppDTO app1 = new OpenAppDTO();
    app1.setAppId(app1Id);
    OpenAppDTO app2 = new OpenAppDTO();
    app2.setAppId(app2Id);
    List<OpenAppDTO> apps = Lists.newArrayList(app1, app2);

    when(appOpenApiService.getAppsBySelf(authorizedAppIds, page, size)).thenReturn(apps);

    mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/apps/by-self")
            .param("page", String.valueOf(page)).param("size", String.valueOf(size)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value(app1Id))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].appId").value(app2Id));

    Mockito.verify(this.consumerService).findAppIdsAuthorizedByConsumerId(consumerId);
    Mockito.verify(this.appOpenApiService).getAppsBySelf(authorizedAppIds, page, size);
  }

  @Test
  public void testFindMissEnvs() throws Exception {
    String appId = "someAppId";

    when(appOpenApiService.findMissEnvs(appId))
        .thenReturn(new MultiResponseEntity(HttpStatus.OK.value(), new ArrayList<>()));
    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId + "/miss_envs"))
        .andExpect(MockMvcResultMatchers.status().isOk());

    Mockito.verify(appOpenApiService).findMissEnvs(appId);
  }

  @Test
  public void testUpdateApp() throws Exception {
    String appId = "app1";
    String operator = "operatorUser";
    OpenAppDTO requestDto = new OpenAppDTO();
    requestDto.setAppId(appId);
    requestDto.setName("App One");

    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("test");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userInfo, null, Collections.emptyList()));

    Mockito.doNothing().when(appOpenApiService).updateApp(Mockito.any(OpenAppDTO.class));
    when(consumerPermissionValidator.isAppAdmin(appId)).thenReturn(true);

    mockMvc
        .perform(MockMvcRequestBuilders.put("/openapi/v1/apps/" + appId).param("operator", operator)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(requestDto)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.appId").value(appId))
        .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("App One"));
  }

  @Test
  public void testUpdateAppWithMismatchedAppId() throws Exception {
    String pathAppId = "app-path";
    String operator = "operatorUser";
    OpenAppDTO requestDto = new OpenAppDTO();
    requestDto.setAppId("app-body");

    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("test");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userInfo, null, Collections.emptyList()));

    when(consumerPermissionValidator.isAppAdmin(pathAppId)).thenReturn(true);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/openapi/v1/apps/" + pathAppId).param("operator", operator)
                .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(requestDto)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());

    Mockito.verify(appOpenApiService, Mockito.never()).updateApp(Mockito.any());
  }

  @Test
  public void testDeleteApp() throws Exception {
    String appId = "app1";
    String operator = "deleter";

    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("test");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userInfo, null, Collections.emptyList()));

    when(appOpenApiService.deleteApp(appId)).thenReturn(new OpenAppDTO());
    when(consumerPermissionValidator.isAppAdmin(appId)).thenReturn(true);

    mockMvc.perform(delete("/openapi/v1/apps/" + appId).param("operator", operator))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(""));

    Mockito.verify(appOpenApiService).deleteApp(appId);
  }
}
