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
package com.ctrip.framework.apollo.portal.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class RoleUtilsTest {

  @Test
  public void testExtractAppIdFromMasterRoleName() throws Exception {
    assertEquals("someApp", RoleUtils.extractAppIdFromMasterRoleName("Master+someApp"));
    assertEquals("someApp", RoleUtils.extractAppIdFromMasterRoleName("Master+someApp+xx"));


    assertNull(RoleUtils.extractAppIdFromMasterRoleName("ReleaseNamespace+app1+application"));
  }

  @Test
  public void testExtractAppIdFromRoleName() throws Exception {
    assertEquals("someApp", RoleUtils.extractAppIdFromRoleName("Master+someApp"));
    assertEquals("someApp", RoleUtils.extractAppIdFromRoleName("ModifyNamespace+someApp+xx"));
    assertEquals("app1", RoleUtils.extractAppIdFromRoleName("ReleaseNamespace+app1+application"));
  }

  @Test
  public void testBuildNamespaceTargetID() throws Exception {
    assertEquals("app1+application",RoleUtils.buildNamespaceTargetId("app1", "application"));
  }

  @Test
  public void testBuildNamespaceTargetIDWithEnv() throws Exception {
    assertEquals("app1+LOCAL+application", RoleUtils.buildNamespaceTargetId("app1", "application", "LOCAL"));
      assertEquals("test-app+DEV+config", RoleUtils.buildNamespaceTargetId("test-app", "config", "DEV"));
  }

  @Test
  public void testBuildClusterTargetId() throws Exception {
    assertEquals("app1+LOCAL+default", RoleUtils.buildClusterTargetId("app1", "LOCAL", "default"));
    assertEquals("test-app+DEV+cluster1", RoleUtils.buildClusterTargetId("test-app", "DEV", "cluster1"));
  }
}
