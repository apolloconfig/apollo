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

import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;

import java.util.List;
import java.util.Set;

public interface InstanceOpenApiService {

  int getInstanceCountByNamespace(String appId, String env, String clusterName,
      String namespaceName);

  OpenInstancePageDTO getByRelease(String env, long releaseId, int page, int size);

  List<OpenInstanceDTO> getByReleasesNotIn(String env, String appId, String clusterName,
      String namespaceName, Set<Long> releaseIds);

  OpenInstancePageDTO getByNamespace(String env, String appId, String clusterName,
      String namespaceName, String instanceAppId, Integer page, Integer size);
}
