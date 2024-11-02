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

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.api.NamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author wxq
 */
@Service
public class ServerNamespaceOpenApiService implements NamespaceOpenApiService {

  private final AppNamespaceService appNamespaceService;
  private final ApplicationEventPublisher publisher;
  private final NamespaceService namespaceService;
  private final NamespaceLockService namespaceLockService;

  public ServerNamespaceOpenApiService(
      AppNamespaceService appNamespaceService,
      ApplicationEventPublisher publisher,
      NamespaceService namespaceService,
      NamespaceLockService namespaceLockService) {
    this.appNamespaceService = appNamespaceService;
    this.publisher = publisher;
    this.namespaceService = namespaceService;
    this.namespaceLockService = namespaceLockService;
  }

  @Override
  public OpenNamespaceDTO getNamespace(String appId, String env, String clusterName,
      String namespaceName, boolean fillItemDetail) {
    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf
        (env), clusterName, namespaceName, fillItemDetail, false);
    if (namespaceBO == null) {
      return null;
    }
    return OpenApiBeanUtils.transformFromNamespaceBO(namespaceBO);
  }

  @Override
  public List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName, boolean fillItemDetail) {
    return OpenApiBeanUtils
        .batchTransformFromNamespaceBOs(namespaceService.findNamespaceBOs(appId, Env
            .valueOf(env), clusterName, fillItemDetail, false));
  }

  @Override
  public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespaceDTO) {
    AppNamespace appNamespace = OpenApiBeanUtils.transformToAppNamespace(appNamespaceDTO);
    AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(appNamespace, appNamespaceDTO.isAppendNamespacePrefix());

    publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));

    return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(createdAppNamespace);
  }

  @Override
  public OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName,
      String namespaceName) {
    NamespaceDTO namespace = namespaceService.loadNamespaceBaseInfo(appId, Env
        .valueOf(env), clusterName, namespaceName);
    NamespaceLockDTO lockDTO = namespaceLockService.getNamespaceLock(appId, Env
        .valueOf(env), clusterName, namespaceName);
    return OpenApiBeanUtils.transformFromNamespaceLockDTO(namespace.getNamespaceName(), lockDTO);
  }
}
