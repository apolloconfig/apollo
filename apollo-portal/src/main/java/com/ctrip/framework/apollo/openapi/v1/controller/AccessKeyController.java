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

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.openapi.api.AccessKeyManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenAccessKeyDTO;
import com.ctrip.framework.apollo.openapi.server.service.AccessKeyOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI v1 controller for access key management.
 */
@RestController("openapiAccessKeyController")
public class AccessKeyController implements AccessKeyManagementApi {

  private final AccessKeyOpenApiService accessKeyOpenApiService;
  private final OpenApiOperatorResolver operatorResolver;
  private final UnifiedPermissionValidator unifiedPermissionValidator;

  public AccessKeyController(AccessKeyOpenApiService accessKeyOpenApiService,
      OpenApiOperatorResolver operatorResolver,
      UnifiedPermissionValidator unifiedPermissionValidator) {
    this.accessKeyOpenApiService = accessKeyOpenApiService;
    this.operatorResolver = operatorResolver;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.CREATE, name = "AccessKey.create")
  public ResponseEntity<OpenAccessKeyDTO> createAccessKey(String appId, String env,
      String operator) {
    requireAccessKeyPermissionForUserToken(appId, env);
    return ResponseEntity.ok(
        accessKeyOpenApiService.createAccessKey(appId, env, operatorResolver.resolve(operator)));
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "AccessKey.delete")
  public ResponseEntity<Void> deleteAccessKey(String appId, String env, Long accessKeyId,
      String operator) {
    requireAccessKeyPermissionForUserToken(appId, env);
    accessKeyOpenApiService.deleteAccessKey(appId, env, accessKeyId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "AccessKey.disable")
  public ResponseEntity<Void> disableAccessKey(String appId, String env, Long accessKeyId,
      String operator) {
    requireAccessKeyPermissionForUserToken(appId, env);
    accessKeyOpenApiService.disableAccessKey(appId, env, accessKeyId,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "AccessKey.enable")
  public ResponseEntity<Void> enableAccessKey(String appId, String env, Long accessKeyId,
      Integer mode, String operator) {
    requireAccessKeyPermissionForUserToken(appId, env);
    accessKeyOpenApiService.enableAccessKey(appId, env, accessKeyId, mode,
        operatorResolver.resolve(operator));
    return ResponseEntity.ok().build();
  }

  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  public ResponseEntity<List<OpenAccessKeyDTO>> findAccessKeys(String appId, String env) {
    requireAccessKeyPermissionForUserToken(appId, env);
    return ResponseEntity.ok(accessKeyOpenApiService.findAccessKeys(appId, env));
  }

  private void requireAccessKeyPermissionForUserToken(String appId, String env) {
    if (UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        && !unifiedPermissionValidator.hasAssignRolePermission(appId, env, null, null)) {
      throw new AccessDeniedException("Assign role permission is required");
    }
  }
}
