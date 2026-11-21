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
package com.ctrip.framework.apollo.biz.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

public class AppNamespaceServiceTest extends AbstractIntegrationTest {

  // an AppNamespace with namespaces in two clusters, from namespace-test.sql
  private static final String APP_ID = "testApp";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String OPERATOR = "apollo";

  @Autowired
  private AppNamespaceService appNamespaceService;
  @Autowired
  private NamespaceService namespaceService;

  @Test
  @Sql(scripts = "/sql/namespace-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteAppNamespace() {
    AppNamespace appNamespace = appNamespaceService.findOne(APP_ID, NAMESPACE);

    appNamespaceService.deleteAppNamespace(appNamespace, OPERATOR);

    assertNull(appNamespaceService.findOne(APP_ID, NAMESPACE));
    assertEquals(0, namespaceService.findByAppIdAndNamespaceName(APP_ID, NAMESPACE).size());
  }

  @Test
  @Sql(scripts = "/sql/namespace-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteAppNamespaceKeepsOneRecreatedAfterItWasRead() {
    AppNamespace read = appNamespaceService.findOne(APP_ID, NAMESPACE);
    // another caller deletes it and creates it again before this deletion starts
    appNamespaceService.deleteAppNamespace(read, OPERATOR);
    AppNamespace recreated = new AppNamespace();
    recreated.setAppId(APP_ID);
    recreated.setName(NAMESPACE);
    recreated.setFormat("properties");
    recreated.setDataChangeCreatedBy(OPERATOR);
    recreated = appNamespaceService.createAppNamespace(recreated);

    try {
      appNamespaceService.deleteAppNamespace(read, OPERATOR);
      fail("Should throw");
    } catch (BadRequestException e) {
      assertEquals("appNamespace not exist for appId:testApp namespaceName:application",
          e.getMessage());
    }

    assertEquals(recreated.getId(), appNamespaceService.findOne(APP_ID, NAMESPACE).getId());
    assertNotNull(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE));
  }
}
