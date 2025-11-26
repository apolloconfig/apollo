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

import com.ctrip.framework.apollo.common.dto.InstanceConfigDTO;
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import java.util.Collections;
import java.util.Date;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ServerInstanceOpenApiService implements InstanceOpenApiService {

  private final InstanceService instanceService;

  public ServerInstanceOpenApiService(InstanceService instanceService) {
    this.instanceService = instanceService;
  }

  @Override
  public int getInstanceCountByNamespace(String appId, String env, String clusterName,
      String namespaceName) {
    return instanceService.getInstanceCountByNamespace(appId, Env.valueOf(env), clusterName,
        namespaceName);
  }

  /**
   * Query instances by release version (supports pagination) - returns OpenAPI DTO
   */
  @Override
  public OpenInstancePageDTO getByRelease(String env, long releaseId, int page, int size) {
    PageDTO<InstanceDTO> portalPageDTO =
        instanceService.getByRelease(Env.valueOf(env), releaseId, page, size);
    // PageDTO<InstanceDTO> portalPageDTO = mockPortalPageDTO(page, size);

    return transformToOpenPageDTO(portalPageDTO);
  }

  /**
   * Query instances not in a specified release - returns an OpenAPI DTO
   */
  @Override
  public List<OpenInstanceDTO> getByReleasesNotIn(String env, String appId, String clusterName,
      String namespaceName, Set<Long> releaseIds) {
    List<InstanceDTO> portalInstances = instanceService.getByReleasesNotIn(Env.valueOf(env), appId,
        clusterName, namespaceName, releaseIds);
    return OpenApiModelConverters.fromInstanceDTOs(portalInstances);
  }

  @Override
  public OpenInstancePageDTO getByNamespace(String env, String appId, String clusterName,
      String namespaceName, String instanceAppId, Integer page, Integer size) {
    // return transformToOpenPageDTO((mockPortalPageDTO(page,size)));
    return transformToOpenPageDTO(instanceService.getByNamespace(Env.valueOf(env), appId,
        clusterName, namespaceName, instanceAppId, page, size));
  }

  /**
   * Convert PageDTO<InstanceDTO> to OpenPageDTOOpenInstanceDTO
   */
  private OpenInstancePageDTO transformToOpenPageDTO(PageDTO<InstanceDTO> pageDTO) {
    List<OpenInstanceDTO> instances = OpenApiModelConverters.fromInstanceDTOs(pageDTO.getContent());
    OpenInstancePageDTO openInstancePageDTO = new OpenInstancePageDTO();
    openInstancePageDTO.setPage(pageDTO.getPage());
    openInstancePageDTO.setSize(pageDTO.getSize());
    openInstancePageDTO.setTotal(pageDTO.getTotal());
    openInstancePageDTO.setInstances(instances);

    return openInstancePageDTO;
  }

  private PageDTO<InstanceDTO> mockPortalPageDTO(int page, int size) {
    InstanceConfigDTO config = new InstanceConfigDTO();
    config.setReleaseDeliveryTime(new Date());
    config.setDataChangeLastModifiedTime(new Date());

    InstanceDTO instance = new InstanceDTO();
    instance.setId(1L);
    instance.setAppId("mock-app");
    instance.setClusterName("default");
    instance.setDataCenter("SHA");
    instance.setIp("10.0.0.1");
    instance.setConfigs(Collections.singletonList(config));

    return new PageDTO<>(Collections.singletonList(instance), PageRequest.of(page, size), 1 // mock
                                                                                            // 总数
    );
  }
}
