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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.openapi.model.OpenPermissionConditionDTO;
import com.ctrip.framework.apollo.openapi.server.service.PermissionOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.RoleType;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Low-level parameter binding tests for {@link PermissionController}.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class PermissionControllerParamBindLowLevelTest {

  private static final String APP_ID = "app-1";
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
  private PermissionOpenApiService permissionOpenApiService;

  @BeforeEach
  public void setUp() {
    when(unifiedPermissionValidator.hasManageAppMasterPermission(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.hasAssignRolePermission(anyString())).thenReturn(true);
    UserInfo user = new UserInfo("portal-user");
    when(userInfoHolder.getUser()).thenReturn(user);
    when(userService.findByUserId("api-operator")).thenReturn(new UserInfo("api-operator"));

    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("portal-user", "N/A",
            AuthorityUtils.NO_AUTHORITIES));
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  @Test
  public void hasAppPermissionShouldUseExplicitUserIdQuery() throws Exception {
    OpenPermissionConditionDTO response = new OpenPermissionConditionDTO();
    response.setHasPermission(true);
    when(permissionOpenApiService.hasAppPermission(APP_ID, "AssignRole", "target-user"))
        .thenReturn(response);

    mockMvc
        .perform(get("/openapi/v1/apps/{appId}/permissions/{permissionType}", APP_ID, "AssignRole")
            .param("userId", "target-user"))
        .andExpect(status().isOk());

    verify(permissionOpenApiService).hasAppPermission(APP_ID, "AssignRole", "target-user");
  }

  @Test
  public void assignNamespaceRoleShouldBindTargetUserAndResolvedOperator() throws Exception {
    mockMvc.perform(post("/openapi/v1/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}",
        APP_ID, NAMESPACE, RoleType.MODIFY_NAMESPACE).param("userId", "target-user")
        .param("operator", "api-operator")).andExpect(status().isOk());

    verify(permissionOpenApiService).assignNamespaceRoleToUser(APP_ID, NAMESPACE,
        RoleType.MODIFY_NAMESPACE, "target-user", "api-operator");
  }

  @Test
  public void initAppPermissionShouldUseCurrentPortalUser() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    mockMvc.perform(post("/openapi/v1/apps/{appId}/namespaces/{namespaceName}/permission-init",
        APP_ID, NAMESPACE)).andExpect(status().isOk());

    verify(permissionOpenApiService).initAppPermission(APP_ID, NAMESPACE, "portal-user");
  }
}
