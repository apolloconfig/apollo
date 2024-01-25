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

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.openapi.api.ClusterOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import org.springframework.stereotype.Service;

/**
 * @author wxq
 */
@Service
public class ServerClusterOpenApiService implements ClusterOpenApiService {

  private final ClusterService clusterService;

  public ServerClusterOpenApiService(ClusterService clusterService) {
    this.clusterService = clusterService;
  }

  @Override
  public OpenClusterDTO getCluster(String appId, String env, String clusterName) {
    ClusterDTO clusterDTO = clusterService.loadCluster(appId, Env.valueOf(env), clusterName);
    return clusterDTO == null ? null : OpenApiBeanUtils.transformFromClusterDTO(clusterDTO);
  }

  @Override
  public OpenClusterDTO createCluster(String env, OpenClusterDTO openClusterDTO) {
    ClusterDTO toCreate = OpenApiBeanUtils.transformToClusterDTO(openClusterDTO);
    ClusterDTO createdClusterDTO = clusterService.createCluster(Env.valueOf(env), toCreate);
    return OpenApiBeanUtils.transformFromClusterDTO(createdClusterDTO);
  }
}
