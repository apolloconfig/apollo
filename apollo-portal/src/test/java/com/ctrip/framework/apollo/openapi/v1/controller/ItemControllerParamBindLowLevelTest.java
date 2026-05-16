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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.openapi.model.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceIdentifier;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceSyncDTO;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
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
 * Low-level MockMvc tests for ItemController parameter binding and identity handling.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class ItemControllerParamBindLowLevelTest {

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
  private ItemOpenApiService itemOpenApiService;

  private final Gson gson = new Gson();

  @BeforeEach
  public void setUp() {
    when(unifiedPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(),
        anyString(), anyString())).thenReturn(true);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(anyString(), anyString(),
        anyString(), anyString())).thenReturn(false);

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
  public void createItemShouldUsePayloadOperatorForConsumerWhenQueryOperatorMissing()
      throws Exception {
    OpenItemDTO request = new OpenItemDTO();
    request.setKey("timeout");
    request.setValue("100");
    request.setDataChangeCreatedBy("api-operator");

    OpenItemDTO response = new OpenItemDTO();
    response.setKey("timeout");
    response.setValue("100");
    when(itemOpenApiService.createItem(anyString(), anyString(), anyString(), anyString(),
        any(OpenItemDTO.class), anyString())).thenReturn(response);

    mockMvc.perform(post(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items",
        ENV, APP_ID, CLUSTER, NAMESPACE).contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value("timeout"));

    ArgumentCaptor<OpenItemDTO> itemCaptor = ArgumentCaptor.forClass(OpenItemDTO.class);
    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);
    verify(itemOpenApiService).createItem(anyString(), anyString(), anyString(), anyString(),
        itemCaptor.capture(), operatorCaptor.capture());
    assertThat(itemCaptor.getValue().getDataChangeCreatedBy()).isEqualTo("api-operator");
    assertThat(operatorCaptor.getValue()).isEqualTo("api-operator");
  }

  @Test
  public void createItemShouldUseCurrentPortalUserAndIgnoreSpoofedPayloadOperator()
      throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    UserInfo portalUser = new UserInfo();
    portalUser.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(portalUser);

    OpenItemDTO request = new OpenItemDTO();
    request.setKey("timeout");
    request.setValue("100");
    request.setDataChangeCreatedBy("spoofed-user");

    OpenItemDTO response = new OpenItemDTO();
    response.setKey("timeout");
    when(itemOpenApiService.createItem(anyString(), anyString(), anyString(), anyString(),
        any(OpenItemDTO.class), anyString())).thenReturn(response);

    mockMvc.perform(post(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items",
        ENV, APP_ID, CLUSTER, NAMESPACE).contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(request))).andExpect(status().isOk());

    ArgumentCaptor<OpenItemDTO> itemCaptor = ArgumentCaptor.forClass(OpenItemDTO.class);
    ArgumentCaptor<String> operatorCaptor = ArgumentCaptor.forClass(String.class);
    verify(itemOpenApiService).createItem(anyString(), anyString(), anyString(), anyString(),
        itemCaptor.capture(), operatorCaptor.capture());
    assertThat(itemCaptor.getValue().getDataChangeCreatedBy()).isEqualTo("portal-user");
    assertThat(itemCaptor.getValue().getDataChangeLastModifiedBy()).isEqualTo("portal-user");
    assertThat(operatorCaptor.getValue()).isEqualTo("portal-user");
  }

  @Test
  public void updateItemShouldRejectPathPayloadKeyMismatch() throws Exception {
    OpenItemDTO request = new OpenItemDTO();
    request.setKey("other-key");
    request.setValue("100");
    request.setDataChangeLastModifiedBy("api-operator");

    mockMvc.perform(put(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}",
        ENV, APP_ID, CLUSTER, NAMESPACE, "timeout").contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(request))).andExpect(status().isBadRequest());

    verify(itemOpenApiService, never()).updateItem(anyString(), anyString(), anyString(),
        anyString(), any(OpenItemDTO.class), anyString());
  }

  @Test
  public void deleteItemShouldRejectBlankConsumerOperator() throws Exception {
    mockMvc.perform(delete(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}",
        ENV, APP_ID, CLUSTER, NAMESPACE, "timeout")).andExpect(status().isBadRequest());

    verify(itemOpenApiService, never()).removeItem(anyString(), anyString(), anyString(),
        anyString(), anyString(), anyString());
  }

  @Test
  public void findItemsShouldReturnEmptyPageForHiddenPortalUserConfig() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);

    mockMvc.perform(get(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items",
        ENV, APP_ID, CLUSTER, NAMESPACE).param("page", "0").param("size", "50"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(50)).andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.content.length()").value(0));

    verify(itemOpenApiService, never()).findItemsByNamespace(anyString(), anyString(), anyString(),
        anyString(), any(Integer.class), any(Integer.class));
  }

  @Test
  public void getItemShouldReturnEmptyBodyForHiddenPortalUserConfig() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);

    mockMvc.perform(get(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}",
        ENV, APP_ID, CLUSTER, NAMESPACE, "timeout")).andExpect(status().isOk())
        .andExpect(content().string(""));

    verify(itemOpenApiService, never()).getItem(anyString(), anyString(), anyString(), anyString(),
        anyString());
  }

  @Test
  public void getItemByEncodedKeyShouldDecodeBase64KeyBeforeCallingService() throws Exception {
    String key = "feature.flag";
    String encodedKey = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    OpenItemDTO response = new OpenItemDTO();
    response.setKey(key);
    when(itemOpenApiService.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, key)).thenReturn(response);

    mockMvc.perform(get(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key}",
        ENV, APP_ID, CLUSTER, NAMESPACE, encodedKey)).andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value(key));

    verify(itemOpenApiService).getItem(APP_ID, ENV, CLUSTER, NAMESPACE, key);
  }

  @Test
  public void getItemByEncodedKeyShouldDecodeUrlSafeBase64KeyBeforeCallingService()
      throws Exception {
    String key = "k'?";
    String encodedKey = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(key.getBytes(StandardCharsets.UTF_8));
    OpenItemDTO response = new OpenItemDTO();
    response.setKey(key);
    when(itemOpenApiService.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, key)).thenReturn(response);

    mockMvc.perform(get(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key}",
        ENV, APP_ID, CLUSTER, NAMESPACE, encodedKey)).andExpect(status().isOk())
        .andExpect(jsonPath("$.key").value(key));

    verify(itemOpenApiService).getItem(APP_ID, ENV, CLUSTER, NAMESPACE, key);
  }

  @Test
  public void getItemByEncodedKeyShouldRejectMalformedBase64Key() throws Exception {
    mockMvc.perform(get(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key}",
        ENV, APP_ID, CLUSTER, NAMESPACE, "not-base64!")).andExpect(status().isBadRequest());

    verify(itemOpenApiService, never()).getItem(anyString(), anyString(), anyString(), anyString(),
        anyString());
  }

  @Test
  public void findItemsShouldDelegateForConsumerEvenWhenConfigWouldBeHiddenForUser()
      throws Exception {
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER,
        NAMESPACE)).thenReturn(true);
    OpenItemPageDTO page = new OpenItemPageDTO();
    page.setPage(0);
    page.setSize(50);
    page.setTotal(0L);
    page.setContent(Collections.emptyList());
    when(itemOpenApiService.findItemsByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, 0, 50))
        .thenReturn(page);

    mockMvc.perform(get(
            "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items",
            ENV, APP_ID, CLUSTER, NAMESPACE).param("page", "0").param("size", "50"))
        .andExpect(status().isOk());

    verify(itemOpenApiService).findItemsByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, 0, 50);
  }

  @Test
  public void compareItemsShouldAcceptEmptySyncItems() throws Exception {
    OpenNamespaceIdentifier namespaceIdentifier = new OpenNamespaceIdentifier();
    namespaceIdentifier.setAppId(APP_ID);
    namespaceIdentifier.setEnv(ENV);
    namespaceIdentifier.setClusterName(CLUSTER);
    namespaceIdentifier.setNamespaceName(NAMESPACE);
    OpenNamespaceSyncDTO request = new OpenNamespaceSyncDTO();
    request.setSyncToNamespaces(Collections.singletonList(namespaceIdentifier));
    request.setSyncItems(Collections.emptyList());
    when(itemOpenApiService.compareItems(anyString(), anyString(), anyString(), anyString(),
        any(OpenNamespaceSyncDTO.class))).thenReturn(Collections.emptyList());

    mockMvc.perform(post(
        "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/diff",
        ENV, APP_ID, CLUSTER, NAMESPACE).contentType(MediaType.APPLICATION_JSON)
        .content(gson.toJson(request))).andExpect(status().isOk());

    ArgumentCaptor<OpenNamespaceSyncDTO> requestCaptor =
        ArgumentCaptor.forClass(OpenNamespaceSyncDTO.class);
    verify(itemOpenApiService).compareItems(anyString(), anyString(), anyString(), anyString(),
        requestCaptor.capture());
    assertThat(requestCaptor.getValue().getSyncItems()).isEmpty();
  }
}
