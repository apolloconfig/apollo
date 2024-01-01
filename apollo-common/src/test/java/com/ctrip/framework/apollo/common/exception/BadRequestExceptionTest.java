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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/22
 */
public class BadRequestExceptionTest {

  private static final String appId = "app-1001";
  private static final String clusterName = "test";
  private static final String namespaceName = "application";

  @Test
  public void testItemAlreadyExists() {
    BadRequestException itemAlreadyExists = BadRequestException.itemAlreadyExists("itemKey");
    assertEquals("item already exists for itemKey:itemKey", itemAlreadyExists.getMessage());
  }

  @Test
  public void testItemNotExists() {
    BadRequestException itemNotExists = BadRequestException.itemNotExists(1001);
    assertEquals("item not exists for itemId:1001", itemNotExists.getMessage());
  }

  @Test
  public void testNamespaceNotExists() {
    BadRequestException namespaceNotExists = BadRequestException.namespaceNotExists();
    assertEquals("namespace not exist.", namespaceNotExists.getMessage());

    BadRequestException namespaceNotExists2 = BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    assertEquals("namespace not exist for appId:app-1001 clusterName:test namespaceName:application", namespaceNotExists2.getMessage());
  }

  @Test
  public void testNamespaceAlreadyExists() {
    BadRequestException namespaceAlreadyExists = BadRequestException.namespaceAlreadyExists(namespaceName);
    assertEquals("namespace already exists for namespaceName:application", namespaceAlreadyExists.getMessage());
  }

  @Test
  public void testAppNamespaceNotExists() {
    BadRequestException appNamespaceNotExists = BadRequestException.appNamespaceNotExists(appId, namespaceName);
    assertEquals("appNamespace not exist for appId:app-1001 namespaceName:application", appNamespaceNotExists.getMessage());
  }

  @Test
  public void testAppNamespaceAlreadyExists() {
    BadRequestException appNamespaceAlreadyExists = BadRequestException.appNamespaceAlreadyExists(appId, namespaceName);
    assertEquals("appNamespace already exists for appId:app-1001 namespaceName:application", appNamespaceAlreadyExists.getMessage());
  }

  @Test
  public void testInvalidNamespaceFormat() {
    BadRequestException invalidNamespaceFormat = BadRequestException.invalidNamespaceFormat("format");
    assertEquals("invalid namespace format:format", invalidNamespaceFormat.getMessage());
  }

  @Test
  public void testInvalidNotificationsFormat() {
    BadRequestException invalidNotificationsFormat = BadRequestException.invalidNotificationsFormat("format");
    assertEquals("invalid notifications format:format", invalidNotificationsFormat.getMessage());
  }

  @Test
  public void testInvalidClusterNameFormat() {
    BadRequestException invalidClusterNameFormat = BadRequestException.invalidClusterNameFormat("format");
    assertEquals("invalid clusterName format:format", invalidClusterNameFormat.getMessage());
  }

  @Test
  public void testInvalidRoleTypeFormat() {
    BadRequestException invalidRoleTypeFormat = BadRequestException.invalidRoleTypeFormat("format");
    assertEquals("invalid roleType format:format", invalidRoleTypeFormat.getMessage());
  }

  @Test
  public void testInvalidEnvFormat() {
    BadRequestException invalidEnvFormat = BadRequestException.invalidEnvFormat("format");
    assertEquals("invalid env format:format", invalidEnvFormat.getMessage());
  }

  @Test
  public void testNamespaceNotMatch(){
    BadRequestException namespaceNotMatch = BadRequestException.namespaceNotMatch();
    assertEquals("invalid request, item and namespace do not match!", namespaceNotMatch.getMessage());
  }

  @Test
  public void testAppNotExists(){
    BadRequestException appNotExists = BadRequestException.appNotExists(appId);
    assertEquals("app not exists for appId:app-1001", appNotExists.getMessage());
  }

  @Test
  public void testAppAlreadyExists(){
    BadRequestException appAlreadyExists = BadRequestException.appAlreadyExists(appId);
    assertEquals("app already exists for appId:app-1001", appAlreadyExists.getMessage());
  }

  @Test
  public void testClusterNotExists(){
    BadRequestException clusterNotExists = BadRequestException.clusterNotExists(clusterName);
    assertEquals("cluster not exists for clusterName:test", clusterNotExists.getMessage());
  }

  @Test
  public void testClusterAlreadyExists(){
    BadRequestException clusterAlreadyExists = BadRequestException.clusterAlreadyExists(clusterName);
    assertEquals("cluster already exists for clusterName:test", clusterAlreadyExists.getMessage());
  }

  @Test
  public void testUserNotExists(){
    BadRequestException userNotExists = BadRequestException.userNotExists("user");
    assertEquals("user not exists for userName:user", userNotExists.getMessage());
  }

  @Test
  public void testUserAlreadyExists(){
    BadRequestException userAlreadyExists = BadRequestException.userAlreadyExists("user");
    assertEquals("user already exists for userName:user", userAlreadyExists.getMessage());
  }

  @Test
  public void testUserAlreadyAuthorized(){
    BadRequestException userAlreadyAuthorized = BadRequestException.userAlreadyAuthorized("user");
    assertEquals("user already authorized", userAlreadyAuthorized.getMessage());
  }

  @Test
  public void testAccessKeyNotExists(){
    BadRequestException accessKeyNotExists = BadRequestException.accessKeyNotExists();
    assertEquals("accessKey not exist.", accessKeyNotExists.getMessage());
  }

}
