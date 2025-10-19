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

import com.ctrip.framework.apollo.openapi.model.MultiResponseEntity;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import java.util.List;
import java.util.Set;
import org.springframework.lang.NonNull;

public interface AppOpenApiService {

  void createApp(@NonNull OpenCreateAppDTO req);

  List<OpenEnvClusterDTO> getEnvClusterInfo(String appId);

  List<OpenAppDTO> getAllApps();

  List<OpenAppDTO> getAppsInfo(List<String> appIds);

  List<OpenAppDTO> getAuthorizedApps();

  void updateApp(OpenAppDTO openAppDTO);

  List<OpenAppDTO> getAppsBySelf(Set<String> appIds, Integer page, Integer size);

  void createAppInEnv(String env, OpenAppDTO app, String operator);

  OpenAppDTO deleteApp(String appId);

  MultiResponseEntity findMissEnvs(String appId);

  MultiResponseEntity getAppNavTree(String appId);
}
