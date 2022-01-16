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
package com.ctrip.framework.foundation.internals.provider;

import com.ctrip.framework.foundation.internals.NetworkInterfaceManager;
import com.ctrip.framework.foundation.spi.provider.NetworkProvider;
import com.ctrip.framework.foundation.spi.provider.Provider;

public class DefaultNetworkProvider implements NetworkProvider {
  @Override
  public String getProperty(String name, String defaultValue) {
    if ("host.address".equalsIgnoreCase(name)) {
      String val = getHostAddress();
      return val == null ? defaultValue : val;
    }
    if ("host.name".equalsIgnoreCase(name)) {
      String val = getHostName();
      return val == null ? defaultValue : val;
    }
    return defaultValue;
  }

  @Override
  public void initialize() {}

  @Override
  public String getHostAddress() {
    return NetworkInterfaceManager.INSTANCE.getLocalHostAddress();
  }

  @Override
  public String getHostName() {
    return NetworkInterfaceManager.INSTANCE.getLocalHostName();
  }

  @Override
  public Class<? extends Provider> getType() {
    return NetworkProvider.class;
  }

  @Override
  public String toString() {
    return "hostName ["
        + getHostName()
        + "] hostIP ["
        + getHostAddress()
        + "] (DefaultNetworkProvider)";
  }
}
