package com.ctrip.framework.apollo.metaservice.controller;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.metaservice.service.DiscoveryService;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HomePageControllerTest {

  @Mock
  private DiscoveryService discoveryService;

  private HomePageController homePageController;

  @Before
  public void setUp() throws Exception {
    homePageController = new HomePageController(discoveryService);
  }

  @Test
  public void testListAllServices() {
    ServiceDTO someServiceDto = mock(ServiceDTO.class);
    ServiceDTO anotherServiceDto = mock(ServiceDTO.class);

    when(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE)).thenReturn(
        Lists.newArrayList(someServiceDto));
    when(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_ADMINSERVICE)).thenReturn(
        Lists.newArrayList(anotherServiceDto));

    List<ServiceDTO> allServices = homePageController.listAllServices();

    assertEquals(2, allServices.size());
    assertSame(someServiceDto, allServices.get(0));
    assertSame(anotherServiceDto, allServices.get(1));
  }
}