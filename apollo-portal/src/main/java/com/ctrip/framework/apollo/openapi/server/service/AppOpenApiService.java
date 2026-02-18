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

import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenMissEnvDTO;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Set;

public interface AppOpenApiService {

  OpenAppDTO createApp(@NonNull OpenCreateAppDTO req);

  List<OpenEnvClusterDTO> getEnvClusters(String appId);

  List<OpenAppDTO> getAllApps();

  List<OpenAppDTO> getAppsInfo(List<String> appIds);

  List<OpenAppDTO> getAuthorizedApps();

  void updateApp(OpenAppDTO openAppDTO);

  List<OpenAppDTO> getAppsWithPageAndSize(Set<String> appIds, Integer page, Integer size);

  void createAppInEnv(String env, OpenAppDTO app);

  OpenAppDTO deleteApp(String appId);

  List<OpenMissEnvDTO> findMissEnvs(String appId);

  List<OpenEnvClusterInfo> getEnvClusterInfos(String appId);
}
