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
package com.ctrip.framework.apollo.openapi.service;

import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConsumerRolePermissionServiceTest extends AbstractIntegrationTest {
  @Autowired
  private ConsumerRolePermissionService consumerRolePermissionService;

  @Before
  public void setUp() throws Exception {

  }

  @Test
  @Sql(scripts = "/sql/permission/insert-test-roles.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/permission/insert-test-permissions.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/permission/insert-test-consumerroles.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/permission/insert-test-rolepermissions.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testConsumerHasPermission() throws Exception {
    String someTargetId = "someTargetId";
    String anotherTargetId = "anotherTargetId";
    String somePermissionType = "somePermissionType";
    String anotherPermissionType = "anotherPermissionType";
    long someConsumerId = 1;
    long anotherConsumerId = 2;
    long someConsumerWithNoPermission = 3;

    assertTrue(consumerRolePermissionService.consumerHasPermission(someConsumerId, somePermissionType, someTargetId));
    assertTrue(consumerRolePermissionService.consumerHasPermission(someConsumerId, anotherPermissionType, anotherTargetId));
    assertTrue(consumerRolePermissionService.consumerHasPermission(anotherConsumerId, somePermissionType, someTargetId));
    assertTrue(consumerRolePermissionService.consumerHasPermission(anotherConsumerId, anotherPermissionType, anotherTargetId));

    assertFalse(consumerRolePermissionService.consumerHasPermission(someConsumerWithNoPermission, somePermissionType, someTargetId));
    assertFalse(consumerRolePermissionService.consumerHasPermission(someConsumerWithNoPermission, anotherPermissionType, anotherTargetId));

  }

  /**
   * Test when user has no roles, should return empty permission set
   */
  @Test
  @Sql(scripts = {
          "/sql/permission/consumer_role_permission_service/test_get_user_permission_set_no_roles.sql"
  }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testGetUserPermissionSet_WhenNoRoles_ShouldReturnEmptySet() {
    // Corresponds to the user without roles in the test script
    long consumerId = 4;
    Set<String> result = consumerRolePermissionService.getUserPermissionSet(consumerId);
    assertTrue("Should return empty set when user has no roles", result.isEmpty());
  }

  /**
   * Test when user has roles but roles have no permissions, should return empty permission set
   */
  @Test
  @Sql(scripts = {
          "/sql/permission/consumer_role_permission_service/test_get_user_permission_set_roles_without_permissions.sql"
  }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testGetUserPermissionSet_WhenRolesButNoPermissions_ShouldReturnEmptySet() {
    // Corresponds to the user with roles but no permissions in the test script
    long consumerId = 5;
    Set<String> result = consumerRolePermissionService.getUserPermissionSet(consumerId);
    assertTrue("Should return empty set when user roles have no permissions", result.isEmpty());
  }

  /**
   * Test when user is associated with multiple roles and permissions, should return correct permission set
   */
  @Test
  @Sql(scripts = {
          "/sql/permission/consumer_role_permission_service/test_get_user_permission_set_with_permissions.sql"
  }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testGetUserPermissionSet_WhenHasPermissions_ShouldReturnCorrectSet() {
    // Corresponds to the user with permissions in the test script
    long consumerId = 6;
    Set<String> result = consumerRolePermissionService.getUserPermissionSet(consumerId);

    Set<String> expected = new HashSet<>(Lists.newArrayList("app:app1", "namespace:ns1", "cluster:cluster1"));

    assertEquals("Permission sets do not match", expected, result);
  }

  /**
   * Test that different users have isolated permission sets
   */
  @Test
  @Sql(scripts = {
          "/sql/permission/consumer_role_permission_service/test_get_user_permission_set_different_users.sql"
  }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testGetUserPermissionSet_DifferentUsersHaveIsolatedPermissions() {
    long consumerId1 = 7;
    long consumerId2 = 8;

    Set<String> permissions1 = consumerRolePermissionService.getUserPermissionSet(consumerId1);
    Set<String> permissions2 = consumerRolePermissionService.getUserPermissionSet(consumerId2);

    assertEquals("User 1 should have 1 permission", 1, permissions1.size());
    assertTrue("User 1 should contain app:app2", permissions1.contains("app:app2"));

    assertEquals("User 2 should have 2 permissions", 2, permissions2.size());
    assertTrue("User 2 should contain namespace:ns2", permissions2.contains("namespace:ns2"));
    assertTrue("User 2 should contain cluster:cluster2", permissions2.contains("cluster:cluster2"));
  }

}