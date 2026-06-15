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
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class ClusterControllerParamBindLowLevelTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private ClusterOpenApiService clusterOpenApiService;

  @MockitoBean
  private UserInfoHolder userInfoHolder;

  private final Gson gson = new Gson();

  @Before
  public void setUp() {
    when(unifiedPermissionValidator.hasCreateClusterPermission(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.isAppAdmin(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);

    UserInfo user = new UserInfo();
    user.setUserId("tester");
    when(userService.findByUserId(anyString())).thenReturn(user);
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
  }

  @Before
  public void setAuthentication() {
    // put a dummy Authentication into SecurityContext so @PreAuthorize won't fail
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("tester", "N/A", AuthorityUtils.NO_AUTHORITIES));
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  @Test
  public void getCluster_shouldBind_path() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "default";

    when(clusterOpenApiService.getCluster(appId, env, clusterName))
        .thenReturn(new OpenClusterDTO());

    mockMvc.perform(
        get("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId, clusterName))
        .andExpect(status().isOk());

    ArgumentCaptor<String> appIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> envCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> clusterNameCaptor = ArgumentCaptor.forClass(String.class);

    verify(clusterOpenApiService, times(1)).getCluster(appIdCaptor.capture(), envCaptor.capture(),
        clusterNameCaptor.capture());
    assertThat(appIdCaptor.getValue()).isEqualTo(appId);
    assertThat(envCaptor.getValue()).isEqualTo(env);
    assertThat(clusterNameCaptor.getValue()).isEqualTo(clusterName);
  }

  @Test
  public void createCluster_shouldBind_path_and_body() throws Exception {
    String appId = "app-1";
    String env = "DEV";

    OpenClusterDTO dto = new OpenClusterDTO();
    dto.setAppId(appId);
    dto.setName("new-cluster");
    dto.setDataChangeCreatedBy("tester");

    when(clusterOpenApiService.createCluster(anyString(), any(OpenClusterDTO.class), anyString()))
        .thenReturn(dto);

    mockMvc
        .perform(post("/openapi/v1/envs/{env}/apps/{appId}/clusters", env, appId)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(dto)))
        .andExpect(status().isOk());

    ArgumentCaptor<String> envCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<OpenClusterDTO> dtoCap = ArgumentCaptor.forClass(OpenClusterDTO.class);

    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);

    verify(clusterOpenApiService, times(1)).createCluster(envCaptor.capture(), dtoCap.capture(),
        operatorCaptor.capture());
    assertThat(envCaptor.getValue()).isEqualTo(env);
    assertThat(dtoCap.getValue().getAppId()).isEqualTo(appId);
    assertThat(dtoCap.getValue().getName()).isEqualTo("new-cluster");
    assertThat(operatorCaptor.getValue()).isEqualTo("tester");
  }

  @Test
  public void createCluster_shouldRejectUserTokenWithoutClusterScope() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "new-cluster";
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(unifiedPermissionValidator.hasCreateClusterPermission(appId)).thenReturn(true);
    when(unifiedPermissionValidator.hasCreateClusterPermission(appId, env, clusterName))
        .thenReturn(false);

    UserInfo currentUser = new UserInfo();
    currentUser.setUserId("token-user");
    when(userInfoHolder.getUser()).thenReturn(currentUser);

    OpenClusterDTO dto = new OpenClusterDTO();
    dto.setAppId(appId);
    dto.setName(clusterName);

    mockMvc
        .perform(post("/openapi/v1/envs/{env}/apps/{appId}/clusters", env, appId)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(dto)))
        .andExpect(status().isForbidden());

    verify(clusterOpenApiService, never()).createCluster(anyString(), any(OpenClusterDTO.class),
        anyString());
  }

  @Test
  public void deleteCluster_shouldBind_path_and_query() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "default";
    String operator = "tester";

    doNothing().when(clusterOpenApiService).deleteCluster(env, appId, clusterName, operator);

    mockMvc.perform(delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId,
        clusterName).param("operator", operator)).andExpect(status().isOk());

    ArgumentCaptor<String> envCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> appIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> clusterNameCaptor = ArgumentCaptor.forClass(String.class);

    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);

    verify(clusterOpenApiService, times(1)).deleteCluster(envCaptor.capture(),
        appIdCaptor.capture(), clusterNameCaptor.capture(), operatorCaptor.capture());
    assertThat(envCaptor.getValue()).isEqualTo(env);
    assertThat(appIdCaptor.getValue()).isEqualTo(appId);
    assertThat(clusterNameCaptor.getValue()).isEqualTo(clusterName);
    assertThat(operatorCaptor.getValue()).isEqualTo(operator);
  }

  @Test
  public void deleteCluster_shouldAllowConsumerAppAdminWithoutSuperAdmin() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "default";
    String operator = "tester";
    when(unifiedPermissionValidator.isAppAdmin(appId)).thenReturn(true);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    doNothing().when(clusterOpenApiService).deleteCluster(env, appId, clusterName, operator);

    mockMvc.perform(delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId,
        clusterName).param("operator", operator)).andExpect(status().isOk());

    verify(clusterOpenApiService, times(1)).deleteCluster(env, appId, clusterName, operator);
  }

  @Test
  public void deleteCluster_shouldRejectConsumerWithoutAppAdminOrSuperAdmin() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "default";
    String operator = "tester";
    when(unifiedPermissionValidator.isAppAdmin(appId)).thenReturn(false);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);

    mockMvc.perform(delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId,
        clusterName).param("operator", operator)).andExpect(status().isForbidden());

    verify(clusterOpenApiService, never()).deleteCluster(anyString(), anyString(), anyString(),
        anyString());
  }

  @Test
  public void deleteCluster_shouldAllowPortalSuperAdmin() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "default";
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);
    when(unifiedPermissionValidator.isAppAdmin(appId)).thenReturn(false);
    UserInfo currentUser = new UserInfo();
    currentUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(currentUser);

    mockMvc.perform(delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId,
        clusterName)).andExpect(status().isOk());

    verify(clusterOpenApiService, times(1)).deleteCluster(env, appId, clusterName, "portal-user");
  }

  @Test
  public void deleteCluster_shouldRejectPortalUserWithoutSuperAdmin() throws Exception {
    String appId = "app-1";
    String env = "DEV";
    String clusterName = "default";
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(false);
    when(unifiedPermissionValidator.isAppAdmin(appId)).thenReturn(true);

    mockMvc.perform(delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId,
        clusterName)).andExpect(status().isForbidden());

    verify(clusterOpenApiService, never()).deleteCluster(anyString(), anyString(), anyString(),
        anyString());
  }
}
