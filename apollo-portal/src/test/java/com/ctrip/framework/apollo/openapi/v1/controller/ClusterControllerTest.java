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
import com.ctrip.framework.apollo.openapi.model.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.server.service.ClusterOpenApiService;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ClusterController.class)
public class ClusterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ClusterOpenApiService clusterOpenApiService;

    @MockBean(name = "consumerPermissionValidator")
    private ConsumerPermissionValidator consumerPermissionValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetCluster() throws Exception {
        String appId = "test-app";
        String env = "DEV";
        String clusterName = "default";
        OpenClusterDTO clusterDTO = new OpenClusterDTO();
        clusterDTO.setAppId(appId);
        clusterDTO.setName(clusterName);

        when(clusterOpenApiService.getCluster(appId, env, clusterName)).thenReturn(clusterDTO);

        this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId, clusterName))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId", is(appId)))
                .andExpect(jsonPath("$.name", is(clusterName)));
    }

    @Test
    public void testCreateCluster() throws Exception {
        String appId = "test-app";
        String env = "DEV";
        String clusterName = "new-cluster";
        String operator = "apollo";

        OpenClusterDTO clusterDTO = new OpenClusterDTO();
        clusterDTO.setAppId(appId);
        clusterDTO.setName(clusterName);
        clusterDTO.setDataChangeCreatedBy(operator);

        UserInfo user = new UserInfo();
        user.setUserId(operator);

        when(consumerPermissionValidator.hasCreateClusterPermission(appId)).thenReturn(true);
        when(userService.findByUserId(operator)).thenReturn(user);
        when(clusterOpenApiService.createCluster(eq(env), any(OpenClusterDTO.class))).thenReturn(clusterDTO);

        this.mockMvc.perform(
                MockMvcRequestBuilders.post("/openapi/v1/envs/{env}/apps/{appId}/clusters", env, appId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clusterDTO))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId", is(appId)))
                .andExpect(jsonPath("$.name", is(clusterName)));
    }

    @Test
    public void testCreateClusterWithAppIdMismatch() throws Exception {
        String appIdInPath = "app-in-path";
        String appIdInBody = "app-in-body";
        String env = "DEV";
        String clusterName = "new-cluster";
        String operator = "apollo";

        OpenClusterDTO clusterDTO = new OpenClusterDTO();
        clusterDTO.setAppId(appIdInBody);
        clusterDTO.setName(clusterName);
        clusterDTO.setDataChangeCreatedBy(operator);

        when(consumerPermissionValidator.hasCreateClusterPermission(appIdInPath)).thenReturn(true);

        this.mockMvc.perform(
                MockMvcRequestBuilders.post("/openapi/v1/envs/{env}/apps/{appId}/clusters", env, appIdInPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clusterDTO))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteCluster() throws Exception {
        String appId = "test-app";
        String env = "DEV";
        String clusterName = "default";

        when(consumerPermissionValidator.isAppAdmin(appId)).thenReturn(true);
        Mockito.doNothing().when(clusterOpenApiService).deleteCluster(env, appId, clusterName);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}", env, appId, clusterName))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Mockito.verify(clusterOpenApiService, times(1)).deleteCluster(env, appId, clusterName);
    }
}