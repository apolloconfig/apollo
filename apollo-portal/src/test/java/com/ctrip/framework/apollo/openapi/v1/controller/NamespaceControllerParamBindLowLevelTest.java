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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.server.service.NamespaceOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Low-level MockMvc tests for NamespaceController generated-route binding and identity handling.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class NamespaceControllerParamBindLowLevelTest {

  private static final String APP_ID = "app-1";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserInfoHolder userInfoHolder;

  @MockitoBean
  private NamespaceOpenApiService namespaceOpenApiService;

  private final Gson gson = new Gson();

  @BeforeEach
  public void setUp() {
    when(unifiedPermissionValidator.hasCreateNamespacePermission(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.hasDeleteNamespacePermission(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.hasCreateAppNamespacePermission(anyString(), any()))
        .thenReturn(true);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(anyString(), anyString(),
        anyString(), anyString())).thenReturn(false);
    when(unifiedPermissionValidator.isAppAdmin(anyString())).thenReturn(true);

    UserInfo user = new UserInfo();
    user.setUserId("tester");
    when(userService.findByUserId(anyString())).thenReturn(user);
    when(userInfoHolder.getUser()).thenReturn(user);

    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("tester", "N/A", AuthorityUtils.NO_AUTHORITIES));
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  @Test
  public void createAppNamespaceShouldUseCurrentPortalUserAndIgnoreSpoofedPayloadOperator()
      throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    UserInfo portalUser = new UserInfo();
    portalUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(portalUser);

    OpenAppNamespaceDTO request = new OpenAppNamespaceDTO();
    request.setAppId(APP_ID);
    request.setName("demo");
    request.setFormat("properties");
    request.setDataChangeCreatedBy("spoofed-user");
    request.setIsPublic(false);
    when(namespaceOpenApiService.createAppNamespace(anyString(), any(OpenAppNamespaceDTO.class),
        anyString())).thenReturn(request);

    mockMvc
        .perform(post("/openapi/v1/apps/{appId}/appnamespaces", APP_ID)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("demo"));

    ArgumentCaptor<OpenAppNamespaceDTO> dtoCaptor =
        ArgumentCaptor.forClass(OpenAppNamespaceDTO.class);
    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);
    verify(namespaceOpenApiService).createAppNamespace(anyString(), dtoCaptor.capture(),
        operatorCaptor.capture());
    assertThat(dtoCaptor.getValue().getDataChangeCreatedBy()).isEqualTo("portal-user");
    assertThat(dtoCaptor.getValue().getDataChangeLastModifiedBy()).isEqualTo("portal-user");
    assertThat(operatorCaptor.getValue()).isEqualTo("portal-user");
  }

  @Test
  public void createAppNamespaceShouldUseAppNamespacePermissionForPortalUser() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.hasCreateNamespacePermission(APP_ID)).thenReturn(false);
    when(unifiedPermissionValidator.hasCreateAppNamespacePermission(anyString(), any()))
        .thenReturn(true);

    UserInfo portalUser = new UserInfo();
    portalUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(portalUser);

    OpenAppNamespaceDTO request = new OpenAppNamespaceDTO();
    request.setAppId(APP_ID);
    request.setName("private-namespace");
    request.setFormat("properties");
    request.setIsPublic(false);
    when(namespaceOpenApiService.createAppNamespace(anyString(), any(OpenAppNamespaceDTO.class),
        anyString())).thenReturn(request);

    mockMvc
        .perform(post("/openapi/v1/apps/{appId}/appnamespaces", APP_ID)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(request)))
        .andExpect(status().isOk());

    verify(unifiedPermissionValidator).hasCreateAppNamespacePermission(anyString(), any());
    verify(namespaceOpenApiService).createAppNamespace(anyString(), any(OpenAppNamespaceDTO.class),
        anyString());
  }

  @Test
  public void createAppNamespaceShouldUseCreateNamespacePermissionForConsumer() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasCreateAppNamespacePermission(anyString(), any()))
        .thenThrow(new UnsupportedOperationException("consumer app namespace permission"));

    OpenAppNamespaceDTO request = new OpenAppNamespaceDTO();
    request.setAppId(APP_ID);
    request.setName("private-namespace");
    request.setFormat("properties");
    request.setIsPublic(false);
    request.setDataChangeCreatedBy("operator");
    when(namespaceOpenApiService.createAppNamespace(anyString(), any(OpenAppNamespaceDTO.class),
        anyString())).thenReturn(request);

    mockMvc
        .perform(post("/openapi/v1/apps/{appId}/appnamespaces", APP_ID)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(request)))
        .andExpect(status().isOk());

    verify(unifiedPermissionValidator).hasCreateNamespacePermission(APP_ID);
    verify(unifiedPermissionValidator, never()).hasCreateAppNamespacePermission(anyString(), any());
  }

  @Test
  public void createAppNamespaceShouldRejectBlankConsumerOperator() throws Exception {
    OpenAppNamespaceDTO request = new OpenAppNamespaceDTO();
    request.setAppId(APP_ID);
    request.setName("demo");
    request.setFormat("properties");

    mockMvc
        .perform(post("/openapi/v1/apps/{appId}/appnamespaces", APP_ID)
            .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(request)))
        .andExpect(status().isBadRequest());

    verify(namespaceOpenApiService, never()).createAppNamespace(anyString(),
        any(OpenAppNamespaceDTO.class), anyString());
  }

  @Test
  public void createNamespacesShouldUseCurrentPortalUser() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    UserInfo portalUser = new UserInfo();
    portalUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(portalUser);

    OpenCreateNamespaceDTO request = new OpenCreateNamespaceDTO();
    request.setAppId(APP_ID);
    request.setEnv(ENV);
    request.setClusterName(CLUSTER);
    request.setAppNamespaceName(NAMESPACE);

    mockMvc.perform(post("/openapi/v1/namespaces").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(Collections.singletonList(request)))).andExpect(status().isOk());

    ArgumentCaptor<List<OpenCreateNamespaceDTO>> namespacesCaptor =
        ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);
    verify(namespaceOpenApiService).createNamespaces(namespacesCaptor.capture(),
        operatorCaptor.capture());
    assertThat(namespacesCaptor.getValue()).hasSize(1);
    assertThat(operatorCaptor.getValue()).isEqualTo("portal-user");
  }

  @Test
  public void createNamespacesShouldRejectAnyAppWithoutCreatePermission() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.hasCreateNamespacePermission(APP_ID)).thenReturn(true);
    when(unifiedPermissionValidator.hasCreateNamespacePermission("other-app")).thenReturn(false);

    OpenCreateNamespaceDTO permitted = new OpenCreateNamespaceDTO();
    permitted.setAppId(APP_ID);
    permitted.setEnv(ENV);
    permitted.setClusterName(CLUSTER);
    permitted.setAppNamespaceName(NAMESPACE);

    OpenCreateNamespaceDTO forbidden = new OpenCreateNamespaceDTO();
    forbidden.setAppId("other-app");
    forbidden.setEnv(ENV);
    forbidden.setClusterName(CLUSTER);
    forbidden.setAppNamespaceName(NAMESPACE);

    mockMvc
        .perform(post("/openapi/v1/namespaces").contentType(MediaType.APPLICATION_JSON)
            .content(gson.toJson(Arrays.asList(permitted, forbidden))))
        .andExpect(status().isForbidden());

    verify(namespaceOpenApiService, never()).createNamespaces(any(), anyString());
  }

  @Test
  public void namespaceCreateOperationsShouldKeepAuditAnnotations() throws Exception {
    Method createAppNamespace = NamespaceController.class.getMethod("createAppNamespace",
        String.class, OpenAppNamespaceDTO.class, String.class);
    ApolloAuditLog createAppNamespaceAudit = createAppNamespace.getAnnotation(ApolloAuditLog.class);
    assertThat(createAppNamespaceAudit).isNotNull();
    assertThat(createAppNamespaceAudit.type()).isEqualTo(OpType.CREATE);
    assertThat(createAppNamespaceAudit.name()).isEqualTo("AppNamespace.create");

    Method createNamespaces =
        NamespaceController.class.getMethod("createNamespaces", List.class, String.class);
    ApolloAuditLog createNamespacesAudit = createNamespaces.getAnnotation(ApolloAuditLog.class);
    assertThat(createNamespacesAudit).isNotNull();
    assertThat(createNamespacesAudit.type()).isEqualTo(OpType.CREATE);
    assertThat(createNamespacesAudit.name()).isEqualTo("Namespace.create");
  }

  @Test
  public void deleteNamespaceShouldRejectBlankConsumerOperator() throws Exception {
    mockMvc.perform(delete(
        "/openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}",
        APP_ID, ENV, CLUSTER, NAMESPACE)).andExpect(status().isBadRequest());

    verify(namespaceOpenApiService, never()).deleteNamespace(anyString(), anyString(), anyString(),
        anyString(), anyString());
  }

  @Test
  public void findNamespaceShouldBindFillItemDetailAndExtendInfo() throws Exception {
    OpenNamespaceDTO namespace = new OpenNamespaceDTO();
    namespace.setAppId(APP_ID);
    namespace.setNamespaceName(NAMESPACE);
    when(namespaceOpenApiService.findNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, false, true))
        .thenReturn(namespace);

    mockMvc
        .perform(get(
            "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}",
            ENV, APP_ID, CLUSTER, NAMESPACE).param("fillItemDetail", "false")
            .param("extendInfo", "true"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.namespaceName").value(NAMESPACE));

    verify(namespaceOpenApiService).findNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, false, true);
  }

  @Test
  public void namespaceLockShouldDelegateToGeneratedService() throws Exception {
    OpenNamespaceLockDTO lock = new OpenNamespaceLockDTO();
    lock.setNamespaceName(NAMESPACE);
    lock.setIsLocked(true);
    lock.setLockedBy("operator");
    lock.setIsEmergencyPublishAllowed(true);
    when(namespaceOpenApiService.getNamespaceLock(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(lock);

    mockMvc.perform(get(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock",
        ENV, APP_ID, CLUSTER, NAMESPACE)).andExpect(status().isOk())
        .andExpect(jsonPath("$.lockedBy").value("operator"))
        .andExpect(jsonPath("$.isEmergencyPublishAllowed").value(true));

    verify(namespaceOpenApiService).getNamespaceLock(APP_ID, ENV, CLUSTER, NAMESPACE);
  }

  @Test
  public void missingNamespacesShouldReturnPlainStringList() throws Exception {
    when(namespaceOpenApiService.findMissingNamespaces(APP_ID, ENV, CLUSTER))
        .thenReturn(List.of("application", "FX.apollo"));

    mockMvc.perform(get(
        "/openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/missing-namespaces",
        APP_ID, ENV, CLUSTER)).andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("application"))
        .andExpect(jsonPath("$[1]").value("FX.apollo"));

    verify(namespaceOpenApiService).findMissingNamespaces(APP_ID, ENV, CLUSTER);
  }
}
