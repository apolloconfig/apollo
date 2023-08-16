/*
 * Copyright 2023 Apollo Authors
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

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.openapi.api.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppCreationEvent;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author wxq
 */
@Service
public class ServerAppOpenApiService implements AppOpenApiService {

  private final PortalSettings portalSettings;
  private final ClusterService clusterService;
  private final AppService appService;

  private final ApplicationEventPublisher publisher;

  private final RoleInitializationService roleInitializationService;

  private final UserInfoHolder userInfoHolder;

  public ServerAppOpenApiService(
      PortalSettings portalSettings,
      ClusterService clusterService,
      AppService appService, ApplicationEventPublisher publisher,
      RoleInitializationService roleInitializationService, UserInfoHolder userInfoHolder) {
    this.portalSettings = portalSettings;
    this.clusterService = clusterService;
    this.appService = appService;
    this.publisher = publisher;
    this.roleInitializationService = roleInitializationService;
    this.userInfoHolder = userInfoHolder;
  }

  private static App convert(OpenAppDTO openAppDTO) {
    return App.builder()
        .appId(openAppDTO.getAppId())
        .name(openAppDTO.getName())
        .ownerName(openAppDTO.getOwnerName())
        .orgId(openAppDTO.getOrgId())
        .orgName(openAppDTO.getOrgName())
        .ownerEmail(openAppDTO.getOwnerEmail())
        .build();
  }

  /**
   * @see com.ctrip.framework.apollo.portal.controller.AppController#create(AppModel)
   */
  @Override
  public void createApp(OpenAppDTO openAppDTO) {
    final String appId = openAppDTO.getAppId();

    List<OpenAppDTO> openAppDTOList = this.getAppsInfo(Collections.singletonList(appId));
    if (null != openAppDTOList && !openAppDTOList.isEmpty()) {
      throw new BadRequestException("AppId " + appId + " exists already, cannot create again");
    }
    App app = convert(openAppDTO);
    appService.createAppAndAddRolePermission(app, openAppDTO.getAdmins());
  }

  @Override
  public List<OpenEnvClusterDTO> getEnvClusterInfo(String appId) {
    List<OpenEnvClusterDTO> envClusters = new LinkedList<>();

    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      OpenEnvClusterDTO envCluster = new OpenEnvClusterDTO();

      envCluster.setEnv(env.getName());
      List<ClusterDTO> clusterDTOs = clusterService.findClusters(env, appId);
      envCluster.setClusters(BeanUtils.toPropertySet("name", clusterDTOs));

      envClusters.add(envCluster);
    }

    return envClusters;
  }

  @Override
  public List<OpenAppDTO> getAllApps() {
    final List<App> apps = this.appService.findAll();
    return OpenApiBeanUtils.transformFromApps(apps);
  }

  @Override
  public List<OpenAppDTO> getAppsInfo(List<String> appIds) {
    final List<App> apps = this.appService.findByAppIds(new HashSet<>(appIds));
    return OpenApiBeanUtils.transformFromApps(apps);
  }

  @Override
  public List<OpenAppDTO> getAuthorizedApps() {
    throw new UnsupportedOperationException();
  }
}
