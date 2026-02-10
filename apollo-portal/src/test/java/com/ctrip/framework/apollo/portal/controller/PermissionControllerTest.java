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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import com.ctrip.framework.apollo.portal.entity.vo.ClusterNamespaceRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Collections;

@ActiveProfiles("skipAuthorization")
public class PermissionControllerTest extends AbstractIntegrationTest {

  private final String appId = "testApp";
  private final String env = "LOCAL";
  private final String clusterName = "testCluster";
  private final String roleType = "ModifyNamespacesInCluster";
  private final String user = "apollo";

  @Autowired
  RoleInitializationService roleInitializationService;

  @Before
  public void setUp() {
    roleInitializationService.initClusterNamespaceRoles(appId, env, clusterName, "apollo");
    Authentication auth = new UsernamePasswordAuthenticationToken("test-user", null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testClusterNamespaceRoleLifeCycle() {

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(user, headers);

    // check role not assigned
    ResponseEntity<ClusterNamespaceRolesAssignedUsers> beforeAssign = restTemplate.getForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_role_users"),
        ClusterNamespaceRolesAssignedUsers.class, appId, env, clusterName);
    assertEquals(200, beforeAssign.getStatusCodeValue());
    ClusterNamespaceRolesAssignedUsers body = beforeAssign.getBody();
    assertNotNull(body);
    assertEquals(appId, body.getAppId());
    assertEquals(env, body.getEnv());
    assertEquals(clusterName, body.getCluster());
    assertTrue(body.getModifyRoleUsers() == null || body.getModifyRoleUsers().isEmpty());

    // assign role to user
    restTemplate.postForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}"), entity,
        Void.class, appId, env, clusterName, roleType);

    // check role assigned
    ResponseEntity<ClusterNamespaceRolesAssignedUsers> afterAssign = restTemplate.getForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_role_users"),
        ClusterNamespaceRolesAssignedUsers.class, appId, env, clusterName);
    assertEquals(200, afterAssign.getStatusCodeValue());
    body = afterAssign.getBody();
    assertNotNull(body);
    assertEquals(appId, body.getAppId());
    assertEquals(env, body.getEnv());
    assertEquals(clusterName, body.getCluster());
    assertTrue(
        body.getModifyRoleUsers().stream().anyMatch(userInfo -> userInfo.getUserId().equals(user)));

    // remove role from user
    restTemplate.delete(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}?user={user}"),
        appId, env, clusterName, roleType, user);

    // check role removed
    ResponseEntity<ClusterNamespaceRolesAssignedUsers> afterRemove = restTemplate.getForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_role_users"),
        ClusterNamespaceRolesAssignedUsers.class, appId, env, clusterName);
    assertEquals(200, afterRemove.getStatusCodeValue());
    body = afterRemove.getBody();
    assertNotNull(body);
    assertEquals(appId, body.getAppId());
    assertEquals(env, body.getEnv());
    assertEquals(clusterName, body.getCluster());
    assertTrue(body.getModifyRoleUsers() == null || body.getModifyRoleUsers().isEmpty());
  }

  /**
   * Verify that env name aliases (e.g. "prod", "PROD") are normalized to the canonical form "PRO"
   * so that role lookup and permission check remain consistent.
   *
   * @see <a href="https://github.com/apolloconfig/apollo/issues/5442">#5442</a>
   */
  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testEnvNameNormalizationForClusterRoles() {
    // Roles were initialized with env = "LOCAL" in setUp().
    // Querying with lowercase "local" should still resolve to the same roles.
    ResponseEntity<ClusterNamespaceRolesAssignedUsers> response = restTemplate.getForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_role_users"),
        ClusterNamespaceRolesAssignedUsers.class, appId, "local", clusterName);
    assertEquals(200, response.getStatusCodeValue());
    ClusterNamespaceRolesAssignedUsers body = response.getBody();
    assertNotNull(body);
    // The returned env should be the normalized form "LOCAL", not the raw input "local"
    assertEquals("LOCAL", body.getEnv());
  }

  /**
   * Verify that "prod" is normalized to "PRO" (the canonical env name in Apollo)
   * when assigning and querying cluster namespace roles.
   *
   * @see <a href="https://github.com/apolloconfig/apollo/issues/5442">#5442</a>
   */
  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testProdEnvNormalizationForClusterRoles() {
    // Initialize roles with canonical env name "PRO"
    roleInitializationService.initClusterNamespaceRoles(appId, "PRO", clusterName, "apollo");

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(user, headers);

    // Assign role using "prod" (alias for "PRO")
    restTemplate.postForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}"), entity,
        Void.class, appId, "prod", clusterName, roleType);

    // Query using "PROD" (another alias) — should still find the assigned role
    ResponseEntity<ClusterNamespaceRolesAssignedUsers> response = restTemplate.getForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_role_users"),
        ClusterNamespaceRolesAssignedUsers.class, appId, "PROD", clusterName);
    assertEquals(200, response.getStatusCodeValue());
    ClusterNamespaceRolesAssignedUsers body = response.getBody();
    assertNotNull(body);
    assertEquals("PRO", body.getEnv());
    assertTrue(
        body.getModifyRoleUsers().stream().anyMatch(userInfo -> userInfo.getUserId().equals(user)));
  }
}
