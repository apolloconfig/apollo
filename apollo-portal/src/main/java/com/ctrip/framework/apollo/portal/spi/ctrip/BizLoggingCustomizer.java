/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.portal.spi.ctrip;

import com.ctrip.framework.apollo.common.customize.LoggingCustomizer;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ctrip")
public class BizLoggingCustomizer extends LoggingCustomizer {

  private final PortalConfig portalConfig;

  public BizLoggingCustomizer(final PortalConfig portalConfig) {
    this.portalConfig = portalConfig;
  }

  @Override
  protected String cloggingUrl() {
    return portalConfig.cloggingUrl();
  }

  @Override
  protected String cloggingPort() {
    return portalConfig.cloggingPort();
  }
}
