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
import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.internals.ConfigServiceLocator;
import java.util.List;

public interface ConfigServiceLoadBalancerClient extends Ordered {

  /**
   * choose 1 config service from multiple service instances
   *
   * @param configServices the return of {@link ConfigServiceLocator#getConfigServices()}
   * @return return 1 config service chosen, null if there is no unavailable config service
   * @throws IllegalArgumentException if arg is null of empty
   */
  ServiceDTO chooseOneFrom(List<ServiceDTO> configServices);
}
