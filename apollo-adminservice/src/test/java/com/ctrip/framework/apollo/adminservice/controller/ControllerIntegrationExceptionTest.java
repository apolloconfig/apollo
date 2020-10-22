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
import org.mockito.MockitoAnnotations;
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
    MockitoAnnotations.initMocks(this);

    realAdminService = ReflectionTestUtils.getField(appController, "adminService");

    ReflectionTestUtils.setField(appController, "adminService", adminService);
  }

  @After
  public void tearDown() throws Exception {
    ReflectionTestUtils.setField(appController, "adminService", realAdminService);
  }

  private String getBaseAppUrl() {
    return "http://localhost:" + port + "/apps/";
  }

  @Test
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateFailed() {
    AppDTO dto = generateSampleDTOData();

    when(adminService.createNewApp(any(App.class))).thenThrow(new RuntimeException("save failed"));

    try {
      restTemplate.postForEntity(getBaseAppUrl(), dto, AppDTO.class);
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
