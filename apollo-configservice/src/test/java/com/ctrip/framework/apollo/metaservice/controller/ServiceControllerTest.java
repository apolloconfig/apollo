package com.ctrip.framework.apollo.metaservice.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.metaservice.service.DiscoveryService;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceControllerTest {

  @Mock
  private DiscoveryService discoveryService;

  @Mock
  private List<ServiceDTO> someServices;

  private ServiceController serviceController;

  @Before
  public void setUp() throws Exception {
    serviceController = new ServiceController(discoveryService);
  }

  @Test
  public void testGetMetaService() {
    assertTrue(serviceController.getMetaService().isEmpty());
  }

  @Test
  public void testGetConfigService() {
    String someAppId = "someAppId";
    String someClientIp = "someClientIp";

    when(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE))
        .thenReturn(someServices);

    assertEquals(someServices, serviceController.getConfigService(someAppId, someClientIp));
  }

  @Test
  public void testGetAdminService() {
    when(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_ADMINSERVICE))
        .thenReturn(someServices);

    assertEquals(someServices, serviceController.getAdminService());

  }
}