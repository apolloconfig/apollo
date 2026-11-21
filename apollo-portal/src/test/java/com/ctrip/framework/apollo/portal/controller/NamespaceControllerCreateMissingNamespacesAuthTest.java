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
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.portal.PortalApplication;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Regression test for GHSA-576m-qfgw-j59w: portal legacy {@code POST
 * /apps/{appId}/envs/{env}/clusters/{clusterName}/missing-namespaces} created namespace
 * structures for an arbitrary application without any authorization check, while the sibling
 * {@code POST /apps/{appId}/namespaces} handler and the OpenAPI counterpart both enforce
 * {@code hasCreateNamespacePermission(#appId)}.
 *
 * <p>The {@code ControllableAuthorizationConfiguration} grants every {@code userHasPermission}
 * check to user {@code luke} and none to any other user; the create-namespace permission itself
 * resolves through {@code RolePermissionService.hasAnyPermission}, which the test stubs for
 * {@code luke} in {@code setUserId}. So {@code luke} is the user with the create-namespace
 * permission on the target app and {@code xxx} is the unprivileged user.
 * {@code NamespaceAPI}, {@code NamespaceService} and {@code AppNamespaceService} are
 * {@code @MockitoBean}s so the request completes without a live admin-service; the
 * security-relevant fact is whether the authorization layer blocks the request before the
 * backend call is reached. The mocks are {@code @MockitoBean}s (not a nested
 * {@code @Configuration} class) on purpose: a nested {@code @Configuration} in this package
 * would be picked up by the portal's component scan and poison every other portal test
 * context with mocked services.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {PortalApplication.class, ControllableAuthorizationConfiguration.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class NamespaceControllerCreateMissingNamespacesAuthTest {

  private final String appId = "victimApp";
  private final String env = "DEV";
  private final String clusterName = "default";
  protected RestTemplate restTemplate = new RestTemplate();

  @Value("${local.server.port}")
  int port;

  @Autowired
  private UserInfoHolder userInfoHolder;
  @Autowired
  private RolePermissionService rolePermissionService;
  @MockitoBean
  private AdminServiceAPI.NamespaceAPI namespaceAPI;
  @MockitoBean
  private NamespaceService namespaceService;
  @MockitoBean
  private AppNamespaceService appNamespaceService;

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
    // hasCreateNamespacePermission resolves through UserPermissionValidator.hasPermissions,
    // which calls RolePermissionService.hasAnyPermission (not userHasPermission, which is
    // the method ControllableAuthorizationConfiguration stubs for "luke"), so grant the
    // create-namespace permission to "luke" explicitly here.
    if ("luke".equals(userId)) {
      when(rolePermissionService.hasAnyPermission(eq("luke"),
          argThat(perms -> hasCreateNamespaceFor(perms, appId)))).thenReturn(true);
    }
  }

  private static boolean hasCreateNamespaceFor(List<Permission> permissions, String targetAppId) {
    return permissions != null && permissions.stream()
        .anyMatch(p -> PermissionType.CREATE_NAMESPACE.equals(p.getPermissionType())
            && targetAppId.equals(p.getTargetId()));
  }

  private String missingNamespacesUrl() {
    return url(
        "/apps/" + appId + "/envs/" + env + "/clusters/" + clusterName + "/missing-namespaces");
  }

  private void stubNoMissingNamespaces() {
    when(namespaceAPI.getAppNamespaces(eq(appId), eq(Env.valueOf(env))))
        .thenReturn(Collections.emptyList());
    when(namespaceService.findNamespaces(eq(appId), eq(Env.valueOf(env)), eq(clusterName)))
        .thenReturn(Collections.emptyList());
    when(appNamespaceService.findByAppId(eq(appId))).thenReturn(Collections.emptyList());
  }

  private AppNamespace privateAppNamespace(String namespaceName) {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId(appId);
    appNamespace.setName(namespaceName);
    appNamespace.setFormat("properties");
    appNamespace.setPublic(false);
    return appNamespace;
  }

  /**
   * Negative control: the guarded sibling rejects an unprivileged user.
   */
  @Test
  public void control_createNamespace_isForbiddenForUnprivilegedUser() {
    setUserId("xxx");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    try {
      restTemplate.postForEntity(url("/apps/" + appId + "/namespaces"),
          new HttpEntity<>("[]", headers), String.class);
      fail("guarded sibling POST /apps/{appId}/namespaces must reject unprivileged user");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
  }

  /**
   * The fix: creating missing namespaces must reject a user without the create-namespace
   * permission on the target app, and must not reach the backend call. On the unfixed code
   * this test fails (200 + backend call reached).
   */
  @Test
  public void createMissingNamespaces_isForbiddenForUnprivilegedUser() {
    setUserId("xxx");
    stubNoMissingNamespaces();
    try {
      restTemplate.postForEntity(missingNamespacesUrl(), null, String.class);
      fail("POST /apps/{appId}/envs/{env}/clusters/{clusterName}/missing-namespaces "
          + "must reject unprivileged user");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
    verify(namespaceAPI, never()).createMissingAppNamespace(any(), any());
    verify(namespaceAPI, never()).getAppNamespaces(any(), any());
  }

  /**
   * Positive control: a user with the create-namespace permission can still trigger the
   * sync when nothing is missing, and no creation call is made.
   */
  @Test
  public void createMissingNamespaces_isAllowedForPrivilegedUser_whenNothingMissing() {
    setUserId("luke");
    stubNoMissingNamespaces();
    ResponseEntity<String> resp =
        restTemplate.postForEntity(missingNamespacesUrl(), null, String.class);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    verify(namespaceAPI, never()).createMissingAppNamespace(any(), any());
  }

  /**
   * Positive control: a user with the create-namespace permission can still create the
   * missing namespace structures for their own app.
   */
  @Test
  public void createMissingNamespaces_isAllowedForPrivilegedUser_createsMissing() {
    setUserId("luke");
    AppNamespace missing = privateAppNamespace("application");
    when(namespaceAPI.getAppNamespaces(eq(appId), eq(Env.valueOf(env))))
        .thenReturn(Collections.emptyList());
    when(namespaceService.findNamespaces(eq(appId), eq(Env.valueOf(env)), eq(clusterName)))
        .thenReturn(Collections.emptyList());
    when(appNamespaceService.findByAppId(eq(appId))).thenReturn(Collections.singletonList(missing));
    when(appNamespaceService.findByAppIdAndName(eq(appId), eq("application"))).thenReturn(missing);
    ResponseEntity<String> resp =
        restTemplate.postForEntity(missingNamespacesUrl(), null, String.class);
    assertEquals(HttpStatus.OK, resp.getStatusCode());
    verify(namespaceAPI, times(1)).createMissingAppNamespace(eq(Env.valueOf(env)),
        argThat(dto -> appId.equals(dto.getAppId()) && "application".equals(dto.getName())));
  }
}
