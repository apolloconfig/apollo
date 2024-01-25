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

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.openapi.api.ReleaseOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import org.springframework.stereotype.Service;

/**
 * @author wxq
 */
@Service
public class ServerReleaseOpenApiService implements ReleaseOpenApiService {
  private final ReleaseService releaseService;

  public ServerReleaseOpenApiService(
      ReleaseService releaseService) {
    this.releaseService = releaseService;
  }

  @Override
  public OpenReleaseDTO publishNamespace(String appId, String env, String clusterName,
      String namespaceName, NamespaceReleaseDTO releaseDTO) {
    NamespaceReleaseModel releaseModel = BeanUtils.transform(NamespaceReleaseModel.class, releaseDTO);

    releaseModel.setAppId(appId);
    releaseModel.setEnv(Env.valueOf(env).toString());
    releaseModel.setClusterName(clusterName);
    releaseModel.setNamespaceName(namespaceName);

    return OpenApiBeanUtils.transformFromReleaseDTO(releaseService.publish(releaseModel));
  }

  @Override
  public OpenReleaseDTO getLatestActiveRelease(String appId, String env, String clusterName,
      String namespaceName) {
    ReleaseDTO releaseDTO = releaseService.loadLatestRelease(appId, Env.valueOf
        (env), clusterName, namespaceName);
    if (releaseDTO == null) {
      return null;
    }

    return OpenApiBeanUtils.transformFromReleaseDTO(releaseDTO);
  }

  @Override
  public void rollbackRelease(String env, long releaseId, String operator) {
    releaseService.rollback(Env.valueOf(env), releaseId, operator);
  }
}
