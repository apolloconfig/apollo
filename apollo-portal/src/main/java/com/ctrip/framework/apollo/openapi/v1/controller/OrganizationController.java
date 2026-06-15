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
package com.ctrip.framework.apollo.openapi.v1.controller;

import com.ctrip.framework.apollo.openapi.api.OrganizationManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenOrganizationDto;
import com.ctrip.framework.apollo.openapi.server.service.OrganizationOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenOperation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

@RestController("openapiOrganizationController")
public class OrganizationController implements OrganizationManagementApi {

  private final OrganizationOpenApiService organizationOpenApiService;
  private final UnifiedPermissionValidator unifiedPermissionValidator;

  public OrganizationController(OrganizationOpenApiService organizationOpenApiService,
      UnifiedPermissionValidator unifiedPermissionValidator) {
    this.organizationOpenApiService = organizationOpenApiService;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
  }

  @Override
  public ResponseEntity<List<OpenOrganizationDto>> getOrganization() {
    requireMetadataReadPermissionForUserToken();
    return ResponseEntity.ok(organizationOpenApiService.getOrganizations());
  }

  private void requireMetadataReadPermissionForUserToken() {
    if (!unifiedPermissionValidator.hasAnyUserTokenOperation(UserTokenOperation.METADATA_READ)) {
      throw new AccessDeniedException("Metadata read permission is required");
    }
  }
}
