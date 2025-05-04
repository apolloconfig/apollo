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
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.openapi.api.InstanceOpenApiService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import org.springframework.stereotype.Service;

@Service
public class ServerInstanceOpenApiService implements InstanceOpenApiService {

    private final InstanceService instanceService;

    public ServerInstanceOpenApiService(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Override
    public int getInstanceCountByNamespace(String appId, String env, String clusterName, String namespaceName) {
        return instanceService.getInstanceCountByNamespace(appId, Env.valueOf(env), clusterName, namespaceName);
    }
}