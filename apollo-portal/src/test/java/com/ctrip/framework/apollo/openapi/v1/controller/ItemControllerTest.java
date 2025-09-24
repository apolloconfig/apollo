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

import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.*;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ItemController.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean(name = "consumerPermissionValidator")
    private ConsumerPermissionValidator consumerPermissionValidator;

    @MockBean
    private ItemOpenApiService itemOpenApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetItem() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String key = "some-key";

        when(itemOpenApiService.getItem(appId, env, clusterName, namespaceName, key)).thenReturn(new OpenItemDTO());

        mockMvc.perform(MockMvcRequestBuilders.get(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items/%s", appId, env, clusterName, namespaceName, key)))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetItems() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";

        OpenPageDTOOpenItemDTO pageDTO = new OpenPageDTOOpenItemDTO();
        pageDTO.setContent(Collections.emptyList());

        when(itemOpenApiService.findItemsByNamespace(appId, env, clusterName, namespaceName, 0, 50)).thenReturn(pageDTO);

        mockMvc.perform(MockMvcRequestBuilders.get(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items", appId, env, clusterName, namespaceName)))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetItemByEncodedKey() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String key = "some-key";
        String encodedKey = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));

        when(itemOpenApiService.getItem(appId, env, clusterName, namespaceName, key)).thenReturn(new OpenItemDTO());

        mockMvc.perform(MockMvcRequestBuilders.get(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/encodedItems/%s", appId, env, clusterName, namespaceName, encodedKey)))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateItem() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String operator = "apollo";

        OpenItemDTO itemDTO = new OpenItemDTO();
        itemDTO.setKey("some-key");
        itemDTO.setValue("some-value");
        itemDTO.setDataChangeCreatedBy(operator);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(operator);
        when(userService.findByUserId(operator)).thenReturn(userInfo);
        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(itemOpenApiService.createItem(eq(appId), eq(env), eq(clusterName), eq(namespaceName), any(OpenItemDTO.class))).thenReturn(itemDTO);

        mockMvc.perform(MockMvcRequestBuilders.post(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items", appId, env, clusterName, namespaceName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateItem() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String key = "some-key";
        String operator = "apollo";

        OpenItemDTO itemDTO = new OpenItemDTO();
        itemDTO.setKey(key);
        itemDTO.setValue("new-value");
        itemDTO.setDataChangeLastModifiedBy(operator);
        itemDTO.setType(0);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(operator);
        when(userService.findByUserId(operator)).thenReturn(userInfo);
        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(itemOpenApiService).updateItem(eq(appId), eq(env), eq(clusterName), eq(namespaceName), any(OpenItemDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.put(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items/%s", appId, env, clusterName, namespaceName, key))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testUpdateItemByEncodedKey() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String key = "some-key";
        String encodedKey = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        String operator = "apollo";

        OpenItemDTO itemDTO = new OpenItemDTO();
        itemDTO.setKey(key);
        itemDTO.setValue("new-value");
        itemDTO.setDataChangeLastModifiedBy(operator);
        itemDTO.setType(0);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(operator);
        when(userService.findByUserId(operator)).thenReturn(userInfo);
        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(itemOpenApiService).updateItem(eq(appId), eq(env), eq(clusterName), eq(namespaceName), any(OpenItemDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.put(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/encodedItems/%s", appId, env, clusterName, namespaceName, encodedKey))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteItem() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String key = "some-key";
        String operator = "apollo";

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(operator);
        when(userService.findByUserId(operator)).thenReturn(userInfo);
        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(itemOpenApiService.loadItem(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(new OpenItemDTO());
        doNothing().when(itemOpenApiService).removeItem(eq(appId), eq(env), eq(clusterName), eq(namespaceName), eq(key));

        mockMvc.perform(MockMvcRequestBuilders.delete(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items/%s", appId, env, clusterName, namespaceName, key))
                .param("operator", operator))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteItemByEncodedKey() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String key = "some-key";
        String encodedKey = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        String operator = "apollo";

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(operator);
        when(userService.findByUserId(operator)).thenReturn(userInfo);
        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(itemOpenApiService.loadItem(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(new OpenItemDTO());
        doNothing().when(itemOpenApiService).removeItem(eq(appId), eq(env), eq(clusterName), eq(namespaceName), eq(key));

        mockMvc.perform(MockMvcRequestBuilders.delete(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/encodedItems/%s", appId, env, clusterName, namespaceName, encodedKey))
                .param("operator", operator))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBatchUpdateItemsByText() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String operator = "apollo";

        OpenNamespaceTextModel model = new OpenNamespaceTextModel();
        model.setOperator(operator);
        model.setFormat("properties");
        model.setConfigText("key1=val1");

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(operator);
        when(userService.findByUserId(operator)).thenReturn(userInfo);
        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(itemOpenApiService).modifyItemsByText(eq(appId), eq(env), eq(clusterName), eq(namespaceName), any(OpenNamespaceTextModel.class));

        mockMvc.perform(MockMvcRequestBuilders.put(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items:batchUpdate", appId, env, clusterName, namespaceName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGetBranchItems() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";
        String branchName = "some-branch";

        when(itemOpenApiService.findBranchItems(appId, env, clusterName, namespaceName, branchName)).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/branches/%s/items", appId, env, clusterName, namespaceName, branchName)))
                .andExpect(status().isOk());
    }

    @Test
    public void testCompareItems() throws Exception {
        OpenNamespaceSyncModel model = new OpenNamespaceSyncModel();
        OpenItemDTO item = new OpenItemDTO();
        item.setKey("k");
        item.setValue("v");
        model.setSyncItems(Collections.singletonList(item));
        OpenNamespaceIdentifier namespaceIdentifier = new OpenNamespaceIdentifier();
        namespaceIdentifier.setAppId("appId");
        namespaceIdentifier.setEnv("dev");
        namespaceIdentifier.setClusterName("default");
        namespaceIdentifier.setNamespaceName("application");
        model.setSyncToNamespaces(Collections.singletonList(namespaceIdentifier));

        OpenItemDiffs itemDiffs = new OpenItemDiffs();
        itemDiffs.setDiffs(new OpenItemChangeSets());

        when(itemOpenApiService.diff(any(OpenNamespaceSyncModel.class))).thenReturn(Collections.singletonList(itemDiffs));

        mockMvc.perform(MockMvcRequestBuilders.post("/openapi/v1/namespaces/items:compare")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isOk());
    }

    @Test
    public void testSyncItems() throws Exception {
        String appId = "appId";
        String namespaceName = "application";

        OpenNamespaceSyncModel model = new OpenNamespaceSyncModel();
        OpenItemDTO item = new OpenItemDTO();
        item.setKey("k");
        item.setValue("v");
        model.setSyncItems(Collections.singletonList(item));
        OpenNamespaceIdentifier namespaceIdentifier = new OpenNamespaceIdentifier();
        namespaceIdentifier.setAppId(appId);
        namespaceIdentifier.setEnv("dev");
        namespaceIdentifier.setClusterName("default");
        namespaceIdentifier.setNamespaceName(namespaceName);
        model.setSyncToNamespaces(Collections.singletonList(namespaceIdentifier));

        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(itemOpenApiService).syncItems(any(OpenNamespaceSyncModel.class));

        mockMvc.perform(MockMvcRequestBuilders.post(
                String.format("/openapi/v1/apps/%s/namespaces/%s/items:sync", appId, namespaceName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testValidateItems() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";

        OpenNamespaceTextModel model = new OpenNamespaceTextModel();
        model.setFormat("properties");
        model.setConfigText("key1=val1");

        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(itemOpenApiService).syntaxCheckText(any(OpenNamespaceTextModel.class));

        mockMvc.perform(MockMvcRequestBuilders.post(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items:validate", appId, env, clusterName, namespaceName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(model)))
                .andExpect(status().isOk());
    }

    @Test
    public void testRevertItems() throws Exception {
        String appId = "appId";
        String env = "dev";
        String clusterName = "default";
        String namespaceName = "application";

        when(consumerPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        doNothing().when(itemOpenApiService).revokeItems(appId, env, clusterName, namespaceName);

        mockMvc.perform(MockMvcRequestBuilders.post(
                String.format("/openapi/v1/apps/%s/env/%s/clusters/%s/namespaces/%s/items:revert", appId, env, clusterName, namespaceName)))
                .andExpect(status().isAccepted());
    }
}

