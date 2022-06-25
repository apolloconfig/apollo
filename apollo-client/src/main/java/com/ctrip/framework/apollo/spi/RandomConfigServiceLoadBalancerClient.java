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

import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * default service provider of {@link ConfigServiceLoadBalancerClient}
 */
public class RandomConfigServiceLoadBalancerClient implements ConfigServiceLoadBalancerClient {

  private static final int ORDER = 0;

  @Override
  public ServiceDTO chooseOneFrom(List<ServiceDTO> configServices) {
    if (null == configServices) {
      throw new IllegalArgumentException("arg is null");
    }
    if (configServices.isEmpty()) {
      throw new IllegalArgumentException("arg is empty");
    }
    int index = ThreadLocalRandom.current().nextInt(configServices.size());
    return configServices.get(index);
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
