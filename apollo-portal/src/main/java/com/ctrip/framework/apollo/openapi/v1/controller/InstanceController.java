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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.api.InstanceManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController("openapiInstanceController")
public class InstanceController implements InstanceManagementApi {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_INSTANCE_PAGE_SIZE = 20;
  private static final Splitter RELEASE_ID_SPLITTER =
      Splitter.on(',').trimResults().omitEmptyStrings();

  private final InstanceService instanceService;

  public InstanceController(final InstanceService instanceService) {
    this.instanceService = instanceService;
  }

  @Override
  public ResponseEntity<OpenInstancePageDTO> getByNamespace(String env, String appId,
      String clusterName, String namespaceName, Integer page, Integer size, String instanceAppId) {
    return ResponseEntity.ok(OpenApiModelConverters
        .fromInstancePageDTO(instanceService.getByNamespace(Env.valueOf(env), appId, clusterName,
            namespaceName, instanceAppId, resolvePage(page), resolvePageSize(size))));
  }

  @Override
  public ResponseEntity<OpenInstancePageDTO> getByRelease(String env, Long releaseId, Integer page,
      Integer size) {
    return ResponseEntity.ok(OpenApiModelConverters.fromInstancePageDTO(instanceService
        .getByRelease(Env.valueOf(env), releaseId, resolvePage(page), resolvePageSize(size))));
  }

  @Override
  public ResponseEntity<List<OpenInstanceDTO>> getByReleasesAndNamespaceNotIn(String env,
      String appId, String clusterName, String namespaceName, String releaseIds) {
    if (releaseIds == null || releaseIds.trim().isEmpty()) {
      throw new BadRequestException("releaseIds should not be empty");
    }

    Set<Long> releaseIdSet;
    try {
      releaseIdSet = RELEASE_ID_SPLITTER.splitToStream(releaseIds).map(Long::parseLong)
          .collect(Collectors.toSet());
    } catch (NumberFormatException ex) {
      throw new BadRequestException("releaseIds should be comma separated numbers");
    }
    if (releaseIdSet.isEmpty()) {
      throw new BadRequestException("releaseIds should not be empty");
    }
    return ResponseEntity.ok(OpenApiModelConverters.fromInstanceDTOs(instanceService
        .getByReleasesNotIn(Env.valueOf(env), appId, clusterName, namespaceName, releaseIdSet)));
  }

  @Override
  public ResponseEntity<Integer> getInstanceCountByNamespace(String env, String appId,
      String clusterName, String namespaceName) {
    return ResponseEntity.ok(instanceService.getInstanceCountByNamespace(appId, Env.valueOf(env),
        clusterName, namespaceName));
  }

  private int resolvePage(Integer page) {
    return page == null ? DEFAULT_PAGE : page;
  }

  private int resolvePageSize(Integer size) {
    return size == null ? DEFAULT_INSTANCE_PAGE_SIZE : size;
  }
}
