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
  public OpenAppDTO createApp(OpenCreateAppDTO req, String operator) {
    App app = convert(req.getApp());
    Set<String> admins = req.getAdmins();
    if (admins == null) {
      admins = Collections.emptySet();
    }
    App createdApp = appService.createAppAndAddRolePermission(app, admins, operator);
    return OpenApiModelConverters.fromApp(createdApp);
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
  public List<OpenEnvClusterInfo> getEnvClusterInfo(String appId) {
    List<OpenEnvClusterInfo> result = new ArrayList<>();
    for (Env env : portalSettings.getActiveEnvs()) {
      try {
        OpenEnvClusterInfo info =
            OpenApiModelConverters.fromEnvClusterInfo(appService.createEnvNavNode(env, appId));
        info.setCode(HttpStatus.OK.value());
        info.setMessage(HttpStatus.OK.getReasonPhrase());
        result.add(info);
      } catch (Exception e) {
        logger.warn("Failed to load env {} navigation for app {}", env, appId, e);
        OpenEnvClusterInfo info = new OpenEnvClusterInfo();
        info.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        info.setMessage("load env:" + env.getName() + " cluster error.");
        info.setEnv(env.getName());
        info.setClusters(Collections.emptyList());
        result.add(info);
      }
    }
    return result;
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
   * @param openAppDTO OpenAPI application DTO
   */
  @Override
  public void updateApp(OpenAppDTO openAppDTO, String operator) {
    App app = convert(openAppDTO);
    App updatedApp = appService.updateAppInLocal(app, operator);
    publisher.publishEvent(new AppInfoChangedEvent(updatedApp));
  }

  /**
   * Get the current user's app list (paginated)
   * @param page Pagination parameter
   * @return App list
   */
  @Override
  public List<OpenAppDTO> getAppsBySelf(Set<String> appIds, Integer page, Integer size) {
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
   * @param env Environment
   * @param app Application information
   * @param operator Operator
   */
  @Override
  public void createAppInEnv(String env, OpenAppDTO app, String operator) {
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
    appService.createAppInRemote(envEnum, appEntity, operator);

    roleInitializationService.initNamespaceSpecificEnvRoles(appEntity.getAppId(),
        ConfigConsts.NAMESPACE_APPLICATION, env, operator);
  }

  /**
   * Delete an application
   * @param appId application ID
   * @return the deleted application
   */
  @Override
  public OpenAppDTO deleteApp(String appId, String operator) {
    App app = appService.deleteAppInLocal(appId, operator);
    publisher.publishEvent(new AppDeletionEvent(app));
    return OpenApiModelConverters.fromApp(app);
  }

  /**
   * Find missing environments
   * @param appId application ID
   * @return list of missing environments
   */
  public List<OpenMissEnvDTO> findMissEnvs(String appId) {
    List<OpenMissEnvDTO> response = new ArrayList<>();
    for (Env env : portalSettings.getActiveEnvs()) {
      try {
        appService.load(env, appId);
      } catch (Exception e) {
        OpenMissEnvDTO entity = new OpenMissEnvDTO();
        if (e instanceof HttpClientErrorException
            && ((HttpClientErrorException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
          entity.setCode(HttpStatus.OK.value());
          entity.setMessage(env.toString());
        } else {
          logger.warn("Failed to load app {} from env {}", appId, env, e);
          entity.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
          entity.setMessage(String.format("load appId:%s from env %s error.", appId, env));
        }
        response.add(entity);
      }
    }
    return response;
  }
}
