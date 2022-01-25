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

import java.io.InputStream;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.foundation.spi.provider.ApplicationProvider;
import com.ctrip.framework.foundation.spi.provider.NetworkProvider;
import com.ctrip.framework.foundation.spi.provider.Provider;
import com.ctrip.framework.foundation.spi.provider.ServerProvider;

public class NullProvider implements ApplicationProvider, NetworkProvider, ServerProvider {
  @Override
  public Class<? extends Provider> getType() {
    return null;
  }

  @Override
  public String getProperty(String name, String defaultValue) {
    return defaultValue;
  }

  @Override
  public void initialize() {

  }

  @Override
  public String getAppId() {
    return null;
  }

  @Override
  public String getApolloLabel() {
    return null;
  }

  @Override
  public String getAccessKeySecret() {
    return null;
  }

  @Override
  public boolean isAppIdSet() {
    return false;
  }

  @Override
  public String getEnvType() {
    return null;
  }

  @Override
  public boolean isEnvTypeSet() {
    return false;
  }

  @Override
  public String getDataCenter() {
    return null;
  }

  @Override
  public boolean isDataCenterSet() {
    return false;
  }

  @Override
  public void initialize(InputStream in) {

  }

  @Override
  public String getHostAddress() {
    return null;
  }

  @Override
  public String getHostName() {
    return null;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public String toString() {
    return "(NullProvider)";
  }
}
