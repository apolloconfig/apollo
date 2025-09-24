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

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.NamespaceGrayDelReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseBO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.server.service.impl.ServerReleaseOpenApiService;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ReleaseController.class)
public class ReleaseControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ReleaseService releaseService;

  @MockBean
  private com.ctrip.framework.apollo.portal.spi.UserService userService;

  @MockBean
  private NamespaceBranchService namespaceBranchService;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  // Only mock the server-side service; it also implements ReleaseOpenApiService
  // to avoid NoUniqueBeanDefinition issues when autowiring both types.

  @MockBean
  private ApplicationEventPublisher publisher;

  @MockBean
  private ServerReleaseOpenApiService serverReleaseOpenApiService;

  @MockBean
  private UserPermissionValidator userPermissionValidator;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testCreateRelease_success_and_missingFields() throws Exception {
    when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    when(userService.findByUserId("apollo")).thenReturn(user);

    NamespaceReleaseDTO model = new NamespaceReleaseDTO();
    model.setReleasedBy("apollo");
    model.setReleaseTitle("title");
    model.setReleaseComment("comment");
    model.setIsEmergencyPublish(false);

    OpenReleaseDTO published = new OpenReleaseDTO();
    published.setId(1L);
    when(serverReleaseOpenApiService.publishNamespace(eq("app"), eq("DEV"), eq("default"), eq("application"), any(NamespaceReleaseDTO.class)))
        .thenReturn(published);

    mockMvc.perform(MockMvcRequestBuilders.post(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(model)))
        .andExpect(status().isOk());

    // missing releasedBy -> 400
    NamespaceReleaseDTO bad = new NamespaceReleaseDTO();
    bad.setReleaseTitle("t");
    mockMvc.perform(MockMvcRequestBuilders.post(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bad)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateGrayRelease_success() throws Exception {
    when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    when(userService.findByUserId("apollo")).thenReturn(user);

    NamespaceReleaseDTO model = new NamespaceReleaseDTO();
    model.setReleasedBy("apollo");
    model.setReleaseTitle("title");
    model.setReleaseComment("comment");

    ReleaseDTO release = new ReleaseDTO();
    release.setId(1L);
    release.setAppId("app");
    release.setClusterName("default");
    release.setNamespaceName("application");
    release.setConfigurations("{}");

    when(releaseService.publish(any(com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel.class)))
        .thenReturn(release);

    mockMvc.perform(MockMvcRequestBuilders.post(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/branch/releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(model)))
        .andExpect(status().isOk());
  }

  @Test
  public void testCreateGrayDelRelease_success() throws Exception {
    when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    when(userService.findByUserId("apollo")).thenReturn(user);

    NamespaceGrayDelReleaseDTO model = new NamespaceGrayDelReleaseDTO();
    model.setReleasedBy("apollo");
    model.setReleaseTitle("title");
    model.setGrayDelKeys(Collections.singletonList("a"));

    ReleaseDTO release = new ReleaseDTO();
    release.setId(2L);
    release.setAppId("app");
    release.setClusterName("default");
    release.setNamespaceName("application");
    release.setConfigurations("{}");
    when(releaseService.publish(any(com.ctrip.framework.apollo.portal.entity.model.NamespaceGrayDelReleaseModel.class), anyString()))
        .thenReturn(release);

    mockMvc.perform(MockMvcRequestBuilders.post(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/branch/gray-del-releases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(model)))
        .andExpect(status().isOk());
  }

  @Test
  public void testRollback_ok_and_forbidden() throws Exception {
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    when(userService.findByUserId("apollo")).thenReturn(user);

    ReleaseDTO release = new ReleaseDTO();
    release.setId(100L);
    release.setAppId("app");
    release.setClusterName("default");
    release.setNamespaceName("application");
    when(releaseService.findReleaseById(Env.DEV, 100L)).thenReturn(release);

    when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);
    doNothing().when(serverReleaseOpenApiService).rollbackRelease("DEV", 100L, "apollo");

    mockMvc.perform(MockMvcRequestBuilders.put("/openapi/v1/envs/DEV/releases/100/rollback")
            .param("operator", "apollo"))
        .andExpect(status().isOk());

    // forbidden when permission missing
    when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(false);
    mockMvc.perform(MockMvcRequestBuilders.put("/openapi/v1/envs/DEV/releases/100/rollback")
            .param("operator", "apollo"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void testGetReleaseById_ok_and_forbidden() throws Exception {
    OpenReleaseDTO dto = new OpenReleaseDTO();
    dto.setAppId("app");
    dto.setClusterName("default");
    dto.setNamespaceName("application");
    when(serverReleaseOpenApiService.getReleaseById("DEV", 10L)).thenReturn(dto);

    when(userPermissionValidator.shouldHideConfigToCurrentUser("app", "DEV", "default", "application"))
        .thenReturn(false);
    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/envs/DEV/releases/10"))
        .andExpect(status().isOk());

    // hide -> 403
    when(userPermissionValidator.shouldHideConfigToCurrentUser("app", "DEV", "default", "application"))
        .thenReturn(true);
    mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/envs/DEV/releases/10"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void testFindAllAndActiveReleases() throws Exception {
    when(userPermissionValidator.shouldHideConfigToCurrentUser("app", "DEV", "default", "application"))
        .thenReturn(false)
        .thenReturn(true); // for the second call sequence below

    List<OpenReleaseBO> bos = Arrays.asList(new OpenReleaseBO(), new OpenReleaseBO());
    when(serverReleaseOpenApiService.findAllReleases("app", "DEV", "default", "application", 0, 5))
        .thenReturn(bos);

    List<OpenReleaseDTO> actives = Arrays.asList(new OpenReleaseDTO());
    when(serverReleaseOpenApiService.findActiveReleases("app", "DEV", "default", "application", 0, 5))
        .thenReturn(actives);

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/releases/all")
            .param("page", "0").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/releases/active")
            .param("page", "0").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0))); // hidden path returns empty list
  }
}
