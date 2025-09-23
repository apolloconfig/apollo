/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.common.http.MultiResponseEntity;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface AppOpenApiService {

  default void createApp(OpenCreateAppDTO req) {
    throw new UnsupportedOperationException();
  }

  List<OpenEnvClusterDTO> getEnvClusterInfo(String appId);

  List<OpenAppDTO> getAllApps();

  List<OpenAppDTO> getAppsInfo(List<String> appIds);

  List<OpenAppDTO> getAuthorizedApps();

  void updateApp(OpenAppDTO openAppDTO);

  List<OpenAppDTO> getAppsBySelf(Set<String> appIds, Pageable page);

  MultiResponseEntity<EnvClusterInfo> getAppNavTree(String appId);
}
