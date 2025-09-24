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
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenPageDTOOpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.server.service.InstanceOpenApiService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = InstanceController.class)
public class InstanceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ConsumerAuthUtil consumerAuthUtil;

  @MockBean
  private InstanceOpenApiService instanceOpenApiService;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  @Test
  public void testGetInstanceCountByNamespace() throws Exception {
    final String appId = "app-id-test";
    final String env = "DEV";
    final String clusterName = "default";
    final String namespaceName = "application";
    final int mockInstanceCount = 10;

    when(this.instanceOpenApiService.getInstanceCountByNamespace(appId, env, clusterName, namespaceName))
        .thenReturn(mockInstanceCount);

    this.mockMvc.perform(MockMvcRequestBuilders.get(
            String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/instances",
                env, appId, clusterName, namespaceName)))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().string(String.valueOf(mockInstanceCount)));
  }

  @Test
  public void testGetInstancesByRelease() throws Exception {
    final String env = "DEV";
    final long releaseId = 100L;
    final int page = 0;
    final int size = 10;

    OpenPageDTOOpenInstanceDTO mockPage = new OpenPageDTOOpenInstanceDTO();
    mockPage.setPage(page);
    mockPage.setSize(size);
    mockPage.setTotal(1L);
    mockPage.setContent(Arrays.asList(new OpenInstanceDTO()));

    when(this.instanceOpenApiService.getByRelease(env, releaseId, page, size))
        .thenReturn(mockPage);

    this.mockMvc.perform(MockMvcRequestBuilders.get(
            String.format("/openapi/v1/envs/%s/releases/%s/instances", env, releaseId))
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size)))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testGetInstancesByNamespace() throws Exception {
    final String env = "PRO";
    final String appId = "some-app";
    final String clusterName = "default";
    final String namespaceName = "application";
    final String instanceAppId = "instance-app-id";
    final int page = 0;
    final int size = 20;

    OpenPageDTOOpenInstanceDTO mockPage = new OpenPageDTOOpenInstanceDTO();
    mockPage.setPage(page);
    mockPage.setSize(size);
    mockPage.setTotal(2L);
    mockPage.setContent(Arrays.asList(new OpenInstanceDTO(), new OpenInstanceDTO()));

    when(this.instanceOpenApiService.getByNamespace(env, appId, clusterName, namespaceName, instanceAppId, page, size))
        .thenReturn(mockPage);

    this.mockMvc.perform(MockMvcRequestBuilders.get(
            String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/instances_search",
                env, appId, clusterName, namespaceName))
            .param("instanceAppId", instanceAppId)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size)))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  public void testGetInstancesExcludingReleases() throws Exception {
    final String env = "UAT";
    final String appId = "another-app";
    final String clusterName = "default";
    final String namespaceName = "application";
    final String releaseIds = "1,2,3";

    when(this.instanceOpenApiService.getByReleasesNotIn(env, appId, clusterName, namespaceName, ImmutableSet.of(1L, 2L, 3L)))
        .thenReturn(Arrays.asList(new OpenInstanceDTO()));

    this.mockMvc.perform(MockMvcRequestBuilders.get(
            String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/instances_not_in",
                env, appId, clusterName, namespaceName))
            .param("excludeReleases", releaseIds))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  public void testGetInstancesExcludingReleasesWithEmptyParam() throws Exception {
    final String env = "UAT";
    final String appId = "another-app";
    final String clusterName = "default";
    final String namespaceName = "application";

    this.mockMvc.perform(MockMvcRequestBuilders.get(
            String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/instances_not_in",
                env, appId, clusterName, namespaceName))
            .param("excludeReleases", ""))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest());
  }
}
