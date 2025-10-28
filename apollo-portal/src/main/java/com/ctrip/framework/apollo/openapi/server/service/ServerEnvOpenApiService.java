/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.environment.Env;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerEnvOpenApiService implements EnvOpenApiService {

  private final PortalSettings portalSettings;

  public ServerEnvOpenApiService(PortalSettings portalSettings) {
    this.portalSettings = portalSettings;
  }

  @Override
  public List<String> getEnvs() {
    List<String> environments = new ArrayList<>();
    for (Env env : portalSettings.getActiveEnvs()) {
      environments.add(env.toString());
    }
    return environments;
  }
}
