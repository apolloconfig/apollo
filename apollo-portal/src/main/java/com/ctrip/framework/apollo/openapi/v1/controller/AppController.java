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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.server.service.ServerAppOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;

@RestController("openapiAppController")
@RequestMapping("/openapi/v1")
public class AppController {

  private final ConsumerAuthUtil consumerAuthUtil;
  private final ConsumerService consumerService;
  private final ServerAppOpenApiService serverAppOpenApiService;

  public AppController(
      final ConsumerAuthUtil consumerAuthUtil,
      final ConsumerService consumerService,
      final ServerAppOpenApiService serverAppOpenApiService) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerService = consumerService;
    this.serverAppOpenApiService = serverAppOpenApiService;
  }

  /**
   * 创建应用
   * POST /openapi/v1/apps
   */
  @Transactional
  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateApplicationPermission()")
  @PostMapping(value = "/apps")
  @ApolloAuditLog(type = OpType.CREATE, name = "App.create")
  public ResponseEntity<OpenAppDTO> createApp(@Valid @RequestBody OpenCreateAppDTO req) {
    if (null == req.getApp()) {
      throw new BadRequestException("App is null");
    }
    
    OpenAppDTO appDto = req.getApp();
    if (null == appDto.getAppId()) {
      throw new BadRequestException("AppId is null");
    }
    
    // 直接调用ServerAppOpenApiService，让Service层处理DTO转换
    serverAppOpenApiService.createApp(req);
    
    if (req.isAssignAppRoleToSelf()) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      consumerService.assignAppRoleToConsumer(consumerId, appDto.getAppId());
    }
    
    return ResponseEntity.ok(appDto);
  }

  /**
   * 获取应用的环境集群信息
   * GET /openapi/v1/apps/{appId}/env-clusters
   */
  @GetMapping(value = "/apps/{appId}/env-clusters")
  public ResponseEntity<List<OpenEnvClusterDTO>> getAppEnvClusters(@PathVariable String appId) {
      List<OpenEnvClusterDTO> envClusters = serverAppOpenApiService.getEnvClusterInfo(appId);
      return ResponseEntity.ok(envClusters);
  }


  /**
   * 查询应用列表
   * GET /openapi/v1/apps?appIds=app1,app2,app3 (可选参数)
   */
  @GetMapping("/apps")
  public ResponseEntity<List<OpenAppDTO>> getApps(@RequestParam(value = "appIds", required = false) String appIds) {
    List<OpenAppDTO> apps;
    
    if (StringUtils.hasText(appIds)) {
      // 查询指定的应用列表
      apps = serverAppOpenApiService.getAppsInfo(Arrays.asList(appIds.split(",")));
    } else {
      // 如果appIds为空，则查找consumerToken能够管理的所有app
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      Set<String> authorizedAppIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);
      apps = serverAppOpenApiService.getAppsInfo(new ArrayList<>(authorizedAppIds));
    }
    
    return ResponseEntity.ok(apps);
  }

  /**
   * 获取当前Consumer授权的应用列表
   * GET /openapi/v1/apps?authorized=true
   */
  @GetMapping(value = "/apps", params = "authorized=true")
  public ResponseEntity<List<OpenAppDTO>> getAuthorizedApps() {
    long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
    Set<String> appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);
    List<OpenAppDTO> apps = serverAppOpenApiService.getAppsInfo(new ArrayList<>(appIds));
    return ResponseEntity.ok(apps);
  }

  /**
   * 获取单个应用信息
   * GET /openapi/v1/apps/{appId}
   */
  @GetMapping("/apps/{appId}")
  public ResponseEntity<OpenAppDTO> getApp(@PathVariable String appId) {
    List<OpenAppDTO> apps = serverAppOpenApiService.getAppsInfo(Arrays.asList(appId));
    if (apps.isEmpty()) {
      throw new BadRequestException("App not found: " + appId);
    }
    return ResponseEntity.ok(apps.get(0));
  }

  /**
   * 更新应用
   * PUT /openapi/v1/apps/{appId}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyApplicationPermission(#appId)")
  @PutMapping("/apps/{appId}")
  @ApolloAuditLog(type = OpType.UPDATE, name = "App.update")
  public ResponseEntity<OpenAppDTO> updateApp(@PathVariable String appId, @Valid @RequestBody OpenAppDTO dto) {
    if (!Objects.equals(appId, dto.getAppId())) {
      throw new BadRequestException("The App Id of path variable and request body is different");
    }
    
    // 直接调用ServerAppOpenApiService，让Service层处理DTO转换
    serverAppOpenApiService.updateApp(dto);
    
    return ResponseEntity.ok(dto);
  }
}

