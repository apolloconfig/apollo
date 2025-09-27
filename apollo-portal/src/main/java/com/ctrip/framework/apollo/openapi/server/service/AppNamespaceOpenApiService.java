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

import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import java.util.List;

public interface AppNamespaceOpenApiService {

  List<OpenAppNamespaceDTO> findPublicAppNamespaces();

  OpenAppNamespaceDTO findPublicAppNamespace(String namespaceName);

  OpenAppNamespaceDTO findByAppIdAndName(String appId, String namespaceName);

  List<OpenAppNamespaceDTO> findByAppId(String appId);

  List<OpenAppNamespaceDTO> findAll();

  void createDefaultAppNamespace(String appId);

  boolean isAppNamespaceNameUnique(String appId, String namespaceName);

  OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace);

  OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace, boolean appendNamespacePrefix);

  OpenAppNamespaceDTO importAppNamespace(OpenAppNamespaceDTO appNamespace);

  OpenAppNamespaceDTO deleteAppNamespace(String appId, String namespaceName);

  void batchDeleteByAppId(String appId);
}

