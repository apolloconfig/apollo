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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.openapi.model.OpenAccessKeyDTO;
import com.ctrip.framework.apollo.openapi.server.service.AccessKeyOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
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

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AccessKeyControllerParamBindLowLevelTest {

  private static final String APP_ID = "app-1";
  private static final String ENV = "DEV";
  private static final Long ACCESS_KEY_ID = 100L;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserInfoHolder userInfoHolder;

  @MockitoBean
  private AccessKeyOpenApiService accessKeyOpenApiService;

  private UserInfo user;

  @BeforeEach
  public void setUp() {
    when(unifiedPermissionValidator.isAppAdmin(anyString())).thenReturn(true);
    user = new UserInfo();
    user.setUserId("portal-user");
    when(userInfoHolder.getUser()).thenReturn(user);
    when(userService.findByUserId("api-operator")).thenReturn(new UserInfo("api-operator"));

    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("portal-user", "N/A",
            AuthorityUtils.NO_AUTHORITIES));
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  @Test
  public void enableAccessKeyShouldUseCurrentPortalUserAndDefaultMode() throws Exception {
    mockMvc.perform(put("/openapi/v1/apps/{appId}/envs/{env}/accesskeys/{accessKeyId}/activation",
        APP_ID, ENV, ACCESS_KEY_ID)).andExpect(status().isOk());

    verify(accessKeyOpenApiService).enableAccessKey(APP_ID, ENV, ACCESS_KEY_ID, 0, "portal-user");
  }

  @Test
  public void disableAccessKeyShouldRejectBlankConsumerOperator() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);

    mockMvc.perform(put("/openapi/v1/apps/{appId}/envs/{env}/accesskeys/{accessKeyId}/deactivation",
        APP_ID, ENV, ACCESS_KEY_ID)).andExpect(status().isBadRequest());

    verify(accessKeyOpenApiService, never()).disableAccessKey(APP_ID, ENV, ACCESS_KEY_ID, null);
  }

  @Test
  public void createAccessKeyShouldUseConsumerOperatorQuery() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    OpenAccessKeyDTO response = new OpenAccessKeyDTO();
    response.setAppId(APP_ID);
    when(accessKeyOpenApiService.createAccessKey(APP_ID, ENV, "api-operator")).thenReturn(response);

    mockMvc.perform(post("/openapi/v1/apps/{appId}/envs/{env}/accesskeys", APP_ID, ENV)
        .param("operator", "api-operator")).andExpect(status().isOk());

    verify(accessKeyOpenApiService).createAccessKey(APP_ID, ENV, "api-operator");
  }
}
