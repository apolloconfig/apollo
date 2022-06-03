/*
 * Copyright 2022 Apollo Authors
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