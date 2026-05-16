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

import com.ctrip.framework.apollo.openapi.model.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemDiffDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceSyncDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceTextModel;
import java.util.List;

/**
 * Portal-local Item OpenAPI service backed by generated OpenAPI model contracts.
 */
public interface ItemOpenApiService {

  OpenItemDTO getItem(String appId, String env, String clusterName, String namespaceName,
      String key);

  OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO, String operator);

  void updateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO, String operator);

  void createOrUpdateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO, String operator);

  void removeItem(String appId, String env, String clusterName, String namespaceName, String key,
      String operator);

  OpenItemPageDTO findItemsByNamespace(String appId, String env, String clusterName,
      String namespaceName, int page, int size);

  List<OpenItemDTO> findBranchItems(String appId, String env, String branchName,
      String namespaceName);

  void batchUpdateItemsByText(String appId, String env, String clusterName, String namespaceName,
      OpenNamespaceTextModel model, String operator);

  List<OpenItemDiffDTO> compareItems(String appId, String env, String clusterName,
      String namespaceName, OpenNamespaceSyncDTO model);

  void syncItems(String appId, String env, String clusterName, String namespaceName,
      OpenNamespaceSyncDTO model, String operator);

  void revertItems(String appId, String env, String clusterName, String namespaceName,
      String operator);
}
