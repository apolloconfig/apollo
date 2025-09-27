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

import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;

/**
 * @author wxq
 */
public interface ReleaseOpenApiService {

  OpenReleaseDTO publishNamespace(String appId, String env, String clusterName,
      String namespaceName,
      NamespaceReleaseDTO releaseDTO);

  OpenReleaseDTO getLatestActiveRelease(String appId, String env, String clusterName,
                                        String namespaceName);

  void rollbackRelease(String env, long releaseId, String operator);

  OpenReleaseDTO getReleaseById(String env, long releaseId);
}
