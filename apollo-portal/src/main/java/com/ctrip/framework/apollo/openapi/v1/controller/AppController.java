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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController("openapiAppController")
public class AppController implements AppManagementApi {

  private final ConsumerAuthUtil consumerAuthUtil;
  private final ConsumerService consumerService;
  private final AppOpenApiService appOpenApiService;
  private final UserService userService;
  private final UserInfoHolder userInfoHolder;
  private final RolePermissionService rolePermissionService;

  public AppController(final ConsumerAuthUtil consumerAuthUtil,
      final ConsumerService consumerService, final AppOpenApiService appOpenApiService,
      final UserService userService, final UserInfoHolder userInfoHolder,
      final RolePermissionService rolePermissionService) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerService = consumerService;
    this.appOpenApiService = appOpenApiService;
    this.userService = userService;
    this.userInfoHolder = userInfoHolder;
    this.rolePermissionService = rolePermissionService;
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
    if (!StringUtils.hasText(app.getAppId())) {
      throw new BadRequestException("AppId is null or blank");
    }
    String resolvedOperator = resolveOperator(app.getDataChangeCreatedBy());
    app.setDataChangeCreatedBy(resolvedOperator);
    app.setDataChangeLastModifiedBy(resolvedOperator);
    OpenAppDTO createdApp = this.appOpenApiService.createApp(req, resolvedOperator);
    if (Boolean.TRUE.equals(req.getAssignAppRoleToSelf())
        && UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      consumerService.assignAppRoleToConsumer(consumerId, app.getAppId(), resolvedOperator);
    }
    return ResponseEntity.ok(createdApp);
  }

  @Override
  public ResponseEntity<List<OpenEnvClusterInfo>> getEnvClusterInfo(String appId) {
    return ResponseEntity.ok(appOpenApiService.getEnvClusterInfo(appId));
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
    Set<String> appIds = findAppIdsAuthorizedByCurrentIdentity();
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
  public ResponseEntity<Void> updateApp(String appId, OpenAppDTO dto, String operator) {
    if (!Objects.equals(appId, dto.getAppId())) {
      throw new BadRequestException("The App Id of path variable and request body is different");
    }
    String resolvedOperator = resolveOperator(operator);
    dto.setDataChangeLastModifiedBy(resolvedOperator);
    appOpenApiService.updateApp(dto, resolvedOperator);

    return ResponseEntity.ok().build();
  }

  /**
   * Get the current Consumer's application list (paginated) (new added)
   */
  @Override
  public ResponseEntity<List<OpenAppDTO>> getAppsBySelf(Integer page, Integer size) {
    Set<String> authorizedAppIds = findAppIdsAuthorizedByCurrentIdentity();
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
  public ResponseEntity<Void> createAppInEnv(String env, OpenAppDTO app, String operator) {
    if (app == null) {
      throw new BadRequestException("App is null");
    }
    String resolvedOperator = resolveOperator(operator);
    app.setDataChangeCreatedBy(resolvedOperator);
    app.setDataChangeLastModifiedBy(resolvedOperator);
    appOpenApiService.createAppInEnv(env, app, resolvedOperator);

    return ResponseEntity.ok().build();
  }

  /**
   * Delete App (new added)
   */
  @Override
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "App.delete")
  public ResponseEntity<Void> deleteApp(String appId, String operator) {
    String resolvedOperator = resolveOperator(operator);
    appOpenApiService.deleteApp(appId, resolvedOperator);
    return ResponseEntity.ok().build();
  }

  /**
   * Find miss env (new added)
   */
  @Override
  public ResponseEntity<List<OpenMissEnvDTO>> findMissEnvs(String appId) {
    return ResponseEntity.ok(appOpenApiService.findMissEnvs(appId));
  }

  private String resolveOperator(String operator) {
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      UserInfo loginUser = userInfoHolder.getUser();
      if (loginUser == null || !StringUtils.hasText(loginUser.getUserId())) {
        throw new BadRequestException("Current user not found");
      }
      return loginUser.getUserId();
    }

    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      if (!StringUtils.hasText(operator)) {
        throw new BadRequestException("operator should not be null or empty");
      }
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      return operator;
    }

    throw new BadRequestException("Unsupported auth type: %s",
        UserIdentityContextHolder.getAuthType());
  }

  private Set<String> findAppIdsAuthorizedByCurrentIdentity() {
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      UserInfo loginUser = userInfoHolder.getUser();
      if (loginUser == null || !StringUtils.hasText(loginUser.getUserId())) {
        return Collections.emptySet();
      }
      Set<String> appIds = new LinkedHashSet<>();
      List<Role> userRoles = rolePermissionService.findUserRoles(loginUser.getUserId());
      if (userRoles == null) {
        return appIds;
      }
      for (Role role : userRoles) {
        if (role == null || !StringUtils.hasText(role.getRoleName())) {
          continue;
        }
        String appId = RoleUtils.extractAppIdFromRoleName(role.getRoleName());
        if (appId != null) {
          appIds.add(appId);
        }
      }
      return appIds;
    }

    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      return this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);
    }

    throw new BadRequestException("Unsupported auth type: %s",
        UserIdentityContextHolder.getAuthType());
  }
}
