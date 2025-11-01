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

import com.ctrip.framework.apollo.openapi.model.OpenOrganizationDto;
import com.ctrip.framework.apollo.openapi.server.service.OrganizationOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"api.pool.max.total=100", "api.pool.max.per.route=100",
    "api.connectionTimeToLive=30000", "api.connectTimeout=5000", "api.readTimeout=5000"})
public class OrganizationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockBean
  private UserService userService;

  @MockBean
  private OrganizationOpenApiService organizationOpenApiService;

  private UserInfo authenticatedUser;

  @Before
  public void setUpSecurityMocks() {
    when(unifiedPermissionValidator.hasCreateClusterPermission(Mockito.anyString())).thenReturn(
        true);
    when(unifiedPermissionValidator.isAppAdmin(Mockito.anyString())).thenReturn(true);

    authenticatedUser = new UserInfo();
    authenticatedUser.setUserId("test-operator");
    when(userService.findByUserId(Mockito.anyString())).thenReturn(authenticatedUser);

    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.clearContext();
    UserIdentityContextHolder.clear();
  }

  private void authenticate() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList()));
  }

  @Test
  public void testGetOrganizations() throws Exception {
    OpenOrganizationDto org = new OpenOrganizationDto();
    org.setOrgId("org-1");
    org.setOrgName("Org One");
    List<OpenOrganizationDto> organizations = Arrays.asList(org);
    when(organizationOpenApiService.getOrganizations()).thenReturn(organizations);

    authenticate();

    this.mockMvc
        .perform(MockMvcRequestBuilders.get("/openapi/v1/organizations")
            .accept(MediaType.APPLICATION_JSON))
        .andDo(MockMvcResultHandlers.print()).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].orgId", is("org-1")))
        .andExpect(jsonPath("$[0].orgName", is("Org One")));

    verify(organizationOpenApiService).getOrganizations();
  }
}
