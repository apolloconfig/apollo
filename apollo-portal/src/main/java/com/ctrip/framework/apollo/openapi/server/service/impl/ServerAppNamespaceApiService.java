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
package com.ctrip.framework.apollo.openapi.server.service.impl;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppNamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServerAppNamespaceApiService implements AppNamespaceOpenApiService {

  private final AppNamespaceService appNamespaceService;
  private final UserInfoHolder userInfoHolder;
  private final ApplicationEventPublisher publisher;

  public ServerAppNamespaceApiService(AppNamespaceService appNamespaceService,
                                      UserInfoHolder userInfoHolder,
                                      ApplicationEventPublisher publisher) {
    this.appNamespaceService = appNamespaceService;
    this.userInfoHolder = userInfoHolder;
    this.publisher = publisher;
  }

  @Override
  public List<OpenAppNamespaceDTO> findPublicAppNamespaces() {
    List<AppNamespace> appNamespaces = appNamespaceService.findPublicAppNamespaces();
    if (appNamespaces == null || appNamespaces.isEmpty()) {
      return Collections.emptyList();
    }
    return appNamespaces.stream()
        .map(OpenApiModelConverters::fromAppNamespace)
        .collect(Collectors.toList());
  }

  @Override
  public OpenAppNamespaceDTO findPublicAppNamespace(String namespaceName) {
    AppNamespace appNamespace = appNamespaceService.findPublicAppNamespace(namespaceName);
    if (appNamespace == null) {
      return null;
    }
    return OpenApiModelConverters.fromAppNamespace(appNamespace);
  }

  @Override
  public OpenAppNamespaceDTO findByAppIdAndName(String appId, String namespaceName) {
    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);
    if (appNamespace == null) {
      return null;
    }
    return OpenApiModelConverters.fromAppNamespace(appNamespace);
  }

  @Override
  public List<OpenAppNamespaceDTO> findByAppId(String appId) {
    List<AppNamespace> appNamespaces = appNamespaceService.findByAppId(appId);
    if (appNamespaces == null || appNamespaces.isEmpty()) {
      return Collections.emptyList();
    }
    return appNamespaces.stream()
        .map(OpenApiModelConverters::fromAppNamespace)
        .collect(Collectors.toList());
  }

  @Override
  public List<OpenAppNamespaceDTO> findAll() {
    List<AppNamespace> appNamespaces = appNamespaceService.findAll();
    if (appNamespaces == null || appNamespaces.isEmpty()) {
      return Collections.emptyList();
    }
    return appNamespaces.stream()
        .map(OpenApiModelConverters::fromAppNamespace)
        .collect(Collectors.toList());
  }

  @Override
  public void createDefaultAppNamespace(String appId) {
    appNamespaceService.createDefaultAppNamespace(appId);
  }

  @Override
  public boolean isAppNamespaceNameUnique(String appId, String namespaceName) {
    return appNamespaceService.isAppNamespaceNameUnique(appId, namespaceName);
  }

  @Override
  public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace) {
    return createAppNamespace(appNamespace, true);
  }

  @Override
  public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace, boolean appendNamespacePrefix) {
    AppNamespace entity = OpenApiModelConverters.toAppNamespace(appNamespace);
    AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(entity, appendNamespacePrefix);
    publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));
    return OpenApiModelConverters.fromAppNamespace(createdAppNamespace);
  }

  @Override
  public OpenAppNamespaceDTO importAppNamespace(OpenAppNamespaceDTO appNamespace) {
    AppNamespace entity = OpenApiModelConverters.toAppNamespace(appNamespace);
    AppNamespace importedAppNamespace = appNamespaceService.importAppNamespaceInLocal(entity);
    return OpenApiModelConverters.fromAppNamespace(importedAppNamespace);
  }

  @Override
  public OpenAppNamespaceDTO deleteAppNamespace(String appId, String namespaceName) {
    AppNamespace deletedAppNamespace = appNamespaceService.deleteAppNamespace(appId, namespaceName);
    return OpenApiModelConverters.fromAppNamespace(deletedAppNamespace);
  }

  @Override
  public void batchDeleteByAppId(String appId) {
    String operator = userInfoHolder.getUser().getUserId();
    appNamespaceService.batchDeleteByAppId(appId, operator);
  }
}

