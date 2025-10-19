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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
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
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AppControllerParamBindLowLevelTest {

  @Autowired
  private MockMvc mockMvc;

  // Keep the same mocks as your working test to satisfy context wiring
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

  private final Gson gson = new Gson();

  @Before
  public void setUp() {
    when(consumerPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    when(consumerPermissionValidator.isAppAdmin(anyString())).thenReturn(true);

    UserInfo user = new UserInfo();
    user.setUserId("tester");
    when(userService.findByUserId(anyString())).thenReturn(user);
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
  }

  @Test
  public void createAppInEnv_shouldBind_env_query_body() throws Exception {
    OpenAppDTO dto = new OpenAppDTO();
    dto.setAppId("demo");
    dto.setName("demo-name");
    dto.setOwnerName("owner");
    dto.setOwnerEmail("owner@example.com");
    dto.setOrgId("org-1");
    dto.setOrgName("Org");

    // Adjust URL here if your mapping is different
    mockMvc.perform(post("/openapi/v1/apps/envs/{env}", "DEV").param("operator", "bob")
        .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(dto))).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    ArgumentCaptor<String> envCap = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<OpenAppDTO> dtoCap = ArgumentCaptor.forClass(OpenAppDTO.class);
    ArgumentCaptor<String> opCap = ArgumentCaptor.forClass(String.class);

    verify(appOpenApiService, times(1)).createAppInEnv(envCap.capture(), dtoCap.capture(),
        opCap.capture());
    assertThat(envCap.getValue()).isEqualTo("DEV");
    assertThat(opCap.getValue()).isEqualTo("bob");
    assertThat(dtoCap.getValue().getAppId()).isEqualTo("demo");
    assertThat(dtoCap.getValue().getName()).isEqualTo("demo-name");
  }

  @Test
  public void getAppsBySelf_shouldBind_page_size_and_ids() throws Exception {
    long consumerId = 9L;
    Set<String> authorizedAppIds = new HashSet<>();
    authorizedAppIds.add("app1");
    authorizedAppIds.add("app2");
    when(consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(consumerId);
    when(consumerService.findAppIdsAuthorizedByConsumerId(consumerId)).thenReturn(authorizedAppIds);

    mockMvc.perform(get("/openapi/v1/apps/by-self").param("page", "0").param("size", "10"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    ArgumentCaptor<Set> idsCap = ArgumentCaptor.forClass(Set.class);
    ArgumentCaptor<Integer> pageCap = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);

    verify(appOpenApiService, times(1)).getAppsBySelf(idsCap.capture(), pageCap.capture(),
        sizeCap.capture());
    assertThat(idsCap.getValue()).containsExactlyInAnyOrder("app1", "app2");
    assertThat(pageCap.getValue()).isEqualTo(0);
    assertThat(sizeCap.getValue()).isEqualTo(10);
  }

  @Test
  public void updateApp_shouldBind_path_query_body() throws Exception {
    OpenAppDTO dto = new OpenAppDTO();
    dto.setAppId("app-1");
    dto.setName("new-name");

    doNothing().when(appOpenApiService).updateApp(any(OpenAppDTO.class));

    mockMvc.perform(put("/openapi/v1/apps/{appId}", "app-1").param("operator", "david")
        .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(dto))).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    ArgumentCaptor<OpenAppDTO> dtoCap = ArgumentCaptor.forClass(OpenAppDTO.class);
    verify(appOpenApiService, times(1)).updateApp(dtoCap.capture());
    assertThat(dtoCap.getValue().getAppId()).isEqualTo("app-1");
    assertThat(dtoCap.getValue().getName()).isEqualTo("new-name");
  }

  @Test
  public void deleteApp_shouldBind_path_and_query() throws Exception {
    when(appOpenApiService.deleteApp("app-1")).thenReturn(new OpenAppDTO());

    mockMvc.perform(delete("/openapi/v1/apps/{appId}", "app-1").param("operator", "alice"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    verify(appOpenApiService, times(1)).deleteApp("app-1");
  }
}
