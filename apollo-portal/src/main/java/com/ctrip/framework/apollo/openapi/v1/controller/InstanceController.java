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

import com.ctrip.framework.apollo.openapi.api.InstanceOpenApiService;
import org.springframework.web.bind.annotation.*;

@RestController("openapiInstanceController")
@RequestMapping("/openapi/v1/envs/{env}")
public class InstanceController {
    private final InstanceOpenApiService instanceOpenApiService;

    public InstanceController(InstanceOpenApiService instanceOpenApiService) {
        this.instanceOpenApiService = instanceOpenApiService;
    }

    @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/instances")
    public int getInstanceCountByNamespace(@PathVariable String appId, @PathVariable String env,
                                           @PathVariable String clusterName, @PathVariable String namespaceName) {
        return this.instanceOpenApiService.getInstanceCountByNamespace(appId, env, clusterName, namespaceName);
    }
}