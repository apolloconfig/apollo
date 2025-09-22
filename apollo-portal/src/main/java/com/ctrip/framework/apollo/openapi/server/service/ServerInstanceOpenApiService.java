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

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.openapi.api.InstanceOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;

/**
 * Server端实现的Instance OpenAPI服务
 * 通过封装调用InstanceService的方法来实现OpenAPI功能
 */
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

    /**
     * 根据发布版本查询实例（支持分页） - 返回OpenAPI DTO
     */
    public PageDTO<OpenInstanceDTO> getByRelease(String env, long releaseId, int page, int size) {
        PageDTO<InstanceDTO> portalPageDTO = instanceService.getByRelease(Env.valueOf(env), releaseId, page, size);

        return transformToOpenPageDTO(portalPageDTO);
    }

    /**
     * 根据命名空间查询实例（支持分页） - 返回OpenAPI DTO
     */
    public PageDTO<OpenInstanceDTO> getByNamespace(String env, String appId, String clusterName, 
                                                        String namespaceName, String instanceAppId, 
                                                        int page, int size) {
        PageDTO<InstanceDTO> portalPageDTO = instanceService.getByNamespace(Env.valueOf(env), appId, clusterName, namespaceName, instanceAppId, page, size);
        return transformToOpenPageDTO(portalPageDTO);
    }

    /**
     * 查询不在指定发布版本中的实例 - 返回OpenAPI DTO
     */
    public List<OpenInstanceDTO> getByReleasesNotIn(String env, String appId, String clusterName, 
                                                     String namespaceName, Set<Long> releaseIds) {
        List<InstanceDTO> portalInstances = instanceService.getByReleasesNotIn(Env.valueOf(env), appId, clusterName, namespaceName, releaseIds);
        return OpenApiBeanUtils.transformFromInstanceDTOs(portalInstances);
    }

    /**
     * 将PageDTO<InstanceDTO>转换为PageDTO<OpenInstanceDTO>
     */
    private PageDTO<OpenInstanceDTO> transformToOpenPageDTO(PageDTO<InstanceDTO> pageDTO) {
        List<OpenInstanceDTO> openContent = OpenApiBeanUtils.transformFromInstanceDTOs(pageDTO.getContent());
        PageRequest pageable = PageRequest.of(pageDTO.getPage(), pageDTO.getSize());
        return new PageDTO<>(openContent, pageable, pageDTO.getTotal());
    }
}