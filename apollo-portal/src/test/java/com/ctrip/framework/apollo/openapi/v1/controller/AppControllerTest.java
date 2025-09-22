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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.common.http.MultiResponseEntity;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.server.service.impl.ServerAppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author wxq
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AppController.class)
public class AppControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ConsumerAuthUtil consumerAuthUtil;
  
  @MockBean
  private ConsumerService consumerService;

  @MockBean
  private ServerAppOpenApiService serverAppOpenApiService;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  @Test
  public void testGetAuthorizedApps() throws Exception {
    final long consumerId = 123456L;
    Mockito.when(this.consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(consumerId);

    Set<String> authorized = new HashSet<>(Arrays.asList("app1", "app2"));
    Mockito.when(this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId))
        .thenReturn(authorized);

    List<OpenAppDTO> mockApps = new ArrayList<>();
    mockApps.add(openApp("app1"));
    mockApps.add(openApp("app2"));
    Mockito.when(this.serverAppOpenApiService.getAppsInfo(Mockito.anyList()))
        .thenReturn(mockApps);

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps")
            .param("authorized", "true"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    ArgumentCaptor<List<String>> idsCaptor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(this.serverAppOpenApiService, times(1)).getAppsInfo(idsCaptor.capture());
    List<String> captured = idsCaptor.getValue();
    // order is not guaranteed, so verify as sets
    org.junit.Assert.assertEquals(new HashSet<>(captured), authorized);
  }

  @Test
  public void testGetAppsWithIds() throws Exception {
    List<OpenAppDTO> mockApps = Arrays.asList(openApp("foo"), openApp("bar"));
    Mockito.when(this.serverAppOpenApiService.getAppsInfo(eq(Arrays.asList("foo", "bar"))))
        .thenReturn(mockApps);

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps")
            .param("appIds", "foo,bar"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].appId", is("foo")))
        .andExpect(jsonPath("$[1].appId", is("bar")));

    Mockito.verify(this.consumerService, never()).findAppIdsAuthorizedByConsumerId(any(Long.class));
  }

  @Test
  public void testGetAppsWithoutIds() throws Exception {
    final long consumerId = 42L;
    Mockito.when(this.consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(consumerId);
    Mockito.when(this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId))
        .thenReturn(new HashSet<>(Arrays.asList("a", "b")));
    Mockito.when(this.serverAppOpenApiService.getAppsInfo(Mockito.anyList()))
        .thenReturn(Arrays.asList(openApp("a"), openApp("b")));

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    Mockito.verify(this.consumerService, times(1)).findAppIdsAuthorizedByConsumerId(consumerId);
    Mockito.verify(this.serverAppOpenApiService, times(1)).getAppsInfo(Mockito.anyList());
  }

  @Test
  public void testGetSingleAppFound() throws Exception {
    Mockito.when(this.serverAppOpenApiService.getAppsInfo(eq(Arrays.asList("demo"))))
        .thenReturn(Arrays.asList(openApp("demo")));

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/demo"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appId", is("demo")));
  }

  @Test
  public void testGetSingleAppNotFound() throws Exception {
    Mockito.when(this.serverAppOpenApiService.getAppsInfo(eq(Arrays.asList("none"))))
        .thenReturn(new ArrayList<>());

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/none"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateAppAssignRole() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    Mockito.when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(11L);

    OpenAppDTO app = openApp("new-app");
    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setApp(app);
    req.setAssignAppRoleToSelf(true);

    this.mockMvc.perform(
            MockMvcRequestBuilders.post("/openapi/v1/apps")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(req))
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appId", is("new-app")));

    Mockito.verify(serverAppOpenApiService, times(1)).createApp(any(OpenCreateAppDTO.class));
    Mockito.verify(consumerService, times(1)).assignAppRoleToConsumer(11L, "new-app");
  }

  @Test
  public void testCreateAppBadRequestWhenAppNull() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setAssignAppRoleToSelf(false);
    this.mockMvc.perform(
            MockMvcRequestBuilders.post("/openapi/v1/apps")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(req))
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateAppBadRequestWhenAppIdNull() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    OpenCreateAppDTO req = new OpenCreateAppDTO();
    req.setApp(new OpenAppDTO());
    req.setAssignAppRoleToSelf(false);
    this.mockMvc.perform(
            MockMvcRequestBuilders.post("/openapi/v1/apps")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(req))
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testUpdateAppOk() throws Exception {
    Mockito.when(consumerPermissionValidator.isAppAdmin("app-x")).thenReturn(true);
    OpenAppDTO app = openApp("app-x");

    this.mockMvc.perform(
            MockMvcRequestBuilders.put("/openapi/v1/apps/app-x")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(app))
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appId", is("app-x")));

    Mockito.verify(serverAppOpenApiService, times(1)).updateApp(any(OpenAppDTO.class));
  }

  @Test
  public void testUpdateAppIdMismatch() throws Exception {
    Mockito.when(consumerPermissionValidator.isAppAdmin("foo")).thenReturn(true);
    OpenAppDTO app = openApp("bar");

    this.mockMvc.perform(
            MockMvcRequestBuilders.put("/openapi/v1/apps/foo")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(app))
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testDeleteApp() throws Exception {
    Mockito.when(consumerPermissionValidator.isAppAdmin("to-del")).thenReturn(true);
    this.mockMvc.perform(MockMvcRequestBuilders.delete("/openapi/v1/apps/to-del"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk());
    Mockito.verify(serverAppOpenApiService, times(1)).deleteApp("to-del");
  }

  @Test
  public void testGetAppEnvClusters() throws Exception {
    List<OpenEnvClusterDTO> envs = new ArrayList<>();
    OpenEnvClusterDTO dto = new OpenEnvClusterDTO();
    dto.setClusters(Arrays.asList("x", "y"));
    dto.setEnv("DEV");
    envs.add(dto);
    Mockito.when(serverAppOpenApiService.getEnvClusterInfo("demo")).thenReturn(envs);

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/demo/envclusters"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].env", is("DEV")));
  }

  @Test
  public void testGetAppsBySelf() throws Exception {
    Mockito.when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(7L);
    Mockito.when(consumerService.findAppIdsAuthorizedByConsumerId(7L))
        .thenReturn(new HashSet<>(Arrays.asList("x", "y")));
    Mockito.when(serverAppOpenApiService.getAppsBySelf(any(Set.class), any()))
        .thenReturn(Arrays.asList(openApp("x"), openApp("y")));

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/by-self")
            .param("page", "0").param("size", "10"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  public void testFindMissEnvs() throws Exception {
    Mockito.when(serverAppOpenApiService.findMissEnvs("zzz"))
        .thenReturn(MultiResponseEntity.ok());

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/zzz/miss_envs"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk());
  }

  @Test
  public void testCreateAppInEnv() throws Exception {
    // Allow create application permission for openapi endpoint
    Mockito.when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    Mockito.when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(100L);

    com.ctrip.framework.apollo.openapi.entity.Consumer c =
        new com.ctrip.framework.apollo.openapi.entity.Consumer();
    c.setName("operator-x");
    Mockito.when(consumerService.getConsumerByConsumerId(100L)).thenReturn(c);

    OpenAppDTO app = openApp("env-app");

    this.mockMvc.perform(
            MockMvcRequestBuilders.post("/openapi/v1/apps/envs/DEV")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(app))
        )
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk());

    // verify invocation with captured DTO instead of instance equality
    ArgumentCaptor<OpenAppDTO> appCaptor = ArgumentCaptor.forClass(OpenAppDTO.class);
    Mockito.verify(serverAppOpenApiService, times(1))
        .createAppInEnv(Mockito.eq("DEV"), appCaptor.capture(), Mockito.eq("operator-x"));
    OpenAppDTO capturedApp = appCaptor.getValue();
    org.junit.Assert.assertEquals("env-app", capturedApp.getAppId());
    org.junit.Assert.assertEquals("env-app", capturedApp.getName());
  }

  private static OpenAppDTO openApp(String appId) {
    OpenAppDTO dto = new OpenAppDTO();
    dto.setAppId(appId);
    dto.setName(appId);
    return dto;
  }

}
