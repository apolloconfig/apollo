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

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.server.service.ReleaseOpenApiService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = NamespaceBranchController.class)
public class NamespaceBranchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean(name = "consumerPermissionValidator")
  private ConsumerPermissionValidator consumerPermissionValidator;

  @MockBean
  private ReleaseOpenApiService releaseOpenApiService;

  @MockBean
  private NamespaceBranchService namespaceBranchService;

  @MockBean
  private com.ctrip.framework.apollo.portal.spi.UserService userService;

  @MockBean
  private com.ctrip.framework.apollo.portal.spi.UserInfoHolder userInfoHolder;

  @MockBean
  private ApplicationEventPublisher publisher;

  @MockBean
  private com.ctrip.framework.apollo.portal.component.config.PortalConfig portalConfig;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testFindBranch_ok_and_notFound() throws Exception {
    // prepare a NamespaceBO with base info to avoid NPE in transform
    com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO bo = new com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO();
    NamespaceDTO base = new NamespaceDTO();
    base.setAppId("app");
    base.setClusterName("default");
    base.setNamespaceName("application");
    bo.setBaseInfo(base);
    Mockito.when(namespaceBranchService.findBranch("app", Env.DEV, "default", "application"))
        .thenReturn(bo);
    Mockito.when(namespaceBranchService.findBranch("app", Env.DEV, "default", "none"))
        .thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches"))
        .andExpect(status().isOk());

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/none/branches"))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateBranch_success_and_missingOperator() throws Exception {
    Mockito.when(consumerPermissionValidator.hasCreateNamespacePermission("app")).thenReturn(true);
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    Mockito.when(userService.findByUserId("apollo")).thenReturn(user);
    Mockito.when(userInfoHolder.getUser()).thenReturn(user);

    NamespaceDTO ns = new NamespaceDTO();
    ns.setAppId("app");
    ns.setClusterName("default");
    ns.setNamespaceName("application");
    Mockito.when(namespaceBranchService.createBranch("app", Env.DEV, "default", "application", "apollo"))
        .thenReturn(ns);

    mockMvc.perform(MockMvcRequestBuilders.post(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches")
            .param("operator", "apollo"))
        .andExpect(status().isCreated());

    // missing operator -> controller fetches from userInfoHolder and still succeeds
    mockMvc.perform(MockMvcRequestBuilders.post(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches"))
        .andExpect(status().isCreated());
  }

  @Test
  public void testDeleteBranch_ok_and_forbidden() throws Exception {
    // PreAuthorize on delete requires modify permission
    Mockito.when(consumerPermissionValidator.hasModifyNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    Mockito.when(userService.findByUserId("apollo")).thenReturn(user);
    Mockito.when(userInfoHolder.getUser()).thenReturn(user);

    // default: no active release
    Mockito.when(releaseOpenApiService.getLatestActiveRelease(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(null);
    // allow delete path by having modify=true and no active release
    Mockito.when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(false);

    mockMvc.perform(MockMvcRequestBuilders.delete(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/branchA")
            .param("operator", "apollo"))
        .andExpect(status().isNoContent());

    Mockito.verify(namespaceBranchService, times(1)).deleteBranch(eq("app"), eq(Env.DEV), eq("default"), eq("application"), eq("branchA"), eq("apollo"));

    // now with active release -> forbidden 403
    Mockito.when(releaseOpenApiService.getLatestActiveRelease("app", "DEV", "branchB", "application"))
        .thenReturn(new com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO());
    mockMvc.perform(MockMvcRequestBuilders.delete(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/branchB")
            .param("operator", "apollo"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void testMergeBranch_ok_and_emergencyNotAllowed() throws Exception {
    Mockito.when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);

    NamespaceReleaseDTO model = new NamespaceReleaseDTO();
    model.setReleaseTitle("t");
    model.setReleaseComment("c");
    model.setIsEmergencyPublish(false);

    ReleaseDTO release = new ReleaseDTO();
    release.setId(1L);
    Mockito.when(namespaceBranchService.merge(eq("app"), eq(Env.DEV), eq("default"), eq("application"), eq("branchA"), anyString(), anyString(), anyBoolean(), anyBoolean()))
        .thenReturn(release);

    mockMvc.perform(MockMvcRequestBuilders.patch(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/branchA")
            .param("deleteBranch", "true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(model)))
        .andExpect(status().isOk());

    // emergency not allowed -> 400
    Mockito.when(portalConfig.isEmergencyPublishAllowed(Env.DEV)).thenReturn(false);
    model.setIsEmergencyPublish(true);
    mockMvc.perform(MockMvcRequestBuilders.patch(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/branchB")
            .param("deleteBranch", "false")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(model)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testGetBranchGrayRules_found_and_notFound() throws Exception {
    Mockito.when(namespaceBranchService.findBranchGrayRules("app", Env.DEV, "default", "application", "b1"))
        .thenReturn(new GrayReleaseRuleDTO("app","default","application","b1"));
    Mockito.when(namespaceBranchService.findBranchGrayRules("app", Env.DEV, "default", "application", "b2"))
        .thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/b1/rules"))
        .andExpect(status().isOk());
    mockMvc.perform(MockMvcRequestBuilders.get(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/b2/rules"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testUpdateBranchGrayRules_ok() throws Exception {
    Mockito.when(consumerPermissionValidator.hasReleaseNamespacePermission("app", "DEV", "default", "application"))
        .thenReturn(true);
    com.ctrip.framework.apollo.portal.entity.bo.UserInfo user = new com.ctrip.framework.apollo.portal.entity.bo.UserInfo();
    user.setUserId("apollo");
    Mockito.when(userService.findByUserId("apollo")).thenReturn(user);
    Mockito.when(userInfoHolder.getUser()).thenReturn(user);

    OpenGrayReleaseRuleDTO rules = new OpenGrayReleaseRuleDTO();
    rules.setRuleItems(new java.util.ArrayList<>());

    mockMvc.perform(MockMvcRequestBuilders.put(
            "/openapi/v1/envs/DEV/apps/app/clusters/default/namespaces/application/branches/b1/rules")
            .contentType(MediaType.APPLICATION_JSON)
            .header("operator", "apollo")
            .content(objectMapper.writeValueAsString(rules)))
        .andExpect(status().isOk());

    Mockito.verify(namespaceBranchService, times(1))
        .updateBranchGrayRules(eq("app"), eq(Env.DEV), eq("default"), eq("application"), eq("b1"), any(GrayReleaseRuleDTO.class), eq("apollo"));
  }
}
