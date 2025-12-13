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

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenMissEnvDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppDeletionEvent;
import com.ctrip.framework.apollo.portal.listener.AppInfoChangedEvent;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
  private static final Logger logger = LoggerFactory.getLogger(ServerAppOpenApiService.class);

  public ServerAppOpenApiService(PortalSettings portalSettings, ClusterService clusterService,
      AppService appService, ApplicationEventPublisher publisher,
      RoleInitializationService roleInitializationService) {
    this.portalSettings = portalSettings;
    this.clusterService = clusterService;
    this.appService = appService;
    this.publisher = publisher;
    this.roleInitializationService = roleInitializationService;
  }

  private App convert(OpenAppDTO dto) {
    return App.builder().appId(dto.getAppId()).name(dto.getName()).ownerName(dto.getOwnerName())
        .orgId(dto.getOrgId()).orgName(dto.getOrgName()).ownerEmail(dto.getOwnerEmail()).build();
  }

  /**
   * @see com.ctrip.framework.apollo.portal.controller.AppController#create(AppModel)
   */
  @Override
  public OpenAppDTO createApp(OpenCreateAppDTO req) {
    App app = convert(req.getApp());
    Set<String> admins = req.getAdmins();
    if (admins == null) {
      admins = Collections.emptySet();
    }
    return OpenApiModelConverters.fromApp(appService.createAppAndAddRolePermission(app, admins));
  }

  @Override
  public List<OpenEnvClusterDTO> getEnvClusters(String appId) {
    List<OpenEnvClusterDTO> envClusters = new LinkedList<>();

    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      OpenEnvClusterDTO envCluster = new OpenEnvClusterDTO();

      envCluster.setEnv(env.getName());
      List<ClusterDTO> clusterDTOs = clusterService.findClusters(env, appId);
      Set<String> clusterNames = clusterDTOs == null ? Collections.emptySet()
          : BeanUtils.toPropertySet("name", clusterDTOs);
      envCluster.setClusters(new ArrayList<>(clusterNames));

      envClusters.add(envCluster);
    }

    return envClusters;
  }

  @Override
  public List<OpenAppDTO> getAllApps() {
    final List<App> apps = this.appService.findAll();
    return OpenApiModelConverters.fromApps(apps);
  }

  @Override
  public List<OpenAppDTO> getAppsInfo(List<String> appIds) {
    if (appIds == null || appIds.isEmpty()) {
      return Collections.emptyList();
    }
    final List<App> apps = this.appService.findByAppIds(new HashSet<>(appIds));
    return OpenApiModelConverters.fromApps(apps);
  }

  @Override
  public List<OpenAppDTO> getAuthorizedApps() {
    throw new UnsupportedOperationException();
  }

  /**
   * Updating Application Information - Using OpenAPI DTOs
   *
   * @param openAppDTO OpenAPI application DTO
   */
  @Override
  public void updateApp(OpenAppDTO openAppDTO) {
    App app = convert(openAppDTO);
    App updatedApp = appService.updateAppInLocal(app);
    publisher.publishEvent(new AppInfoChangedEvent(updatedApp));
  }

  /**
   * Get the current user's app list (paginated)
   *
   * @param page Pagination parameter
   * @return App list
   */
  @Override
  public List<OpenAppDTO> getAppsWithPageAndSize(Set<String> appIds, Integer page, Integer size) {
    int pageIndex = page == null ? 0 : page;
    int pageSize = (size == null || size <= 0) ? 20 : size;
    Pageable pageable = Pageable.ofSize(pageSize).withPage(pageIndex);
    Set<String> targetAppIds = appIds == null ? Collections.emptySet() : appIds;
    if (targetAppIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<App> apps = appService.findByAppIds(targetAppIds, pageable);
    return OpenApiModelConverters.fromApps(apps);
  }

  /**
   * Create an application in a specified environment
   *
   * @param env Environment
   * @param app Application information
   */
  @Override
  public void createAppInEnv(String env, OpenAppDTO app) {
    if (env == null) {
      throw BadRequestException.invalidEnvFormat("null");
    }
    Env envEnum;
    try {
      envEnum = Env.valueOf(env);
    } catch (IllegalArgumentException e) {
      throw BadRequestException.invalidEnvFormat(env);
    }
    App appEntity = convert(app);
    appService.createAppInRemoteNew(envEnum, appEntity);

    roleInitializationService.initNamespaceSpecificEnvRoles(appEntity.getAppId(),
        ConfigConsts.NAMESPACE_APPLICATION, env,
        UserIdentityContextHolder.getOperator().getUserId());
  }

  /**
   * Delete an application
   *
   * @param appId application ID
   * @return the deleted application
   */
  @Override
  public OpenAppDTO deleteApp(String appId) {
    App app = appService.deleteAppInLocal(appId);
    publisher.publishEvent(new AppDeletionEvent(app));
    return OpenApiModelConverters.fromApp(app);
  }

  /**
   * Find missing environments
   *
   * @param appId application ID
   * @return list of missing environments
   */
  public List<OpenMissEnvDTO> findMissEnvs(String appId) {
    List<OpenMissEnvDTO> missEnvs = new ArrayList<>();

    for (Env env : portalSettings.getActiveEnvs()) {
      try {
        appService.load(env, appId);
      } catch (Exception e) {
        OpenMissEnvDTO missEnv = new OpenMissEnvDTO();
        if (e instanceof HttpClientErrorException
            && ((HttpClientErrorException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
          missEnv.setCode(HttpStatus.OK.value());
          missEnv.setMessage(env.toString());
        } else {
          missEnv.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
          missEnv.setMessage(
              String.format("load appId:%s from env %s error.", appId, env) + e.getMessage());
        }
        missEnvs.add(missEnv);
      }
    }
    return missEnvs;
  }

  /**
   * Find AppNavTree
   *
   * @param appId
   * @return list of EnvClusterInfos
   */
  @Override
  public List<OpenEnvClusterInfo> getEnvClusterInfos(String appId) {
    List<OpenEnvClusterInfo> envClusterInfos = new ArrayList<>();
    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      try {
        OpenEnvClusterInfo openEnvClusterInfo =
            OpenApiModelConverters.fromEnvClusterInfo(appService.createEnvNavNode(env, appId));
        openEnvClusterInfo.setCode(HttpStatus.OK.value());
        envClusterInfos.add(openEnvClusterInfo);
      } catch (Exception e) {
        OpenEnvClusterInfo envClusterInfo = new OpenEnvClusterInfo();
        envClusterInfo.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        envClusterInfo.setEnv(env.getName());
        envClusterInfo.setMessage("load env:" + env.getName() + " cluster error." + e.getMessage());
        envClusterInfos.add(envClusterInfo);
      }
    }
    return envClusterInfos;
  }
}
