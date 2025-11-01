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

import com.ctrip.framework.apollo.openapi.model.OpenOrganizationDto;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServerOrganizationOpenApiService implements OrganizationOpenApiService {

  private final PortalConfig portalConfig;

  public ServerOrganizationOpenApiService(PortalConfig portalConfig) {
    this.portalConfig = portalConfig;
  }

  @Override
  public List<OpenOrganizationDto> getOrganizations() {
    return OpenApiModelConverters.fromOrganizations(portalConfig.organizations());
  }
}
