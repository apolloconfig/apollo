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

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppNamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.server.service.NamespaceOpenApiService;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for NamespaceController endpoints (authorization mocked).
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = NamespaceController.class)
public class NamespaceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private NamespaceOpenApiService namespaceOpenApiService;

  @MockBean
  private AppNamespaceOpenApiService appNamespaceOpenApiService;

  @MockBean
  private NamespaceService namespaceService;

  @MockBean
  private AppNamespaceService appNamespaceService;

  @MockBean
  private ApplicationEventPublisher applicationEventPublisher;

  @MockBean
  private com.ctrip.framework.apollo.portal.spi.UserService userService;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testCreateAppNamespace_success() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateNamespacePermission("appId")).thenReturn(true);
    UserInfo user = new UserInfo();
    user.setUserId("apollo");
    Mockito.when(userService.findByUserId("apollo")).thenReturn(user);

    OpenAppNamespaceDTO req = new OpenAppNamespaceDTO();
    req.setAppId("appId");
    req.setName("application");
    req.setFormat("properties");
    req.setDataChangeCreatedBy("apollo");

    Mockito.when(namespaceOpenApiService.createAppNamespace(any(OpenAppNamespaceDTO.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(MockMvcRequestBuilders.post("/openapi/v1/apps/appId/appnamespaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appId", is("appId")))
        .andExpect(jsonPath("$.name", is("application")));
  }

  @Test
  public void testCreateAppNamespace_appIdMismatch() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateNamespacePermission("appInPath"))
        .thenReturn(true);
    UserInfo user = new UserInfo();
    user.setUserId("apollo");
    Mockito.when(userService.findByUserId("apollo")).thenReturn(user);

    OpenAppNamespaceDTO req = new OpenAppNamespaceDTO();
    req.setAppId("appInBody");
    req.setName("application");
    req.setFormat("properties");
    req.setDataChangeCreatedBy("apollo");

    mockMvc.perform(MockMvcRequestBuilders.post("/openapi/v1/apps/appInPath/appnamespaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateAppNamespace_invalidName() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateNamespacePermission("appId"))
        .thenReturn(true);
    Mockito.when(userService.findByUserId("apollo")).thenReturn(new UserInfo());

    OpenAppNamespaceDTO req = new OpenAppNamespaceDTO();
    req.setAppId("appId");
    req.setName("invalid name");
    req.setFormat("properties");
    req.setDataChangeCreatedBy("apollo");

    mockMvc.perform(MockMvcRequestBuilders.post("/openapi/v1/apps/appId/appnamespaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE)))
        .andExpect(content().string(containsString(InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE)));
  }

  @Test
  public void testCreateAppNamespace_invalidFormat() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateNamespacePermission("appId"))
        .thenReturn(true);
    Mockito.when(userService.findByUserId("apollo")).thenReturn(new UserInfo());

    OpenAppNamespaceDTO req = new OpenAppNamespaceDTO();
    req.setAppId("appId");
    req.setName("valid-name");
    req.setFormat("invalid-format");
    req.setDataChangeCreatedBy("apollo");

    mockMvc.perform(MockMvcRequestBuilders.post("/openapi/v1/apps/appId/appnamespaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testFindNamespaces_ok() throws Exception {
    OpenNamespaceDTO ns = new OpenNamespaceDTO();
    // minimal fields; tests only status and size
    Mockito.when(namespaceOpenApiService.getNamespaces("app", "DEV", "default", true))
        .thenReturn(Collections.singletonList(ns));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  public void testLoadNamespace_foundAndNotFound() throws Exception {
    OpenNamespaceDTO ns = new OpenNamespaceDTO();
    Mockito.when(namespaceOpenApiService.getNamespace("app", "DEV", "default", "application", true))
        .thenReturn(ns);
    Mockito.when(namespaceOpenApiService.getNamespace("app", "DEV", "default", "missing", true))
        .thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application"))
        .andExpect(status().isOk());

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/missing"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testGetNamespaceLock_foundAndNotFound() throws Exception {
    Mockito.when(namespaceOpenApiService.getNamespaceLock("app", "DEV", "default", "application"))
        .thenReturn(new OpenNamespaceLockDTO());
    Mockito.when(namespaceOpenApiService.getNamespaceLock("app", "DEV", "default", "missing"))
        .thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/lock"))
        .andExpect(status().isOk());
    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/missing/lock"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testGetAppNamespaces_listing() throws Exception {
    OpenAppNamespaceDTO a = new OpenAppNamespaceDTO();
    a.setAppId("a");
    a.setName("x");
    OpenAppNamespaceDTO b = new OpenAppNamespaceDTO();
    b.setAppId("b");
    b.setName("y");

    Mockito.when(appNamespaceOpenApiService.findPublicAppNamespaces()).thenReturn(Arrays.asList(a, b));
    Mockito.when(appNamespaceOpenApiService.findAll()).thenReturn(Collections.singletonList(a));
    Mockito.when(appNamespaceOpenApiService.findByAppId("app"))
        .thenReturn(Collections.singletonList(a));
    Mockito.when(appNamespaceOpenApiService.findByAppIdAndName("app", "x"))
        .thenReturn(a);
    Mockito.when(appNamespaceOpenApiService.findByAppIdAndName("app", "none"))
        .thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/appnamespaces")
            .param("publicOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/appnamespaces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/app/appnamespaces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/app/appnamespaces/x"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name", is("x")));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/app/appnamespaces/none"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testDeleteAppNamespace() throws Exception {
    Mockito.when(consumerPermissionValidator.hasDeleteNamespacePermission("app"))
        .thenReturn(true);
    OpenAppNamespaceDTO deleted = new OpenAppNamespaceDTO();
    deleted.setAppId("app");
    deleted.setName("x");
    Mockito.when(appNamespaceOpenApiService.deleteAppNamespace("app", "x")).thenReturn(deleted);

    mockMvc.perform(MockMvcRequestBuilders.delete("/openapi/v1/apps/app/appnamespaces/x"))
        .andExpect(status().isNoContent());

    Mockito.verify(appNamespaceOpenApiService, times(1)).deleteAppNamespace("app", "x");
  }

  @Test
  public void testGetNamespacesReleaseStatus() throws Exception {
    Map<String, Map<String, Boolean>> statusMap = new HashMap<>();
    statusMap.put("DEV", Collections.singletonMap("ns", true));
    Mockito.when(namespaceService.getNamespacesPublishInfo("app"))
        .thenReturn(statusMap);

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/app/namespaces/releases/status"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("DEV")));
  }

  @Test
  public void testGetPublicAppNamespaceInstances_empty() throws Exception {
    Mockito.when(namespaceService.getPublicAppNamespaceAllNamespaces(eq(Env.DEV), eq("public.ns"), eq(0), eq(10)))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/appnamespaces/public.ns/instances")
            .param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  public void testGetPublicNamespaceAssociation_notFound() throws Exception {
    Mockito.when(namespaceService.findPublicNamespaceForAssociatedNamespace(eq(Env.DEV), anyString(), anyString(), anyString()))
        .thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/apps/app/envs/DEV/clusters/default/namespaces/application/public-association"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testCheckNamespaceIntegrity() throws Exception {
    // config db namespaces empty
    Mockito.when(namespaceService.findNamespaces("app", Env.DEV, "default"))
        .thenReturn(Collections.emptyList());
    // portal db has two private app namespaces
    AppNamespace ns1 = Mockito.mock(AppNamespace.class);
    Mockito.when(ns1.isPublic()).thenReturn(false);
    Mockito.when(ns1.getName()).thenReturn("db");
    AppNamespace ns2 = Mockito.mock(AppNamespace.class);
    Mockito.when(ns2.isPublic()).thenReturn(false);
    Mockito.when(ns2.getName()).thenReturn("cache");
    Mockito.when(appNamespaceService.findByAppId("app")).thenReturn(Arrays.asList(ns1, ns2));

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/apps/app/envs/DEV/clusters/default/namespaces/integrity-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }
}
