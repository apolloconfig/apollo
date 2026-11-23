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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.ControllableAuthorizationConfiguration;
import com.ctrip.framework.apollo.portal.PortalApplication;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Regression test for GHSA-w267-r3xx-grw2: with the create-application restriction enabled,
 * an unauthorized Portal user could register a missing environment for an existing application,
 * because portal {@code POST /apps/envs/{env}} was missing the authorization check that its
 * sibling {@code POST /apps} enforces.
 *
 * <p>The endpoint allows the call when the caller holds the platform create-application
 * permission, or when the caller already administers the target app (recovery of a missing
 * environment for their own app). Role initialization only sets up the env roles for the new
 * environment; it does not grant the caller an administrator role, so the caller must already
 * hold one of the two permissions above.
 *
 * <p>The test runs with the create-application restriction enabled
 * ({@code role.create-application.enabled=true}). The {@code ControllableAuthorizationConfiguration}
 * grants every {@code userHasPermission} check to user {@code luke} and none to any other user,
 * so {@code luke} is the user with the platform create-application permission (but not an admin
 * of the target app), and {@code xxx} is the unprivileged user. {@code AppService} and
 * {@code RoleInitializationService} are {@code @MockitoBean}s so the request completes without a live
 * admin-service; the security-relevant fact is whether the authorization layer blocks the
 * request before the backend call is reached. The mocks are {@code @MockitoBean}s (not a nested
 * {@code @Configuration} class) on purpose: a nested {@code @Configuration} in this package would
 * be picked up by the portal's component scan and poison every other portal test context with
 * a mocked {@code AppService}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {PortalApplication.class, ControllableAuthorizationConfiguration.class},
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"role.create-application.enabled=true"})
public class AppControllerCreateAppInEnvAuthTest {

  private final Gson GSON = new Gson();
  private final String appId = "attacker-app";
  private final String env = "LOCAL";
  protected RestTemplate restTemplate = new RestTemplate();

  @Value("${local.server.port}")
  int port;

  @Autowired
  private UserInfoHolder userInfoHolder;
  @Autowired
  private RolePermissionService rolePermissionService;
  @MockitoBean
  private AppService appService;
  @MockitoBean
  private RoleInitializationService roleInitializationService;

  @PostConstruct
  private void postConstruct() {
    System.setProperty("spring.profiles.active", "test");
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
  }

  protected String url(String path) {
    return "http://localhost:" + port + path;
  }

  void setUserId(String userId) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    when(userInfoHolder.getUser()).thenReturn(userInfo);
  }

  private HttpEntity<String> appEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    String body = "{\"appId\":\"" + appId + "\",\"name\":\"x\",\"orgId\":\"o\",\"orgName\":\"o\","
        + "\"ownerName\":\"nobody\",\"ownerEmail\":\"a@b.c\"}";
    return new HttpEntity<>(body, headers);
  }

  private static boolean hasAssignRoleFor(List<Permission> permissions, String targetAppId) {
    return permissions != null && permissions.stream()
        .anyMatch(p -> PermissionType.ASSIGN_ROLE.equals(p.getPermissionType())
            && targetAppId.equals(p.getTargetId()));
  }

  /**
   * Negative control: the guarded sibling rejects an unprivileged user.
   */
  @Test
  public void control_createApp_isForbiddenForUnprivilegedUser() {
    setUserId("xxx");
    try {
      restTemplate.postForEntity(url("/apps"), appEntity(), String.class);
      fail("guarded sibling POST /apps must reject unprivileged user");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
  }

  /**
   * The fix: the env-scoped create must reject a user who neither holds the platform
   * create-application permission nor administers the target app, and must not reach the
   * backend call. On the unfixed code this test fails (200 + backend call reached).
   */
  @Test
  public void createAppInEnv_isForbiddenForUnprivilegedUser() {
    setUserId("xxx");
    try {
      restTemplate.postForEntity(url("/apps/envs/{env}"), appEntity(), String.class, env);
      fail("POST /apps/envs/{env} must reject unprivileged user");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
    verify(appService, never()).createAppInRemote(any(), any(), any());
  }

  /**
   * Recovery: an admin of the target app may register a missing environment for their own app,
   * even without the platform create-application permission.
   */
  @Test
  public void createAppInEnv_isAllowedForTargetAppAdmin() {
    setUserId("app-admin");
    when(rolePermissionService.hasAnyPermission(eq("app-admin"),
        argThat(perms -> hasAssignRoleFor(perms, appId)))).thenReturn(true);
    ResponseEntity<String> resp =
        restTemplate.postForEntity(url("/apps/envs/{env}"), appEntity(), String.class, env);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    verify(appService, times(1)).createAppInRemote(eq(Env.valueOf(env)), any(), eq("app-admin"));
  }

  /**
   * The admin allowance is scoped to the target app: an admin of a different app is rejected.
   */
  @Test
  public void createAppInEnv_isForbiddenForAdminOfAnotherApp() {
    setUserId("other-admin");
    when(rolePermissionService.hasAnyPermission(eq("other-admin"),
        argThat(perms -> hasAssignRoleFor(perms, "other-app")))).thenReturn(true);
    try {
      restTemplate.postForEntity(url("/apps/envs/{env}"), appEntity(), String.class, env);
      fail("POST /apps/envs/{env} must reject an admin of a different app");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
    verify(appService, never()).createAppInRemote(any(), any(), any());
  }

  /**
   * Positive control: a user with the platform create-application permission can still register
   * an app into an environment.
   */
  @Test
  public void createAppInEnv_isAllowedForPrivilegedUser() {
    setUserId("luke");
    ResponseEntity<String> resp =
        restTemplate.postForEntity(url("/apps/envs/{env}"), appEntity(), String.class, env);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    verify(appService, times(1)).createAppInRemote(eq(Env.valueOf(env)), any(), eq("luke"));
  }
}
