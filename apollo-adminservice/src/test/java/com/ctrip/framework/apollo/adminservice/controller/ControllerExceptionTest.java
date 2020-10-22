package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.service.AdminService;
import com.ctrip.framework.apollo.biz.service.AppService;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ControllerExceptionTest {

  private AppController appController;

  @Mock
  private AppService appService;

  @Mock
  private AdminService adminService;

  @Before
  public void setUp() {
    appController = new AppController(appService, adminService);
  }

  @Test(expected = NotFoundException.class)
  public void testFindNotExists() {
    when(appService.findOne(any(String.class))).thenReturn(null);
    appController.get("unexist");
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteNotExists() {
    when(appService.findOne(any(String.class))).thenReturn(null);
    appController.delete("unexist", null);
  }

  @Test
  public void testFindEmpty() {
    when(appService.findAll(any(Pageable.class))).thenReturn(new ArrayList<>());
    Pageable pageable = PageRequest.of(0, 10);
    List<AppDTO> appDTOs = appController.find(null, pageable);
    Assert.assertNotNull(appDTOs);
    Assert.assertEquals(0, appDTOs.size());

    appDTOs = appController.find("", pageable);
    Assert.assertNotNull(appDTOs);
    Assert.assertEquals(0, appDTOs.size());
  }

  @Test
  public void testFindByName() {
    Pageable pageable = PageRequest.of(0, 10);
    List<AppDTO> appDTOs = appController.find("unexist", pageable);
    Assert.assertNotNull(appDTOs);
    Assert.assertEquals(0, appDTOs.size());
  }

  @Test(expected = ServiceException.class)
  public void createFailed() {
    AppDTO dto = generateSampleDTOData();

    when(appService.findOne(any(String.class))).thenReturn(null);
    when(adminService.createNewApp(any(App.class)))
        .thenThrow(new ServiceException("create app failed"));

    appController.create(dto);
  }

  private AppDTO generateSampleDTOData() {
    AppDTO dto = new AppDTO();
    dto.setAppId("someAppId");
    dto.setName("someName");
    dto.setOwnerName("someOwner");
    dto.setOwnerEmail("someOwner@ctrip.com");
    dto.setDataChangeLastModifiedBy("test");
    dto.setDataChangeCreatedBy("test");
    return dto;
  }
}
