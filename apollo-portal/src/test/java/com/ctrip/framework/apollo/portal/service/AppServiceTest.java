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
package com.ctrip.framework.apollo.portal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.repository.AppRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author wxq
 */
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest
@ContextConfiguration(classes = AppService.class)
class AppServiceTest {

  private static final String OPERATOR_USER_ID = "userId-operator";

  @Autowired
  AppService appService;

  @MockBean
  UserInfoHolder userInfoHolder;
  @MockBean
  AdminServiceAPI.AppAPI appAPI;
  @MockBean
  AppRepository appRepository;
  @MockBean
  ClusterService clusterService;
  @MockBean
  AppNamespaceService appNamespaceService;
  @MockBean
  RoleInitializationService roleInitializationService;
  @MockBean
  RolePermissionService rolePermissionService;
  @MockBean
  FavoriteService favoriteService;
  @MockBean
  UserService userService;
  @MockBean
  ApplicationEventPublisher publisher;
  @MockBean
  ApolloAuditLogApi apolloAuditLogApi;
  @MockBean
  PortalSettings portalSettings;

  @BeforeEach
  void beforeEach() {
    // reset the mock after each test
    Mockito.reset(
        userInfoHolder,
        appAPI,
        appRepository,
        clusterService,
        appNamespaceService,
        roleInitializationService,
        rolePermissionService,
        favoriteService,
        userService,
        publisher,
        apolloAuditLogApi
    );
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(OPERATOR_USER_ID);
    Mockito.when(userInfoHolder.getUser())
        .thenReturn(userInfo);
  }

  @Test
  void createAppAndAddRolePermissionButAppAlreadyExists() {
    Mockito.when(appRepository.findByAppId(Mockito.any()))
        .thenReturn(new App());

    assertThrows(
        BadRequestException.class,
        () -> appService.createAppAndAddRolePermission(new App(), Collections.emptySet())
    );
  }

  @Test
  void createAppAndAddRolePermissionButOwnerNotExists() {
    Mockito.when(userService.findByUserId(Mockito.any()))
        .thenReturn(null);
    assertThrows(
        BadRequestException.class,
        () -> appService.createAppAndAddRolePermission(new App(), Collections.emptySet())
    );
  }

  @Test
  void createAppAndAddRolePermission() {
    final String userId = "user100";
    final String appId = "appId100";
    {
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(userId);
      userInfo.setEmail("xxx@xxx.com");
      Mockito.when(userService.findByUserId(Mockito.eq(userId)))
          .thenReturn(userInfo);
    }

    final App app = new App();
    app.setAppId(appId);
    app.setOwnerName(userId);
    Set<String> admins = new HashSet<>(Arrays.asList("user1", "user2"));

    final App createdApp = new App();
    createdApp.setAppId(appId);
    createdApp.setOwnerName(userId);
    {
      Mockito.when(appRepository.save(Mockito.eq(app)))
          .thenReturn(createdApp);
    }
    appService.createAppAndAddRolePermission(app, admins);
    Mockito.verify(appRepository, Mockito.times(1))
        .findByAppId(Mockito.eq(appId));
    Mockito.verify(userService, Mockito.times(1))
        .findByUserId(Mockito.eq(userId));
    Mockito.verify(userInfoHolder, Mockito.times(2))
        .getUser();
    Mockito.verify(appRepository, Mockito.times(1))
        .save(Mockito.eq(app));
    Mockito.verify(appNamespaceService, Mockito.times(1))
        .createDefaultAppNamespace(Mockito.eq(appId));
    Mockito.verify(roleInitializationService, Mockito.times(1))
        .initAppRoles(Mockito.eq(createdApp));

    Mockito.verify(rolePermissionService, Mockito.times(1))
        .assignRoleToUsers(Mockito.any(), Mockito.eq(admins), Mockito.eq(OPERATOR_USER_ID));
  }

  @Test
  void testDeleteAppInLocal() {
    final String appId = "appId100";
    {
      App app = new App();
      app.setAppId(appId);
      Mockito.when(appRepository.findByAppId(Mockito.eq(appId)))
          .thenReturn(app);
    }
    {
      Mockito.when(appRepository.deleteApp(Mockito.eq(appId), Mockito.eq(OPERATOR_USER_ID)))
          .thenReturn(1);
    }

    App deletedApp = appService.deleteAppInLocal(appId);
    Mockito.verify(appRepository, Mockito.times(1))
        .deleteApp(Mockito.eq(appId), Mockito.eq(OPERATOR_USER_ID));
    Mockito.verify(userInfoHolder, Mockito.times(1))
        .getUser();
    Mockito.verify(apolloAuditLogApi, Mockito.times(1))
        .appendDataInfluences(Mockito.eq(Collections.singletonList(deletedApp)), Mockito.eq(App.class));
    Mockito.verify(appNamespaceService, Mockito.times(1))
        .batchDeleteByAppId(Mockito.eq(appId), Mockito.eq(OPERATOR_USER_ID));
    Mockito.verify(favoriteService, Mockito.times(1))
        .batchDeleteByAppId(Mockito.eq(appId), Mockito.eq(OPERATOR_USER_ID));
    Mockito.verify(rolePermissionService, Mockito.times(1))
        .deleteRolePermissionsByAppId(Mockito.eq(appId), Mockito.eq(OPERATOR_USER_ID));

    assertEquals(OPERATOR_USER_ID, deletedApp.getDataChangeLastModifiedBy());
  }
}