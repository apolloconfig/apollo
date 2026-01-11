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
package com.ctrip.framework.apollo.openapi.v1.controller;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.api.AppManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenMissEnvDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import java.util.HashSet;
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
  private final RolePermissionService rolePermissionService;
  private final UserInfoHolder userInfoHolder;

  public AppController(final ConsumerAuthUtil consumerAuthUtil,
      final ConsumerService consumerService, final AppOpenApiService appOpenApiService,
      final UserService userService, final RolePermissionService rolePermissionService,
      final UserInfoHolder userInfoHolder) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerService = consumerService;
    this.appOpenApiService = appOpenApiService;
    this.userService = userService;
    this.rolePermissionService = rolePermissionService;
    this.userInfoHolder = userInfoHolder;
  }

  /**
   * @see com.ctrip.framework.apollo.portal.controller.AppController#create(AppModel)
   */
  @Transactional
  @PreAuthorize(value = "@unifiedPermissionValidator.hasCreateApplicationPermission()")
  @Override
  public ResponseEntity<OpenAppDTO> createApp(OpenCreateAppDTO req) {
    if (null == req.getApp()) {
      throw new BadRequestException("App is null");
    }
    final OpenAppDTO app = req.getApp();
    if (null == app.getAppId()) {
      throw new BadRequestException("AppId is null");
    }
    String operator =
        app.getDataChangeCreatedBy() == null ? app.getOwnerName() : app.getDataChangeCreatedBy();
    if (userService.findByUserId(operator) == null) {
      throw new BadRequestException("operator missing or not exist: " + operator);
    }
    UserIdentityContextHolder.setOperator(new UserInfo(operator));
    // create app
    OpenAppDTO openAppDTO = this.appOpenApiService.createApp(req);
    if (Boolean.TRUE.equals(req.getAssignAppRoleToSelf())
        && UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      consumerService.assignAppRoleToConsumer(consumerId, app.getAppId());
    }
    UserIdentityContextHolder.clear();
    return ResponseEntity.ok(openAppDTO);
  }

  @Override
  public ResponseEntity<List<OpenEnvClusterDTO>> getEnvClusters(String appId) {
    return ResponseEntity.ok(appOpenApiService.getEnvClusters(appId));
  }

  @Override
  public ResponseEntity<List<OpenAppDTO>> findApps(String appIds) {
    if (StringUtils.hasText(appIds)) {
      return ResponseEntity
          .ok(this.appOpenApiService.getAppsInfo(Arrays.asList(appIds.split(","))));
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
    OpenAppDTO result = apps.get(0);
    result.setOwnerDisplayName(result.getOwnerName());

    return ResponseEntity.ok(result);
  }

  /**
   * update app (new added)
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "App.update")
  public ResponseEntity<Void> updateApp(String appId, OpenAppDTO dto, String operator) {
    if (!Objects.equals(appId, dto.getAppId())) {
      throw new BadRequestException("The App Id of path variable and request body is different");
    }
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      UserIdentityContextHolder.setOperator(new UserInfo(operator));
    }
    appOpenApiService.updateApp(dto);

    return ResponseEntity.ok().build();
  }

  /**
   * Get the current Consumer's application list (paginated) (new added)
   */
  @Override
  public ResponseEntity<List<OpenAppDTO>> getAppsBySelf(Integer page, Integer size) {
    Set<String> appIds = new HashSet<>();
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);
    } else {
      String userId = userInfoHolder.getUser().getUserId();
      List<Role> userRoles = rolePermissionService.findUserRoles(userId);

      for (Role role : userRoles) {
        String appId = RoleUtils.extractAppIdFromRoleName(role.getRoleName());
        if (appId != null) {
          appIds.add(appId);
        }
      }
    }

    List<OpenAppDTO> apps = appOpenApiService.getAppsWithPageAndSize(appIds, page, size);
    return ResponseEntity.ok(apps);
  }

  /**
   * Create an application in a specified environment (new added) POST /openapi/v1/apps/envs/{env}
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.hasCreateApplicationPermission()")
  @ApolloAuditLog(type = OpType.CREATE, name = "App.create.forEnv")
  public ResponseEntity<Void> createAppInEnv(String env, OpenAppDTO app, String operator) {
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      UserIdentityContextHolder.setOperator(new UserInfo(operator));
    }
    appOpenApiService.createAppInEnv(env, app);

    return ResponseEntity.ok().build();
  }

  /**
   * Delete App (new added)
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "App.delete")
  public ResponseEntity<Void> deleteApp(String appId, String operator) {
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      UserIdentityContextHolder.setOperator(new UserInfo(operator));
    }
    appOpenApiService.deleteApp(appId);
    return ResponseEntity.ok().build();
  }

  /**
   * Find miss env (new added)
   */
  @Override
  public ResponseEntity<List<OpenMissEnvDTO>> findMissEnvs(String appId) {
    return ResponseEntity.ok(appOpenApiService.findMissEnvs(appId));
  }

  /**
   * Find appNavTree (new added)
   */
  @Override
  public ResponseEntity<List<OpenEnvClusterInfo>> getEnvClusterInfo(String appId) {
    return ResponseEntity.ok(appOpenApiService.getEnvClusterInfos(appId));
  }
}
