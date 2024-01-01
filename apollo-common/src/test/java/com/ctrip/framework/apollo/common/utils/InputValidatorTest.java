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
package com.ctrip.framework.apollo.common.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class InputValidatorTest {

  @Test
  public void testValidClusterName() throws Exception {
    checkClusterName("some.cluster-_name.123", true);
    checkClusterName("some.cluster-_name.123.yml", true);
    checkClusterName("some.&.name", false);
    checkClusterName("", false);
    checkClusterName(null, false);
    checkClusterName(".",false);
  }

  @Test
  public void testValidAppNamespaceName() throws Exception {
    checkAppNamespaceName("some.cluster-_name.123", true);
    checkAppNamespaceName("some.&.name", false);
    checkAppNamespaceName("", false);
    checkAppNamespaceName(null, false);
    checkAppNamespaceName("some.name.json", false);
    checkAppNamespaceName("some.name.yml", false);
    checkAppNamespaceName("some.name.yaml", false);
    checkAppNamespaceName("some.name.xml", false);
    checkAppNamespaceName("some.name.properties", false);
    checkAppNamespaceName("..xml", false);
  }

  private void checkClusterName(String name, boolean valid) {
    assertEquals(valid, InputValidator.isValidClusterNamespace(name));
  }

  private void checkAppNamespaceName(String name, boolean valid) {
    assertEquals(valid, InputValidator.isValidAppNamespace(name));
  }
}
