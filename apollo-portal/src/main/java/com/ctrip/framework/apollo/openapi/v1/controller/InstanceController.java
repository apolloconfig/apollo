/*
 * Copyright 2024 Apollo Authors
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.api.InstanceOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenPageDTO;
import com.ctrip.framework.apollo.openapi.server.service.ServerInstanceOpenApiService;
import com.google.common.base.Splitter;

@RestController("openapiInstanceController")
@RequestMapping("/openapi/v1")
public class InstanceController {
    
    private static final Splitter RELEASES_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
    
    private final InstanceOpenApiService instanceOpenApiService;
    private final ServerInstanceOpenApiService serverInstanceOpenApiService;

    public InstanceController(InstanceOpenApiService instanceOpenApiService, 
                             ServerInstanceOpenApiService serverInstanceOpenApiService) {
        this.instanceOpenApiService = instanceOpenApiService;
        this.serverInstanceOpenApiService = serverInstanceOpenApiService;
    }

    /**
     * 获取命名空间下的实例数量
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances/count
     */
    @GetMapping(value = "/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances/count")
    public ResponseEntity<Integer> getInstanceCount(@PathVariable String env,
                                                    @PathVariable String appId,
                                                    @PathVariable String clusterName, 
                                                    @PathVariable String namespaceName) {
        int count = this.instanceOpenApiService.getInstanceCountByNamespace(appId, env, clusterName, namespaceName);
        return ResponseEntity.ok(count);
    }

    /**
     * 根据发布版本查询实例（支持分页）
     * GET /openapi/v1/envs/{env}/releases/{releaseId}/instances
     */
    @GetMapping(value = "/envs/{env}/releases/{releaseId}/instances")
    public ResponseEntity<OpenPageDTO<OpenInstanceDTO>> getInstancesByRelease(@PathVariable String env, 
                                                                              @PathVariable Long releaseId,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "20") int size) {
        OpenPageDTO<OpenInstanceDTO> instances = serverInstanceOpenApiService.getByRelease(env, releaseId, page, size);
        return ResponseEntity.ok(instances);
    }

    /**
     * 根据命名空间查询实例（支持分页）
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances
     */
    @GetMapping(value = "/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances")
    public ResponseEntity<OpenPageDTO<OpenInstanceDTO>> getInstancesByNamespace(@PathVariable String env, 
                                                                                @PathVariable String appId,
                                                                                @PathVariable String clusterName, 
                                                                                @PathVariable String namespaceName,
                                                                                @RequestParam(required = false) String instanceAppId,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {
        OpenPageDTO<OpenInstanceDTO> instances = serverInstanceOpenApiService.getByNamespace(env, appId, clusterName, namespaceName, instanceAppId, page, size);
        return ResponseEntity.ok(instances);
    }

    /**
     * 查询不在指定发布版本中的实例
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances?excludeReleases=1,2,3
     */
    @GetMapping(value = "/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances", 
                params = "excludeReleases")
    public ResponseEntity<List<OpenInstanceDTO>> getInstancesExcludingReleases(@PathVariable String env, 
                                                                               @PathVariable String appId,
                                                                               @PathVariable String clusterName, 
                                                                               @PathVariable String namespaceName,
                                                                               @RequestParam("excludeReleases") String releaseIds) {
        Set<Long> releaseIdSet = RELEASES_SPLITTER.splitToList(releaseIds).stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(releaseIdSet)) {
            throw new BadRequestException("excludeReleases parameter cannot be empty");
        }

        List<OpenInstanceDTO> instances = serverInstanceOpenApiService.getByReleasesNotIn(env, appId, clusterName, namespaceName, releaseIdSet);
        return ResponseEntity.ok(instances);
    }
}