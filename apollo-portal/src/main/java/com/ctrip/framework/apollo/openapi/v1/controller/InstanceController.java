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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenPageDTOOpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.server.service.InstanceOpenApiService;
import com.google.common.base.Splitter;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController("openapiInstanceController")
@RequestMapping("/openapi/v1/envs/{env}")
public class InstanceController {
    
    private static final Splitter RELEASES_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
    
    private final InstanceOpenApiService instanceOpenApiService;

    public InstanceController(InstanceOpenApiService instanceOpenApiService) {
        this.instanceOpenApiService = instanceOpenApiService;
    }

    /**
     * 获取命名空间下的实例数量
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances
     */
    @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances")
    public int getInstanceCountByNamespace(@PathVariable String appId, @PathVariable String env,
                                           @PathVariable String clusterName, @PathVariable String namespaceName) {
        return this.instanceOpenApiService.getInstanceCountByNamespace(appId, env, clusterName, namespaceName);
    }

    /**
     * 根据发布版本查询实例（支持分页）
     * GET /openapi/v1/envs/{env}/releases/{releaseId}/instances
     */
    @GetMapping(value = "/releases/{releaseId}/instances")
    public ResponseEntity<OpenPageDTOOpenInstanceDTO> getInstancesByRelease(@PathVariable String env,
                                                                          @PathVariable Long releaseId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        OpenPageDTOOpenInstanceDTO instances = instanceOpenApiService.getByRelease(env, releaseId, page, size);
        return ResponseEntity.ok(instances);
    }

    /**
     * 根据命名空间查询实例（支持分页）
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances_search
     */
    @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances_search")
    public ResponseEntity<OpenPageDTOOpenInstanceDTO> getInstancesByNamespace(@PathVariable String env,
                                                                         @PathVariable String appId,
                                                                         @PathVariable String clusterName,
                                                                         @PathVariable String namespaceName,
                                                                         @RequestParam(required = false) String instanceAppId,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        OpenPageDTOOpenInstanceDTO instances = instanceOpenApiService.getByNamespace(env, appId, clusterName, namespaceName, instanceAppId, page, size);
        return ResponseEntity.ok(instances);
    }

    /**
     * 查询不在指定发布版本中的实例
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances_not_in?excludeReleases=1,2,3
     */
    @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances_not_in",
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

        List<OpenInstanceDTO> instances = instanceOpenApiService.getByReleasesNotIn(env, appId, clusterName, namespaceName, releaseIdSet);
        return ResponseEntity.ok(instances);
    }
}