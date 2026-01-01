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
import com.ctrip.framework.apollo.openapi.server.service.InstanceOpenApiService;
import com.google.common.base.Splitter;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController("openapiInstanceController")
public class InstanceController implements InstanceManagementApi {

  private final InstanceOpenApiService instanceOpenApiService;

  public InstanceController(InstanceOpenApiService instanceOpenApiService) {
    this.instanceOpenApiService = instanceOpenApiService;
  }

  @Override
  public ResponseEntity<Integer> getInstanceCountByNamespace(String env, String appId,
      String clusterName, String namespaceName) {
    return ResponseEntity.ok(this.instanceOpenApiService.getInstanceCountByNamespace(appId, env,
        clusterName, namespaceName));
  }

  /**
   * Query instances by release version (supports pagination)
   */
  @Override
  public ResponseEntity<OpenInstancePageDTO> getByRelease(String env, Long releaseId, Integer page,
      Integer size) {
    return ResponseEntity.ok(this.instanceOpenApiService.getByRelease(env, releaseId, page, size));
  }

  /**
   * Query instance by namespace (supports pagination)
   */
  @Override
  public ResponseEntity<OpenInstancePageDTO> getByNamespace(String env, String appId,
      String clusterName, String namespaceName, Integer page, Integer size, String instanceAppId) {
    return ResponseEntity.ok(instanceOpenApiService.getByNamespace(env, appId, clusterName,
        namespaceName, instanceAppId, page, size));
  }

  /**
   * Query instances not in a specified release
   */
  @Override
  public ResponseEntity<List<OpenInstanceDTO>> getByReleasesAndNamespaceNotIn(String env,
      String appId, String clusterName, String namespaceName, String releaseIds) {
    if (null == releaseIds || releaseIds.isEmpty()) {
      throw new BadRequestException("release ids can not be empty");
    }
    List<String> rawReleaseIds =
        Splitter.on(",").omitEmptyStrings().trimResults().splitToList(releaseIds);

    if (CollectionUtils.isEmpty(rawReleaseIds)) {
      throw new BadRequestException("excludeReleases parameter cannot be empty");
    }

    final Set<Long> releaseIdSet;
    try {
      releaseIdSet = rawReleaseIds.stream().map(Long::parseLong).collect(Collectors.toSet());
    } catch (NumberFormatException ex) {
      throw new BadRequestException(
          "excludeReleases parameter must contain only numeric release ids", ex);
    }

    return ResponseEntity.ok(this.instanceOpenApiService.getByReleasesNotIn(env, appId, clusterName,
        namespaceName, releaseIdSet));
  }
}
