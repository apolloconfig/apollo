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

import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.repository.ConsumerAuditRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerTokenRepository;
import com.ctrip.framework.apollo.openapi.server.service.ServerAppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RolePermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author wxq
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AppController.class)
@Import({ConsumerService.class, ServerAppOpenApiService.class})
public class AppControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PortalSettings portalSettings;

  @MockBean
  private AppService appService;

  @MockBean
  private ClusterService clusterService;

  @MockBean
  private ConsumerAuthUtil consumerAuthUtil;

  @MockBean
  private PermissionRepository permissionRepository;

  @MockBean
  private RolePermissionRepository rolePermissionRepository;

  @MockBean
  private UserInfoHolder userInfoHolder;
  @MockBean
  private ConsumerTokenRepository consumerTokenRepository;
  @MockBean
  private ConsumerRepository consumerRepository;
  @MockBean
  private ConsumerAuditRepository consumerAuditRepository;
  @MockBean
  private ConsumerRoleRepository consumerRoleRepository;
  @MockBean
  private PortalConfig portalConfig;
  @MockBean
  private RolePermissionService rolePermissionService;
  @MockBean
  private UserService userService;
  @MockBean
  private RoleRepository roleRepository;

  @Test
  public void testFindAppsAuthorized() throws Exception {
    final long consumerId = 123456;
    Mockito.when(this.consumerAuthUtil.retrieveConsumerIdFromCtx()).thenReturn(consumerId);

    final List<ConsumerRole> consumerRoles = Arrays.asList(
        generateConsumerRoleByRoleId(6),
        generateConsumerRoleByRoleId(7),
        generateConsumerRoleByRoleId(8)
    );
    Mockito.when(this.consumerRoleRepository.findByConsumerId(consumerId))
        .thenReturn(consumerRoles);

    Mockito.when(this.roleRepository.findAllById(Mockito.any())).thenAnswer(invocation -> {
      Set<Role> roles = new HashSet<>();
      Iterable<Long> roleIds = invocation.getArgument(0);
      for (Long roleId : roleIds) {
        if (roleId == 6) {
          roles.add(generateRoleByIdAndRoleName(6, "ModifyNamespace+app1+application+DEV"));
        }
        if (roleId == 7) {
          roles.add(generateRoleByIdAndRoleName(7, "ReleaseNamespace+app1+application+DEV"));
        }
        if (roleId == 8) {
          roles.add(generateRoleByIdAndRoleName(8, "Master+app2"));
        }
      }
      assertEquals(3, roles.size());
      return roles;
    });

    this.mockMvc.perform(MockMvcRequestBuilders.get("/openapi/v1/apps/authorized"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

    Mockito.verify(this.consumerRoleRepository, Mockito.times(1)).findByConsumerId(consumerId);
    Mockito.verify(this.roleRepository, Mockito.times(1)).findAllById(Mockito.any());

    ArgumentCaptor<Set<String>> appIdsCaptor = ArgumentCaptor.forClass(Set.class);
    Mockito.verify(this.appService).findByAppIds(appIdsCaptor.capture());
    Set<String> appIds = appIdsCaptor.getValue();
    assertEquals(Sets.newHashSet("app1", "app2"), appIds);
  }

  private static ConsumerRole generateConsumerRoleByRoleId(long roleId) {
    ConsumerRole consumerRole = new ConsumerRole();
    consumerRole.setRoleId(roleId);
    return consumerRole;
  }

  private static Role generateRoleByIdAndRoleName(long id, String roleName) {
    Role role = new Role();
    role.setId(id);
    role.setRoleName(roleName);
    return role;
  }

}