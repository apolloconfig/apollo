/*
 * Copyright 2023 Apollo Authors
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

import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertEquals(e1.getMessage(), e2.getMessage());
  }

  @Test
  public void testAppNotFountException() {
    AppNotFountException exception = new AppNotFountException(appId);
    Assert.assertEquals(exception.getMessage(), "app not found for appId:app-1001");
  }

  @Test
  public void testClusterNotFountException() {
    ClusterNotFoundException exception = new ClusterNotFoundException(appId, clusterName);
    Assert.assertEquals(exception.getMessage(), "cluster not found for appId:app-1001 clusterName:test");
  }

  @Test
  public void testNamespaceNotFountException() {
    NamespaceNotFoundException exception = new NamespaceNotFoundException(appId, clusterName, namespaceName);
    Assert.assertEquals(exception.getMessage(), "namespace not found for appId:app-1001 clusterName:test namespaceName:application");

    exception = new NamespaceNotFoundException(66);
    Assert.assertEquals(exception.getMessage(), "namespace not found for namespaceId:66");
  }

  @Test
  public void testReleaseNotFountException() {
    ReleaseNotFoundException exception = new ReleaseNotFoundException(66);
    Assert.assertEquals(exception.getMessage(), "release not found for releaseId:66");
  }

}