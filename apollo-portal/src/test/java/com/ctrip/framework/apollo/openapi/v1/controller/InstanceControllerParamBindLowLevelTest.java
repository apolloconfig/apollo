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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class InstanceControllerParamBindLowLevelTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private InstanceOpenApiService instanceOpenApiService;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  @Test
  public void getInstanceCountByNamespace_shouldBindPathVariables() throws Exception {
    when(instanceOpenApiService.getInstanceCountByNamespace(anyString(), anyString(), anyString(),
        anyString()))
        .thenReturn(15);

    mockMvc.perform(
            get("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances",
                "FAT", "sample-app", "default", "application"))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

    verify(instanceOpenApiService)
        .getInstanceCountByNamespace("sample-app", "FAT", "default", "application");
  }

  @Test
  public void getInstancesByRelease_shouldBindPathAndQuery() throws Exception {
    OpenInstancePageDTO page = new OpenInstancePageDTO();
    page.setPage(1);
    page.setSize(5);
    page.setTotal(20L);
    page.setInstances(Collections.singletonList(new OpenInstanceDTO()));

    when(instanceOpenApiService.getByRelease("DEV", 123L, 1, 5)).thenReturn(page);

    mockMvc.perform(get("/openapi/v1/envs/{env}/instances/by-release", "DEV")
        .param("releaseId", "123").param("page", "1").param("size", "5"))
        .andExpect(status().isOk());

    verify(instanceOpenApiService).getByRelease("DEV", 123L, 1, 5);
  }

  @Test
  public void getInstancesExcludingReleases_shouldRejectEmptyParam() throws Exception {
    mockMvc
        .perform(get("/openapi/v1/envs/{env}/instances/by-namespace-and-releases-not-in", "UAT")
            .param("appId", "demo-app").param("clusterName", "default")
            .param("namespaceName", "application").param("releaseIds", ""))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(instanceOpenApiService);
  }

  @Test
  public void getInstancesExcludingReleases_shouldRejectNonNumeric() throws Exception {
    mockMvc
        .perform(get("/openapi/v1/envs/{env}/instances/by-namespace-and-releases-not-in", "PRO")
            .param("appId", "app").param("clusterName", "cluster")
            .param("namespaceName", "namespace").param("releaseIds", "1,invalid,3"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(instanceOpenApiService);
  }

  @Test
  public void getInstancesExcludingReleases_shouldBindAllParameters() throws Exception {
    when(instanceOpenApiService.getByReleasesNotIn(
        anyString(), anyString(), anyString(), anyString(), anySet()))
        .thenReturn(Arrays.asList(new OpenInstanceDTO(), new OpenInstanceDTO()));

    mockMvc.perform(
            get("/openapi/v1/envs/{env}/instances/by-namespace-and-releases-not-in",
                "PRO")
                .param("appId", "bind-app")
                .param("clusterName", "cluster-a")
                .param("namespaceName", "namespace-x")
                .param("releaseIds", "10,  11 ,12"))
        .andExpect(status().isOk());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
    verify(instanceOpenApiService)
        .getByReleasesNotIn(eq("PRO"), eq("bind-app"), eq("cluster-a"), eq("namespace-x"),
            captor.capture());

    assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 11L, 12L);
  }
}
