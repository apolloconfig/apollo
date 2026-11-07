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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.repository.AppNamespaceRepository;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;

public class AppNamespaceControllerTest extends AbstractControllerTest {

  // an AppNamespace with a namespace in the cluster, from test-itemset.sql
  private static final String APP_ID = "someAppId";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "someNamespace";

  @Autowired
  private AppNamespaceRepository namespaceRepository;
  @Autowired
  private NamespaceService namespaceService;

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreate() {
    String appId = "6666";
    String name = "testnamespace";
    String comment = "comment";
    AppNamespaceDTO dto = new AppNamespaceDTO();
    dto.setAppId(appId);
    dto.setName(name);
    dto.setComment(comment);
    dto.setDataChangeCreatedBy("apollo");

    AppNamespaceDTO resultDto = restTemplate
        .postForEntity(url("/apps/{appId}/appnamespaces"), dto, AppNamespaceDTO.class, appId)
        .getBody();

    Assert.assertNotNull(resultDto);
    Assert.assertEquals(appId, resultDto.getAppId());
    Assert.assertTrue(resultDto.getId() > 0);

    AppNamespace savedAppNs = namespaceRepository.findByAppIdAndName(appId, name);
    Assert.assertNotNull(savedAppNs);
    Assert.assertNotNull(savedAppNs.getDataChangeCreatedTime());
    Assert.assertNotNull(savedAppNs.getDataChangeLastModifiedTime());
    Assert.assertNotNull(savedAppNs.getDataChangeLastModifiedBy());
    Assert.assertNotNull(savedAppNs.getDataChangeCreatedBy());
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql",
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteWithoutAppNamespaceId() {
    restTemplate.delete(url("/apps/{appId}/appnamespaces/{namespaceName}?operator={operator}"),
        APP_ID, NAMESPACE, "apollo");

    Assert.assertNull(namespaceRepository.findByAppIdAndName(APP_ID, NAMESPACE));
    Assert.assertNull(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE));
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql",
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteRefusesAppNamespaceRecreatedAfterItsIdWasRead() {
    long readId = namespaceRepository.findByAppIdAndName(APP_ID, NAMESPACE).getId();
    // another caller deletes the AppNamespace with that id, and it is created again
    restTemplate.delete(deleteByIdUrl(), APP_ID, NAMESPACE, "apollo", readId);
    Assert.assertNull(namespaceRepository.findByAppIdAndName(APP_ID, NAMESPACE));
    AppNamespaceDTO dto = new AppNamespaceDTO();
    dto.setAppId(APP_ID);
    dto.setName(NAMESPACE);
    dto.setDataChangeCreatedBy("apollo");
    AppNamespaceDTO recreated = restTemplate
        .postForEntity(url("/apps/{appId}/appnamespaces"), dto, AppNamespaceDTO.class, APP_ID)
        .getBody();

    try {
      restTemplate.delete(deleteByIdUrl(), APP_ID, NAMESPACE, "apollo", readId);
      Assert.fail("Should throw");
    } catch (HttpClientErrorException e) {
      Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Assert.assertTrue(e.getResponseBodyAsString().contains("it may have been recreated"));
    }

    Assert.assertEquals(recreated.getId(),
        namespaceRepository.findByAppIdAndName(APP_ID, NAMESPACE).getId());
    Assert.assertNotNull(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE));

    restTemplate.delete(deleteByIdUrl(), APP_ID, NAMESPACE, "apollo", recreated.getId());

    Assert.assertNull(namespaceRepository.findByAppIdAndName(APP_ID, NAMESPACE));
    Assert.assertNull(namespaceService.findOne(APP_ID, CLUSTER, NAMESPACE));
  }

  private String deleteByIdUrl() {
    return url("/apps/{appId}/appnamespaces/{namespaceName}?operator={operator}"
        + "&appNamespaceId={appNamespaceId}");
  }
}
