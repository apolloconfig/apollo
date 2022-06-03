package com.ctrip.framework.apollo.spi;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author wxq
 */
public class RandomConfigServiceLoadBalancerClientTest {

  @Test
  public void chooseOneFrom() {
    ConfigServiceLoadBalancerClient loadBalancerClient = new RandomConfigServiceLoadBalancerClient();
    List<ServiceDTO> configServices = generateConfigServices();
    for (int i = 0; i < 100; i++) {
      ServiceDTO serviceDTO = loadBalancerClient.chooseOneFrom(configServices);
      // always contains it
      assertTrue(configServices.contains(serviceDTO));
    }
  }

  private static List<ServiceDTO> generateConfigServices() {
    List<ServiceDTO> configServices = new ArrayList<>();
    {
      ServiceDTO serviceDTO = new ServiceDTO();
      serviceDTO.setAppName("appName1");
      configServices.add(serviceDTO);
    }
    {
      ServiceDTO serviceDTO = new ServiceDTO();
      serviceDTO.setAppName("appName2");
      configServices.add(serviceDTO);
    }
    {
      ServiceDTO serviceDTO = new ServiceDTO();
      serviceDTO.setAppName("appName3");
      configServices.add(serviceDTO);
    }
    return configServices;
  }
}