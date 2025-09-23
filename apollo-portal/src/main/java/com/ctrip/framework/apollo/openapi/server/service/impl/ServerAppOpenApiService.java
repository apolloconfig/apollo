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
package com.ctrip.framework.apollo.openapi.server.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.http.MultiResponseEntity;
import com.ctrip.framework.apollo.common.http.RichResponseEntity;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppDeletionEvent;
import com.ctrip.framework.apollo.portal.listener.AppInfoChangedEvent;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;

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

  public ServerAppOpenApiService(
      PortalSettings portalSettings,
      ClusterService clusterService,
      AppService appService,
      ApplicationEventPublisher publisher,
      RoleInitializationService roleInitializationService) {
    this.portalSettings = portalSettings;
    this.clusterService = clusterService;
    this.appService = appService;
    this.publisher = publisher;
    this.roleInitializationService = roleInitializationService;
  }

  private App convert(OpenAppDTO dto) {
    return App.builder()
        .appId(dto.getAppId())
        .name(dto.getName())
        .ownerName(dto.getOwnerName())
        .orgId(dto.getOrgId())
        .orgName(dto.getOrgName())
        .ownerEmail(dto.getOwnerEmail())
        .build();
  }

  /**
   * @see com.ctrip.framework.apollo.portal.controller.AppController#create(AppModel)
   */
  @Override
  public void createApp(OpenCreateAppDTO req) {
      App app = null;
      if (req.getApp() != null) {
          app = convert(req.getApp());
      }
      appService.createAppAndAddRolePermission(app, new HashSet<>(req.getAdmins()));
  }

  @Override
  public List<OpenEnvClusterDTO> getEnvClusterInfo(String appId) {
    List<OpenEnvClusterDTO> envClusters = new LinkedList<>();

    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      OpenEnvClusterDTO envCluster = new OpenEnvClusterDTO();

      envCluster.setEnv(env.getName());
      List<ClusterDTO> clusterDTOs = clusterService.findClusters(env, appId);
      envCluster.setClusters(new ArrayList<>(BeanUtils.toPropertySet("name", clusterDTOs)));

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

  /**
   * 更新应用信息 - 使用OpenAPI DTO
   * @param openAppDTO OpenAPI应用DTO
   */
  @Override
  public void updateApp(OpenAppDTO openAppDTO) {
    App app = convert(openAppDTO);
    App updatedApp = appService.updateAppInLocal(app);
    publisher.publishEvent(new AppInfoChangedEvent(updatedApp));
  }

  /**
   * 获取当前用户的应用列表（分页）
   * @param page 分页参数
   * @return 应用列表
   */
  @Override
  public List<OpenAppDTO> getAppsBySelf(Set<String> appIds, Pageable page) {
    List<App> apps = appService.findByAppIds(appIds, page);
    return OpenApiBeanUtils.transformFromApps(apps);
  }

  /**
   * 获取应用导航树信息
   * @param appId 应用ID
   * @return 导航树信息
   */
  @Override
  public MultiResponseEntity<EnvClusterInfo> getAppNavTree(String appId) {
    return getEnvClusterInfoMultiResponseEntity(appId, portalSettings, appService);
  }

  public static MultiResponseEntity<EnvClusterInfo> getEnvClusterInfoMultiResponseEntity(String appId, PortalSettings portalSettings, AppService appService) {
    MultiResponseEntity<EnvClusterInfo> response = MultiResponseEntity.ok();
    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      try {
        response.addResponseEntity(RichResponseEntity.ok(appService.createEnvNavNode(env, appId)));
      } catch (Exception e) {
        response.addResponseEntity(RichResponseEntity.error(HttpStatus.INTERNAL_SERVER_ERROR,
            "load env:" + env.getName() + " cluster error." + e.getMessage()));
      }
    }
    return response;
  }

  /**
   * 在指定环境创建应用
   * @param env 环境
   * @param app 应用信息
   * @param operator 操作人
   */
  public void createAppInEnv(String env, OpenAppDTO app, String operator) {
    App appEntity = convert(app);
    appService.createAppInRemote(Env.valueOf(env), appEntity);
    
    roleInitializationService.initNamespaceSpecificEnvRoles(appEntity.getAppId(), 
        ConfigConsts.NAMESPACE_APPLICATION, env, operator);
  }

  /**
   * 删除应用
   * @param appId 应用ID
   * @return 被删除的应用
   */
  public OpenAppDTO deleteApp(String appId) {
    App app = appService.deleteAppInLocal(appId);
    publisher.publishEvent(new AppDeletionEvent(app));
    return OpenApiBeanUtils.transformFromApp(app);
  }

  /**
   * 查找缺失的环境
   * @param appId 应用ID
   * @return 缺失环境列表
   */
  public MultiResponseEntity<String> findMissEnvs(String appId) {
    MultiResponseEntity<String> response = MultiResponseEntity.ok();
    for (Env env : portalSettings.getActiveEnvs()) {
      try {
        appService.load(env, appId);
      } catch (Exception e) {
        if (e instanceof HttpClientErrorException &&
            ((HttpClientErrorException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
          response.addResponseEntity(RichResponseEntity.ok(env.toString()));
        } else {
          response.addResponseEntity(RichResponseEntity.error(HttpStatus.INTERNAL_SERVER_ERROR,
              String.format("load appId:%s from env %s error.", appId, env) + e.getMessage()));
        }
      }
    }
    return response;
  }
}
