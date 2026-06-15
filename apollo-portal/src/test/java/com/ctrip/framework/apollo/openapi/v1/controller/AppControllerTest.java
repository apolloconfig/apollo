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

import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenMissEnvDTO;
import com.ctrip.framework.apollo.openapi.repository.ConsumerAuditRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerTokenRepository;
import com.ctrip.framework.apollo.openapi.server.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RolePermissionRepository;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * @author wxq
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"api.pool.max.total=100", "api.pool.max.per.route=100",
    "api.connectionTimeToLive=30000", "api.connectTimeout=5000", "api.readTimeout=5000"})
public class AppControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockitoBean
  private PortalSettings portalSettings;

  @MockitoBean
  private AppService appService;

  @MockitoBean
  private ClusterService clusterService;

  @MockitoBean
  private ConsumerAuthUtil consumerAuthUtil;

  @MockitoBean
  private PermissionRepository permissionRepository;

  @MockitoBean
  private AppOpenApiService appOpenApiService;

  @MockitoBean
  private ConsumerService consumerService;

  @MockitoBean
  private RolePermissionRepository rolePermissionRepository;

  @MockitoBean
  private UserInfoHolder userInfoHolder;
  @MockitoBean
  private ConsumerTokenRepository consumerTokenRepository;
  @MockitoBean
  private ConsumerRepository consumerRepository;
  @MockitoBean
  private ConsumerAuditRepository consumerAuditRepository;
  @MockitoBean
  private ConsumerRoleRepository consumerRoleRepository;
  @MockitoBean
  private RolePermissionService rolePermissionService;
  @MockitoBean
  private UserService userService;
  @MockitoBean
  private RoleRepository roleRepository;
  @MockitoBean
  private RoleInitializationService roleInitializationService;
  @MockitoBean
  private ApplicationEventPublisher applicationEventPublisher;

  private final Gson gson = new Gson();

  @BeforeEach
  public void setUpSecurityMocks() {
    when(unifiedPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    when(unifiedPermissionValidator.hasCreateNamespacePermission(Mockito.any()))
        .thenReturn(true);
    when(unifiedPermissionValidator.isAppAdmin(Mockito.anyString())).thenReturn(true);
    when(unifiedPermissionValidator.isSuperAdmin()).thenReturn(true);
    when(unifiedPermissionValidator.hasReadApplicationPermission(Mockito.anyString()))
        .thenReturn(true);

    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("test");
    when(userService.findByUserId(Mockito.anyString())).thenReturn(userInfo);

    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
  }

  @AfterEach
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void testCreateAppForUserTokenUsesCurrentTokenUser() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    UserInfo tokenUser = new UserInfo();
    tokenUser.setUserId("token-user");
    when(userInfoHolder.getUser()).thenReturn(tokenUser);

    OpenAppDTO app = new OpenAppDTO();
    app.setAppId("user-token-app");
    app.setDataChangeCreatedBy("spoofed-user");
    OpenCreateAppDTO request = new OpenCreateAppDTO();
    request.setApp(app);

    mockMvc.perform(post("/openapi/v1/apps").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(request))).andExpect(MockMvcResultMatchers.status().isOk());

    ArgumentCaptor<OpenCreateAppDTO> requestCaptor =
        ArgumentCaptor.forClass(OpenCreateAppDTO.class);
    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(appOpenApiService).createApp(requestCaptor.capture(), operatorCaptor.capture());
    assertEquals("token-user", operatorCaptor.getValue());
    assertEquals("token-user", requestCaptor.getValue().getApp().getDataChangeCreatedBy());
    assertEquals("token-user", requestCaptor.getValue().getApp().getDataChangeLastModifiedBy());
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
  public void findAppsShouldRejectUserTokenWhenRequestedAppIsOutsideScope() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(unifiedPermissionValidator.hasReadApplicationPermission("app1")).thenReturn(true);
    when(unifiedPermissionValidator.hasReadApplicationPermission("secret-app")).thenReturn(false);

    this.mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/apps").param("appIds", "app1,secret-app"))
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    verify(appOpenApiService, never()).getAppsInfo(anyList());
  }

  @Test
  public void testGetEnvClusters() throws Exception {
    String appId = "someAppId";

    OpenEnvClusterDTO devCluster = new OpenEnvClusterDTO();
    devCluster.setEnv("DEV");
    devCluster.setClusters(Lists.newArrayList("default"));
    OpenEnvClusterDTO fatCluster = new OpenEnvClusterDTO();
    fatCluster.setEnv("FAT");
    fatCluster.setClusters(Lists.newArrayList("default", "feature"));

    when(appOpenApiService.getEnvClusters(appId))
        .thenReturn(Lists.newArrayList(devCluster, fatCluster));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId + "/envclusters"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].env").value("DEV"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].clusters[0]").value("default"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].env").value("FAT"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].clusters[0]").value("default"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].clusters[1]").value("feature"));

    Mockito.verify(appOpenApiService).getEnvClusters(appId);
  }

  @Test
  public void testGetEnvClusterInfo() throws Exception {
    String appId = "someAppId";

    OpenEnvClusterInfo devInfo = new OpenEnvClusterInfo();
    devInfo.setEnv("DEV");
    devInfo.setCode(HttpStatus.OK.value());
    devInfo.setMessage(HttpStatus.OK.getReasonPhrase());
    com.ctrip.framework.apollo.openapi.model.OpenClusterDTO defaultCluster =
        new com.ctrip.framework.apollo.openapi.model.OpenClusterDTO();
    defaultCluster.setName("default");
    defaultCluster.setAppId(appId);
    devInfo.setClusters(Lists.newArrayList(defaultCluster));

    when(appOpenApiService.getEnvClusterInfo(appId)).thenReturn(Lists.newArrayList(devInfo));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId + "/env-cluster-info"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].env").value("DEV"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].code").value(HttpStatus.OK.value()))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.[0].message").value(HttpStatus.OK.getReasonPhrase()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].clusters[0].name").value("default"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].clusters[0].appId").value(appId));

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
    when(unifiedPermissionValidator.hasReadApplicationPermission(Mockito.anyString()))
        .thenReturn(false);

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value("app1"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[1].appId").value("app2"));

    Mockito.verify(appOpenApiService).getAllApps();
    Mockito.verify(unifiedPermissionValidator, Mockito.never())
        .hasReadApplicationPermission(Mockito.anyString());
  }

  @Test
  public void testFindAllAppsFiltersUserTokenScope() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    OpenAppDTO app1 = new OpenAppDTO();
    app1.setAppId("app1");
    OpenAppDTO app2 = new OpenAppDTO();
    app2.setAppId("app2");
    when(appOpenApiService.getAllApps()).thenReturn(Lists.newArrayList(app1, app2));
    when(unifiedPermissionValidator.hasReadApplicationPermission(Mockito.anyString()))
        .thenReturn(false);
    when(unifiedPermissionValidator.hasReadApplicationPermission("app1")).thenReturn(true);

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value("app1"));

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
  public void testGetAppsBySelfForPortalUser() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    int page = 0;
    int size = 10;
    String app1Id = "app1";
    String app2Id = "app2";
    Set<String> authorizedAppIds = Sets.newHashSet(app1Id, app2Id);

    UserInfo loginUser = new UserInfo();
    loginUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(loginUser);

    Role masterRole = new Role();
    masterRole.setRoleName(RoleUtils.buildAppMasterRoleName(app1Id));
    Role namespaceRole = new Role();
    namespaceRole.setRoleName(RoleUtils.buildModifyNamespaceRoleName(app2Id, "application"));
    Role systemRole = new Role();
    systemRole.setRoleName("CreateApplication+System");
    when(rolePermissionService.findUserRoles(loginUser.getUserId()))
        .thenReturn(Lists.newArrayList(masterRole, namespaceRole, systemRole));

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

    Mockito.verify(rolePermissionService).findUserRoles(loginUser.getUserId());
    Mockito.verify(appOpenApiService).getAppsBySelf(authorizedAppIds, page, size);
    Mockito.verify(consumerAuthUtil, never()).retrieveConsumerIdFromCtx();
  }

  @Test
  public void testGetAppsBySelfForPortalUserWithoutLoginUser() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    int page = 0;
    int size = 10;
    when(userInfoHolder.getUser()).thenReturn(null);
    when(appOpenApiService.getAppsBySelf(Collections.emptySet(), page, size))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/apps/by-self")
            .param("page", String.valueOf(page)).param("size", String.valueOf(size)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().json("[]"));

    Mockito.verify(rolePermissionService, never()).findUserRoles(anyString());
    Mockito.verify(appOpenApiService).getAppsBySelf(Collections.emptySet(), page, size);
    Mockito.verify(consumerAuthUtil, never()).retrieveConsumerIdFromCtx();
  }

  @Test
  public void testGetAppsBySelfForPortalUserWithoutRoles() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    int page = 0;
    int size = 10;
    UserInfo loginUser = new UserInfo();
    loginUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(loginUser);
    when(rolePermissionService.findUserRoles(loginUser.getUserId())).thenReturn(null);
    when(appOpenApiService.getAppsBySelf(Collections.emptySet(), page, size))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/apps/by-self")
            .param("page", String.valueOf(page)).param("size", String.valueOf(size)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().json("[]"));

    Mockito.verify(rolePermissionService).findUserRoles(loginUser.getUserId());
    Mockito.verify(appOpenApiService).getAppsBySelf(Collections.emptySet(), page, size);
    Mockito.verify(consumerAuthUtil, never()).retrieveConsumerIdFromCtx();
  }

  @Test
  public void testFindAppsAuthorizedForUserTokenUsesReadableApps() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    OpenAppDTO app1 = new OpenAppDTO();
    app1.setAppId("app1");
    OpenAppDTO app2 = new OpenAppDTO();
    app2.setAppId("app2");
    when(appOpenApiService.getAllApps()).thenReturn(Lists.newArrayList(app1, app2));
    when(unifiedPermissionValidator.hasReadApplicationPermission(Mockito.anyString()))
        .thenReturn(false);
    when(unifiedPermissionValidator.hasReadApplicationPermission("app1")).thenReturn(true);

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/authorized"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value("app1"));

    Mockito.verify(appOpenApiService).getAllApps();
    Mockito.verify(consumerAuthUtil, never()).retrieveConsumerIdFromCtx();
    Mockito.verify(appOpenApiService, never()).getAppsInfo(Mockito.anyList());
  }

  @Test
  public void testGetAppsBySelfForUserTokenUsesReadableApps() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    OpenAppDTO app1 = new OpenAppDTO();
    app1.setAppId("app1");
    OpenAppDTO app2 = new OpenAppDTO();
    app2.setAppId("app2");
    OpenAppDTO app3 = new OpenAppDTO();
    app3.setAppId("app3");
    when(appOpenApiService.getAllApps()).thenReturn(Lists.newArrayList(app1, app2, app3));
    when(unifiedPermissionValidator.hasReadApplicationPermission(Mockito.anyString()))
        .thenReturn(false);
    when(unifiedPermissionValidator.hasReadApplicationPermission("app1")).thenReturn(true);
    when(unifiedPermissionValidator.hasReadApplicationPermission("app2")).thenReturn(true);

    mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/apps/by-self").param("page", "0")
            .param("size", "1"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].appId").value("app1"));

    Mockito.verify(appOpenApiService).getAllApps();
    Mockito.verify(consumerAuthUtil, never()).retrieveConsumerIdFromCtx();
    Mockito.verify(appOpenApiService, never()).getAppsBySelf(Mockito.anySet(), Mockito.anyInt(),
        Mockito.anyInt());
  }

  @Test
  public void testFindMissEnvs() throws Exception {
    String appId = "someAppId";

    OpenMissEnvDTO missEnv = new OpenMissEnvDTO();
    missEnv.setCode(HttpStatus.OK.value());
    missEnv.setMessage("FAT");
    when(appOpenApiService.findMissEnvs(appId)).thenReturn(Lists.newArrayList(missEnv));

    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/" + appId + "/miss-envs"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].code").value(HttpStatus.OK.value()))
        .andExpect(MockMvcResultMatchers.jsonPath("$.[0].message").value("FAT"));

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

    Mockito.doNothing().when(appOpenApiService).updateApp(Mockito.any(OpenAppDTO.class),
        Mockito.eq(operator));
    when(unifiedPermissionValidator.isAppAdmin(appId)).thenReturn(true);

    mockMvc
        .perform(MockMvcRequestBuilders.put("/openapi/v1/apps/" + appId).param("operator", operator)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(requestDto)))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(""));

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

    when(unifiedPermissionValidator.isAppAdmin(pathAppId)).thenReturn(true);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/openapi/v1/apps/" + pathAppId).param("operator", operator)
                .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(requestDto)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());

    Mockito.verify(appOpenApiService, Mockito.never()).updateApp(Mockito.any(),
        Mockito.anyString());
  }

  @Test
  public void testDeleteApp() throws Exception {
    String appId = "app1";
    String operator = "deleter";

    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("test");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(userInfo, null, Collections.emptyList()));

    when(appOpenApiService.deleteApp(appId, operator)).thenReturn(new OpenAppDTO());

    mockMvc.perform(delete("/openapi/v1/apps/" + appId).param("operator", operator))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(""));

    Mockito.verify(appOpenApiService).deleteApp(appId, operator);
  }
}
