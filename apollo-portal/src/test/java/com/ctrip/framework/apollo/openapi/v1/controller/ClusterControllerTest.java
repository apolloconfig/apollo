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

import com.ctrip.framework.apollo.openapi.model.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.server.service.ClusterOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.gson.Gson;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"api.pool.max.total=100", "api.pool.max.per.route=100",
    "api.connectionTimeToLive=30000", "api.connectTimeout=5000", "api.readTimeout=5000"})
public class ClusterControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockBean
  private UserService userService;

  @MockBean
  private ClusterOpenApiService clusterOpenApiService;

  private final Gson gson = new Gson();
  private UserInfo authenticatedUser;

  @Before
  public void setUpSecurityMocks() {
    when(unifiedPermissionValidator.hasCreateClusterPermission(Mockito.anyString()))
        .thenReturn(true);
    when(unifiedPermissionValidator.isAppAdmin(Mockito.anyString())).thenReturn(true);

    authenticatedUser = new UserInfo();
    authenticatedUser.setUserId("test-operator");
    when(userService.findByUserId(Mockito.anyString())).thenReturn(authenticatedUser);

    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  private void authenticate() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList()));
  }

  @Test
  public void testGetCluster() throws Exception {
    String appId = "test-app";
    String env = "DEV";
    String clusterName = "default";
    OpenClusterDTO clusterDTO = new OpenClusterDTO();
    clusterDTO.setAppId(appId);
    clusterDTO.setName(clusterName);

    when(clusterOpenApiService.getCluster(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString())).thenReturn(clusterDTO);

    this.mockMvc
        .perform(
            MockMvcRequestBuilders.get("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}",
                env, appId, clusterName).accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk())
        .andExpect(jsonPath("$.appId", is(appId))).andExpect(jsonPath("$.name", is(clusterName)));

    verify(clusterOpenApiService).getCluster(appId, env, clusterName);
  }

  @Test
  public void testCreateCluster() throws Exception {
    String appId = "test-app";
    String env = "DEV";
    String clusterName = "new-cluster";
    String operator = "apollo";

    authenticate();

    OpenClusterDTO clusterDTO = new OpenClusterDTO();
    clusterDTO.setAppId(appId);
    clusterDTO.setName(clusterName);
    clusterDTO.setDataChangeCreatedBy(operator);

    UserInfo user = new UserInfo();
    user.setUserId(operator);

    when(userService.findByUserId(operator)).thenReturn(user);
    when(clusterOpenApiService.createCluster(eq(env), any(OpenClusterDTO.class)))
        .thenReturn(clusterDTO);

    this.mockMvc
        .perform(
            MockMvcRequestBuilders.post("/openapi/v1/envs/{env}/apps/{appId}/clusters", env, appId)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(gson.toJson(clusterDTO)))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk())
        .andExpect(jsonPath("$.appId", is(appId))).andExpect(jsonPath("$.name", is(clusterName)));

    verify(clusterOpenApiService).createCluster(eq(env), any(OpenClusterDTO.class));
  }

  @Test
  public void testCreateClusterWithAppIdMismatch() throws Exception {
    String appIdInPath = "app-in-path";
    String appIdInBody = "app-in-body";
    String env = "DEV";
    String clusterName = "new-cluster";
    String operator = "apollo";

    authenticate();

    OpenClusterDTO clusterDTO = new OpenClusterDTO();
    clusterDTO.setAppId(appIdInBody);
    clusterDTO.setName(clusterName);
    clusterDTO.setDataChangeCreatedBy(operator);

    this.mockMvc
        .perform(MockMvcRequestBuilders
            .post("/openapi/v1/envs/{env}/apps/{appId}/clusters", env, appIdInPath)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .content(gson.toJson(clusterDTO)))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isBadRequest());

    verify(clusterOpenApiService, never()).createCluster(Mockito.anyString(),
        Mockito.any(OpenClusterDTO.class));
  }

  @Test
  public void testDeleteCluster() throws Exception {
    String appId = "test-app";
    String env = "DEV";
    String clusterName = "default";
    String operator = "apollo";

    authenticate();

    UserInfo user = new UserInfo();
    user.setUserId(operator);

    when(userService.findByUserId(operator)).thenReturn(user);

    Mockito.doNothing().when(clusterOpenApiService).deleteCluster(env, appId, clusterName);

    this.mockMvc
        .perform(MockMvcRequestBuilders
            .delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId,
                clusterName)
            .accept(MediaType.APPLICATION_JSON).param("operator", operator))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk());

    verify(clusterOpenApiService, times(1)).deleteCluster(env, appId, clusterName);
  }
}
