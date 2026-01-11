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

import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.server.service.InstanceOpenApiService;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Collections;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"api.pool.max.total=100", "api.pool.max.per.route=100",
    "api.connectionTimeToLive=30000", "api.connectTimeout=5000", "api.readTimeout=5000"})
public class InstanceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InstanceOpenApiService instanceOpenApiService;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  @Test
  public void testGetInstanceCountByNamespace() throws Exception {
    String appId = "app-id-test";
    String env = "DEV";
    String clusterName = "default";
    String namespaceName = "application";
    int mockInstanceCount = 10;

    Mockito.when(
        instanceOpenApiService.getInstanceCountByNamespace(appId, env, clusterName, namespaceName))
        .thenReturn(mockInstanceCount);

    mockMvc
        .perform(MockMvcRequestBuilders
            .get(String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/instances",
                env, appId, clusterName, namespaceName)))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().string(String.valueOf(mockInstanceCount)));

    Mockito.verify(instanceOpenApiService).getInstanceCountByNamespace(appId, env, clusterName,
        namespaceName);
  }

  @Test
  public void testGetInstancesByRelease() throws Exception {
    String env = "DEV";
    int releaseId = 100;
    int page = 0;
    int size = 10;

    OpenInstancePageDTO mockPage = new OpenInstancePageDTO();
    mockPage.setPage(page);
    mockPage.setSize(size);
    mockPage.setTotal(1L);
    mockPage.setInstances(Collections.singletonList(new OpenInstanceDTO()));

    Mockito.when(instanceOpenApiService.getByRelease(env, releaseId, page, size))
        .thenReturn(mockPage);

    mockMvc
        .perform(MockMvcRequestBuilders
            .get(String.format("/openapi/v1/envs/%s/instances/by-release", env))
            .param("releaseId", String.valueOf(releaseId)).param("page", String.valueOf(page))
            .param("size", String.valueOf(size)))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk())
        .andExpect(jsonPath("$.instances").isArray())
        .andExpect(jsonPath("$.instances[0]").exists());

    Mockito.verify(instanceOpenApiService).getByRelease(env, releaseId, page, size);
  }

  @Test
  public void testGetInstancesExcludingReleases() throws Exception {
    String env = "UAT";
    String appId = "another-app";
    String clusterName = "default";
    String namespaceName = "application";
    String releaseIds = "1,2,3";
    Set<Long> releaseIdSet = Sets.newHashSet(1L, 2L, 3L);

    Mockito.when(instanceOpenApiService.getByReleasesNotIn(env, appId, clusterName, namespaceName,
        releaseIdSet)).thenReturn(Collections.singletonList(new OpenInstanceDTO()));

    mockMvc
        .perform(MockMvcRequestBuilders
            .get(String.format("/openapi/v1/envs/%s/instances/by-namespace-and-releases-not-in",
                env))
            .param("appId", appId).param("clusterName", clusterName)
            .param("namespaceName", namespaceName).param("releaseIds", releaseIds))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$[0]").exists());

    Mockito.verify(instanceOpenApiService).getByReleasesNotIn(env, appId, clusterName,
        namespaceName, releaseIdSet);
  }

  @Test
  public void testGetInstancesExcludingReleases_InvalidReleaseIds() throws Exception {
    String env = "UAT";
    String appId = "another-app";
    String clusterName = "default";
    String namespaceName = "application";
    String releaseIds = "1,abc,3";

    mockMvc
        .perform(MockMvcRequestBuilders
            .get(String.format("/openapi/v1/envs/%s/instances/by-namespace-and-releases-not-in",
                env))
            .param("appId", appId).param("clusterName", clusterName)
            .param("namespaceName", namespaceName).param("releaseIds", releaseIds))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(instanceOpenApiService);
  }

}
