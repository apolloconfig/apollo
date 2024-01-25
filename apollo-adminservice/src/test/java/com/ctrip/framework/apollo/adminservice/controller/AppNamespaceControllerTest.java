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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.repository.AppNamespaceRepository;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

public class AppNamespaceControllerTest extends AbstractControllerTest{

  @Autowired
  private AppNamespaceRepository namespaceRepository;

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreate(){
    String appId = "6666";
    String name = "testnamespace";
    String comment = "comment";
    AppNamespaceDTO dto = new AppNamespaceDTO();
    dto.setAppId(appId);
    dto.setName(name);
    dto.setComment(comment);
    dto.setDataChangeCreatedBy("apollo");

    AppNamespaceDTO resultDto = restTemplate.postForEntity(url("/apps/{appId}/appnamespaces"), dto, AppNamespaceDTO.class, appId).getBody();

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
}
