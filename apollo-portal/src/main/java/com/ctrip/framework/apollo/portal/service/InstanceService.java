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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class InstanceService {


  private final AdminServiceAPI.InstanceAPI instanceAPI;

  public InstanceService(final AdminServiceAPI.InstanceAPI instanceAPI) {
    this.instanceAPI = instanceAPI;
  }

  public PageDTO<InstanceDTO> getByRelease(Env env, long releaseId, int page, int size){
    return instanceAPI.getByRelease(env, releaseId, page, size);
  }

  public PageDTO<InstanceDTO> getByNamespace(Env env, String appId, String clusterName, String namespaceName,
                                             String instanceAppId, int page, int size){
    return instanceAPI.getByNamespace(appId, env, clusterName, namespaceName, instanceAppId, page, size);
  }

  public int getInstanceCountByNamespace(String appId, Env env, String clusterName, String namespaceName){
    return instanceAPI.getInstanceCountByNamespace(appId, env, clusterName, namespaceName);
  }

  public List<InstanceDTO> getByReleasesNotIn(Env env, String appId, String clusterName, String namespaceName, Set<Long> releaseIds){
    return instanceAPI.getByReleasesNotIn(appId, env, clusterName, namespaceName, releaseIds);
  }



}
