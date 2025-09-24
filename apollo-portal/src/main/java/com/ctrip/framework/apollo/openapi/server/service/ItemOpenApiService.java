/*
 * Copyright 2022 Apollo Authors
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

import com.ctrip.framework.apollo.openapi.model.*;
import com.ctrip.framework.apollo.portal.environment.Env;

import java.util.List;

/**
 * @author wxq
 */
public interface ItemOpenApiService {

  OpenItemDTO getItem(String appId, String env, String clusterName, String namespaceName,
      String key);

  OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO);

  void updateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO);

  void createOrUpdateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO);

  void removeItem(String appId, String env, String clusterName, String namespaceName, String key);

  OpenPageDTOOpenItemDTO findItemsByNamespace(String appId, String env, String clusterName,
                                              String namespaceName, int page, int size);

  OpenItemDTO loadItem(Env env, String appId, String clusterName, String namespaceName, String key);

  void modifyItemsByText(String appId, String env, String clusterName, String namespaceName,
                         OpenNamespaceTextModel model);

  List<OpenItemDTO> findBranchItems(String appId, String env, String clusterName,
                                    String namespaceName, String branchName);

  List<OpenItemDiffs> diff(OpenNamespaceSyncModel model);

  void syncItems(OpenNamespaceSyncModel model);

  void syntaxCheckText(OpenNamespaceTextModel model);

  void revokeItems(String appId, String env, String clusterName, String namespaceName);

  List<OpenItemDTO> findItemsWithOrder(String appId, String env, String clusterName,
                                       String namespaceName, String orderBy);
}
