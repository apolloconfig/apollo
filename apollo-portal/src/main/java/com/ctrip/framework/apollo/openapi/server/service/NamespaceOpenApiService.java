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

import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceUsageDTO;
import java.util.List;
import java.util.Map;

/**
 * Portal-local Namespace OpenAPI service backed by generated OpenAPI model contracts.
 */
public interface NamespaceOpenApiService {

  OpenNamespaceDTO findNamespace(String appId, String env, String clusterName, String namespaceName,
      boolean fillItemDetail, boolean extendInfo);

  List<OpenNamespaceDTO> findNamespaces(String appId, String env, String clusterName,
      boolean fillItemDetail, boolean extendInfo);

  OpenNamespaceDTO findPublicNamespaceForAssociatedNamespace(String env, String appId,
      String clusterName, String namespaceName, boolean extendInfo);

  OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName,
      String namespaceName);

  OpenAppNamespaceDTO createAppNamespace(String appId, OpenAppNamespaceDTO appNamespace,
      String operator);

  void createNamespaces(List<OpenCreateNamespaceDTO> namespaces, String operator);

  void deleteNamespace(String appId, String env, String clusterName, String namespaceName,
      String operator);

  List<OpenNamespaceUsageDTO> findNamespaceUsage(String appId, String env, String clusterName,
      String namespaceName);

  Map<String, Map<String, Boolean>> getNamespacesReleaseStatus(String appId);

  List<String> findMissingNamespaces(String appId, String env, String clusterName);

  void createMissingNamespaces(String appId, String env, String clusterName, String operator);

  List<OpenAppNamespaceDTO> getAppNamespaces();

  List<OpenAppNamespaceDTO> getAppNamespacesByAppId(String appId);

  OpenAppNamespaceDTO findAppNamespace(String appId, String namespaceName);

  void deleteAppNamespace(String appId, String namespaceName, String operator);

  List<OpenNamespaceUsageDTO> findAppNamespaceUsage(String appId, String namespaceName);

  List<OpenNamespaceDTO> getPublicAppNamespaceInstances(String env, String publicNamespaceName,
      int page, int size);
}
