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
package com.ctrip.framework.apollo.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NotFoundExceptionTest {

  private static final String appId = "app-1001";
  private static final String clusterName = "test";
  private static final String namespaceName = "application";

  @Test
  public void testConstructor() {
    String key = "test.key";
    NotFoundException e1, e2;
    e1 = new NotFoundException("item not found for %s %s %s %s", appId,
        clusterName, namespaceName, key);
    e2 = new NotFoundException(
        String.format("item not found for %s %s %s %s", appId, clusterName, namespaceName, key));
    assertEquals(e1.getMessage(), e2.getMessage());
  }

  @Test
  public void testAppNotFoundException() {
    NotFoundException exception = NotFoundException.appNotFound(appId);
    assertEquals(exception.getMessage(), "app not found for appId:app-1001");
  }

  @Test
  public void testClusterNotFoundException() {
    NotFoundException exception = NotFoundException.clusterNotFound(appId, clusterName);
    assertEquals(exception.getMessage(), "cluster not found for appId:app-1001 clusterName:test");
  }

  @Test
  public void testNamespaceNotFoundException() {
    NotFoundException exception = NotFoundException.namespaceNotFound(appId, clusterName, namespaceName);
    assertEquals(exception.getMessage(), "namespace not found for appId:app-1001 clusterName:test namespaceName:application");

    exception = NotFoundException.namespaceNotFound(66);
    assertEquals(exception.getMessage(), "namespace not found for namespaceId:66");
  }

  @Test
  public void testReleaseNotFoundException() {
    NotFoundException exception = NotFoundException.releaseNotFound(66);
    assertEquals(exception.getMessage(), "release not found for releaseId:66");
  }

  @Test
  public void testItemNotFoundException(){
    NotFoundException exception = NotFoundException.itemNotFound(66);
    assertEquals(exception.getMessage(), "item not found for itemId:66");

    exception = NotFoundException.itemNotFound("test.key");
    assertEquals(exception.getMessage(), "item not found for itemKey:test.key");

    exception = NotFoundException.itemNotFound(appId, clusterName, namespaceName, "test.key");
    assertEquals(exception.getMessage(), "item not found for appId:app-1001 clusterName:test namespaceName:application itemKey:test.key");

    exception = NotFoundException.itemNotFound(appId, clusterName, namespaceName, 66);
    assertEquals(exception.getMessage(), "item not found for appId:app-1001 clusterName:test namespaceName:application itemId:66");
  }

  @Test
  void roleNotFound() {
    NotFoundException exception = NotFoundException.roleNotFound("CreateApplication+SystemRole");
    assertEquals(exception.getMessage(), "role not found for roleName:CreateApplication+SystemRole, please check apollo portal DB table 'Role'");
  }
}