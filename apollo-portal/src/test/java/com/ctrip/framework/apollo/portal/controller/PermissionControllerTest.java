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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

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
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}"), entity, Void.class,
        appId, env, clusterName, roleType);

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
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/ns_roles/{roleType}?user={user}"), appId,
        env, clusterName, roleType, user);

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
}
