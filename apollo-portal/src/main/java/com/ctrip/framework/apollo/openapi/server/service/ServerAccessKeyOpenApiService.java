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

import static com.ctrip.framework.apollo.common.constants.AccessKeyMode.FILTER;

import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.common.utils.UniqueKeyGenerator;
import com.ctrip.framework.apollo.openapi.model.OpenAccessKeyDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AccessKeyService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Default server-side OpenAPI implementation for access key management.
 */
@Service
public class ServerAccessKeyOpenApiService implements AccessKeyOpenApiService {

  private final AccessKeyService accessKeyService;

  public ServerAccessKeyOpenApiService(AccessKeyService accessKeyService) {
    this.accessKeyService = accessKeyService;
  }

  @Override
  public List<OpenAccessKeyDTO> findAccessKeys(String appId, String env) {
    return OpenApiModelConverters
        .fromAccessKeyDTOs(accessKeyService.findByAppId(Env.valueOf(env), appId));
  }

  @Override
  public OpenAccessKeyDTO createAccessKey(String appId, String env, String operator) {
    AccessKeyDTO accessKey = new AccessKeyDTO();
    accessKey.setAppId(appId);
    accessKey.setSecret(UniqueKeyGenerator.generateId());
    accessKey.setDataChangeCreatedBy(operator);
    accessKey.setDataChangeLastModifiedBy(operator);
    return OpenApiModelConverters
        .fromAccessKeyDTO(accessKeyService.createAccessKey(Env.valueOf(env), accessKey));
  }

  @Override
  public void deleteAccessKey(String appId, String env, Long accessKeyId, String operator) {
    accessKeyService.deleteAccessKey(Env.valueOf(env), appId, accessKeyId, operator);
  }

  @Override
  public void enableAccessKey(String appId, String env, Long accessKeyId, Integer mode,
      String operator) {
    int resolvedMode = mode == null ? FILTER : mode;
    accessKeyService.enable(Env.valueOf(env), appId, accessKeyId, resolvedMode, operator);
  }

  @Override
  public void disableAccessKey(String appId, String env, Long accessKeyId, String operator) {
    accessKeyService.disable(Env.valueOf(env), appId, accessKeyId, operator);
  }
}
