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

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.constant.TracerEventType;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.tracer.Tracer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClusterService {

  private final UserInfoHolder userInfoHolder;
  private final AdminServiceAPI.ClusterAPI clusterAPI;
  private final RoleInitializationService roleInitializationService;

  public ClusterService(final UserInfoHolder userInfoHolder, final AdminServiceAPI.ClusterAPI clusterAPI,
      RoleInitializationService roleInitializationService) {
    this.userInfoHolder = userInfoHolder;
    this.clusterAPI = clusterAPI;
    this.roleInitializationService = roleInitializationService;
  }

  public List<ClusterDTO> findClusters(Env env, String appId) {
    return clusterAPI.findClustersByApp(appId, env);
  }

  public ClusterDTO createCluster(Env env, ClusterDTO cluster) {
    if (!clusterAPI.isClusterUnique(cluster.getAppId(), env, cluster.getName())) {
      throw BadRequestException.clusterAlreadyExists(cluster.getName());
    }
    ClusterDTO clusterDTO = clusterAPI.create(env, cluster);

    roleInitializationService.initClusterNamespaceRoles(cluster.getAppId(), env.getName(), cluster.getName(),
        userInfoHolder.getUser().getUserId());

    Tracer.logEvent(TracerEventType.CREATE_CLUSTER, cluster.getAppId(), "0", cluster.getName());

    return clusterDTO;
  }

  public void deleteCluster(Env env, String appId, String clusterName){
    clusterAPI.delete(env, appId, clusterName, userInfoHolder.getUser().getUserId());
  }

  public ClusterDTO loadCluster(String appId, Env env, String clusterName){
    return clusterAPI.loadCluster(appId, env, clusterName);
  }

}
