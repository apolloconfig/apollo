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
package com.ctrip.framework.apollo.openapi.v1.controller;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.api.AppManagementApi;
import com.ctrip.framework.apollo.openapi.model.MultiResponseEntity;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController("openapiAppController")
public class AppController implements AppManagementApi {

  private final ConsumerAuthUtil consumerAuthUtil;
  private final ConsumerService consumerService;
  private final AppOpenApiService appOpenApiService;
  private final UserService userService;

  public AppController(
          final ConsumerAuthUtil consumerAuthUtil,
          final ConsumerService consumerService,
          final AppOpenApiService appOpenApiService,
          final UserService userService) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerService = consumerService;
    this.appOpenApiService = appOpenApiService;
    this.userService = userService;
  }

  /**
   * @see com.ctrip.framework.apollo.portal.controller.AppController#create(AppModel)
   */
  @Transactional
  @PreAuthorize(value = "@unifiedPermissionValidator.hasCreateApplicationPermission()")
  @Override
  public ResponseEntity<Object> createApp(OpenCreateAppDTO req) {
    if (null == req.getApp()) {
      throw new BadRequestException("App is null");
    }
    final OpenAppDTO app = req.getApp();
    if (null == app.getAppId()) {
      throw new BadRequestException("AppId is null");
    }
    // create app
    this.appOpenApiService.createApp(req);
    if (Boolean.TRUE.equals(req.getAssignAppRoleToSelf())) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      consumerService.assignAppRoleToConsumer(consumerId, app.getAppId());
    }
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<OpenEnvClusterDTO>> getEnvClusterInfo(String appId) {
    return ResponseEntity.ok(appOpenApiService.getEnvClusterInfo(appId));
  }

  @Override
  public ResponseEntity<List<OpenAppDTO>> findApps(String appIds) {
    if (StringUtils.hasText(appIds)) {
      return ResponseEntity.ok(this.appOpenApiService.getAppsInfo(Arrays.asList(appIds.split(","))));
    } else {
      return ResponseEntity.ok(this.appOpenApiService.getAllApps());
    }
  }

  /**
   * @return which apps can be operated by open api
   */
  @Override
  public ResponseEntity<List<OpenAppDTO>> findAppsAuthorized() {
    long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();

    Set<String> appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);

    return ResponseEntity.ok(appOpenApiService.getAppsInfo(new ArrayList<>(appIds)));
  }

  /**
   * get single app info (new added)
   */
  @Override
  public ResponseEntity<OpenAppDTO> getApp(String appId) {
    List<OpenAppDTO> apps = appOpenApiService.getAppsInfo(Collections.singletonList(appId));
    if (null == apps || apps.isEmpty()) {
      throw new BadRequestException("App not found: " + appId);
    }
    return ResponseEntity.ok(apps.get(0));
  }

  /**
   * update app (new added)
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "App.update")
  public ResponseEntity<OpenAppDTO> updateApp(String appId, String operator, OpenAppDTO dto) {
    if (!Objects.equals(appId, dto.getAppId())) {
      throw new BadRequestException("The App Id of path variable and request body is different");
    }
    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }
    appOpenApiService.updateApp(dto);

    return ResponseEntity.ok(dto);
  }

  /**
   * Get the current Consumer's application list (paginated) (new added)
   */
  @Override
  public ResponseEntity<List<OpenAppDTO>> getAppsBySelf(Integer page, Integer size) {
    long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
    Set<String> authorizedAppIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);
    List<OpenAppDTO> apps = appOpenApiService.getAppsBySelf(authorizedAppIds, page, size);
    return ResponseEntity.ok(apps);
  }

  /**
   * Create an application in a specified environment (new added)
   * POST /openapi/v1/apps/envs/{env}
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasCreateApplicationPermission()")
  @ApolloAuditLog(type = OpType.CREATE, name = "App.create.forEnv")
  public ResponseEntity<Object> createAppInEnv(String env, String operator, OpenAppDTO app) {
    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }
    appOpenApiService.createAppInEnv(env, app, operator);

    return ResponseEntity.ok().build();
  }

  /**
   * Delete App (new added)
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "App.delete")
  public ResponseEntity<Object> deleteApp(String appId, String operator) {
    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }
    appOpenApiService.deleteApp(appId);
    return ResponseEntity.ok().build();
  }

  /**
   * Find miss env (new added)
   */
  @Override
  public ResponseEntity<MultiResponseEntity> findMissEnvs(String appId) {
    return ResponseEntity.ok(appOpenApiService.findMissEnvs(appId));
  }

  /**
   * Find appNavTree (new added)
   */
  @Override
  public ResponseEntity<MultiResponseEntity> getAppNavTree(String appId) {
    return ResponseEntity.ok(appOpenApiService.getAppNavTree(appId));
  }
}
