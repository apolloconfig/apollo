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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import java.util.ArrayList;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

class ConfigServiceLoadBalancerClientTest {

  /**
   * all {@link ConfigServiceLoadBalancerClient}'s implementations need to conform it.
   */
  @Test
  void chooseOneFrom() {
    Iterator<ConfigServiceLoadBalancerClient> loadBalancerClientIterator =
        ServiceBootstrap.loadAll(ConfigServiceLoadBalancerClient.class);
    while (loadBalancerClientIterator.hasNext()) {
      ConfigServiceLoadBalancerClient configServiceLoadBalancerClient = loadBalancerClientIterator.next();
      expectException(configServiceLoadBalancerClient);
    }
  }

  private static void expectException(ConfigServiceLoadBalancerClient loadBalancerClient) {
    // arg is null
    assertThrows(IllegalArgumentException.class, () -> loadBalancerClient.chooseOneFrom(null));
    // arg is empty
    assertThrows(IllegalArgumentException.class,
        () -> loadBalancerClient.chooseOneFrom(new ArrayList<>()));
  }

  @Test
  void classTypeMatch() {
    ConfigServiceLoadBalancerClient loadBalancerClient =
        ServiceBootstrap.loadPrimary(ConfigServiceLoadBalancerClient.class);
    assertTrue(loadBalancerClient instanceof DefaultConfigServiceLoadBalancerClient);
  }
}