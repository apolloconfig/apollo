/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.api;

import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;

/**
 * @author wxq
 */
public interface ApolloItemOpenApi {

  OpenItemDTO getItem(String appId, String env, String clusterName, String namespaceName,
      String key);

  OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO);

  void updateItem(String appId, String env, String clusterName, String namespaceName, String key,
      OpenItemDTO itemDTO, boolean createIfNotExists);

  void removeItem(String appId, String env, String clusterName, String namespaceName, String key,
      String operator);
}
