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

import com.google.gson.Gson;

import com.ctrip.framework.apollo.biz.service.AdminService;
import com.ctrip.framework.apollo.biz.service.AppService;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.entity.App;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ControllerIntegrationExceptionTest extends AbstractControllerTest {

  @Autowired
  AppController appController;

  @Mock
  AdminService adminService;

  private Object realAdminService;

  @Autowired
  AppService appService;

  private static final Gson GSON = new Gson();

  @Before
  public void setUp() {
    realAdminService = ReflectionTestUtils.getField(appController, "adminService");

    ReflectionTestUtils.setField(appController, "adminService", adminService);
  }

  @After
  public void tearDown() throws Exception {
    ReflectionTestUtils.setField(appController, "adminService", realAdminService);
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateFailed() {
    AppDTO dto = generateSampleDTOData();

    when(adminService.createNewApp(any(App.class))).thenThrow(new RuntimeException("save failed"));

    try {
      restTemplate.postForEntity(url("/apps/"), dto, AppDTO.class);
    } catch (HttpStatusCodeException e) {
      @SuppressWarnings("unchecked")
      Map<String, String> attr = GSON.fromJson(e.getResponseBodyAsString(), Map.class);
      Assert.assertEquals("save failed", attr.get("message"));
    }
    App savedApp = appService.findOne(dto.getAppId());
    Assert.assertNull(savedApp);
  }

  private AppDTO generateSampleDTOData() {
    AppDTO dto = new AppDTO();
    dto.setAppId("someAppId");
    dto.setName("someName");
    dto.setOwnerName("someOwner");
    dto.setOwnerEmail("someOwner@ctrip.com");
    return dto;
  }
}
