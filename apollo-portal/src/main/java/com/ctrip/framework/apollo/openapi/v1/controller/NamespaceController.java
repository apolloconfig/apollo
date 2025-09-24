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

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppNamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.server.service.NamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceDeletionEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@RestController("openapiNamespaceController")
public class NamespaceController {

  private final UserService userService;
  private final NamespaceOpenApiService namespaceOpenApiService;
  private final AppNamespaceOpenApiService appNamespaceOpenApiService;
  private final NamespaceService namespaceService;
  private final AppNamespaceService appNamespaceService;
  private final ApplicationEventPublisher publisher;

  public NamespaceController(
      final UserService userService,
      NamespaceOpenApiService namespaceOpenApiService,
      AppNamespaceOpenApiService appNamespaceOpenApiService,
      NamespaceService namespaceService,
      AppNamespaceService appNamespaceService,
      ApplicationEventPublisher publisher) {
    this.userService = userService;
    this.namespaceOpenApiService = namespaceOpenApiService;
    this.appNamespaceOpenApiService = appNamespaceOpenApiService;
    this.namespaceService = namespaceService;
    this.appNamespaceService = appNamespaceService;
    this.publisher = publisher;
  }

  /**
   * 创建AppNamespace
   * POST /openapi/v1/apps/{appId}/appnamespaces
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateNamespacePermission(#appId)")
  @PostMapping(value = "/openapi/v1/apps/{appId}/appnamespaces")
  public ResponseEntity<OpenAppNamespaceDTO> createAppNamespace(@PathVariable String appId,
                                                                @RequestBody OpenAppNamespaceDTO appNamespaceDTO) {

    if (!Objects.equals(appId, appNamespaceDTO.getAppId())) {
      throw new BadRequestException("AppId not equal. AppId in path = %s, AppId in payload = %s", appId,
                                                  appNamespaceDTO.getAppId());
    }
    RequestPrecondition.checkArgumentsNotEmpty(appNamespaceDTO.getAppId(), appNamespaceDTO.getName(),
                                               appNamespaceDTO.getFormat(), appNamespaceDTO.getDataChangeCreatedBy());

    if (!InputValidator.isValidAppNamespace(appNamespaceDTO.getName())) {
      throw BadRequestException.invalidNamespaceFormat(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE + " & "
                                                  + InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE);
    }

    if (!ConfigFileFormat.isValidFormat(appNamespaceDTO.getFormat())) {
      throw BadRequestException.invalidNamespaceFormat(appNamespaceDTO.getFormat());
    }

    String operator = appNamespaceDTO.getDataChangeCreatedBy();
    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }

    OpenAppNamespaceDTO created = this.namespaceOpenApiService.createAppNamespace(appNamespaceDTO);
    
    return ResponseEntity.ok(created);
  }

  /**
   * 获取指定集群下的所有Namespace
   * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces
   */
  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces")
  public ResponseEntity<List<OpenNamespaceDTO>> findNamespaces(@PathVariable String appId, @PathVariable String env,
                                                              @PathVariable String clusterName,
                                                              @RequestParam(defaultValue = "true") boolean fillItemDetail) {
    List<OpenNamespaceDTO> namespaces = this.namespaceOpenApiService.getNamespaces(appId, env, clusterName, fillItemDetail);
    return ResponseEntity.ok(namespaces);
  }

  /**
   * 获取指定的Namespace
   * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}
   */
  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}")
  public ResponseEntity<OpenNamespaceDTO> loadNamespace(@PathVariable String appId, @PathVariable String env,
                                                       @PathVariable String clusterName, @PathVariable String namespaceName,
                                                       @RequestParam(defaultValue = "true") boolean fillItemDetail) {
    OpenNamespaceDTO namespace = this.namespaceOpenApiService.getNamespace(appId, env, clusterName, namespaceName, fillItemDetail);
    if (namespace == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(namespace);
  }

  /**
   * 获取Namespace的锁状态
   * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock
   */
  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock")
  public ResponseEntity<OpenNamespaceLockDTO> getNamespaceLock(@PathVariable String appId, @PathVariable String env,
                                                               @PathVariable String clusterName,
                                                               @PathVariable String namespaceName) {
    OpenNamespaceLockDTO lock = this.namespaceOpenApiService.getNamespaceLock(appId, env, clusterName, namespaceName);
    if (lock == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(lock);
  }

  // ======================= AppNamespace资源管理 =======================
  
  /**
   * 获取所有公共AppNamespace
   * GET /openapi/v1/appnamespaces?public=true
   */
  @GetMapping("/openapi/v1/appnamespaces")
  public ResponseEntity<List<OpenAppNamespaceDTO>> getAppNamespaces(@RequestParam(defaultValue = "false") boolean publicOnly) {
    List<OpenAppNamespaceDTO> appNamespaces;
    if (publicOnly) {
      appNamespaces = appNamespaceOpenApiService.findPublicAppNamespaces();
    } else {
      appNamespaces = appNamespaceOpenApiService.findAll();
    }
    return ResponseEntity.ok(appNamespaces);
  }

  /**
   * 获取指定应用的AppNamespace
   * GET /openapi/v1/apps/{appId}/appnamespaces
   */
  @GetMapping("/openapi/v1/apps/{appId}/appnamespaces")
  public ResponseEntity<List<OpenAppNamespaceDTO>> getAppNamespacesByApp(@PathVariable String appId) {
    List<OpenAppNamespaceDTO> appNamespaces = appNamespaceOpenApiService.findByAppId(appId);
    return ResponseEntity.ok(appNamespaces);
  }

  /**
   * 获取指定的AppNamespace
   * GET /openapi/v1/apps/{appId}/appnamespaces/{namespaceName}
   */
  @GetMapping("/openapi/v1/apps/{appId}/appnamespaces/{namespaceName:.+}")
  public ResponseEntity<OpenAppNamespaceDTO> getAppNamespace(@PathVariable String appId, @PathVariable String namespaceName) {
    OpenAppNamespaceDTO appNamespace = appNamespaceOpenApiService.findByAppIdAndName(appId, namespaceName);
    
    if (appNamespace == null) {
      throw BadRequestException.appNamespaceNotExists(appId, namespaceName);
    }

    return ResponseEntity.ok(appNamespace);
  }

  /**
   * 删除AppNamespace
   * DELETE /openapi/v1/apps/{appId}/appnamespaces/{namespaceName}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasDeleteNamespacePermission(#appId)")
  @DeleteMapping("/openapi/v1/apps/{appId}/appnamespaces/{namespaceName:.+}")
  public ResponseEntity<Void> deleteAppNamespace(@PathVariable String appId, @PathVariable String namespaceName) {
    OpenAppNamespaceDTO deletedAppNamespace = appNamespaceOpenApiService.deleteAppNamespace(appId, namespaceName);
    
    // 转换为AppNamespace实体用于事件发布
    AppNamespace appNamespace = OpenApiBeanUtils.transformToAppNamespace(deletedAppNamespace);
    publisher.publishEvent(new AppNamespaceDeletionEvent(appNamespace));

    return ResponseEntity.noContent().build();
  }

 
  // ======================= 发布状态管理 =======================
  
  /**
   * 获取应用下所有Namespace的发布状态
   * GET /openapi/v1/apps/{appId}/namespaces/releases/status
   */
  @GetMapping("/openapi/v1/apps/{appId}/namespaces/releases/status")
  public ResponseEntity<Map<String, Map<String, Boolean>>> getNamespacesReleaseStatus(@PathVariable String appId) {
    Map<String, Map<String, Boolean>> status = namespaceService.getNamespacesPublishInfo(appId);
    return ResponseEntity.ok(status);
  }

  /**
   * 获取公共AppNamespace的所有实例
   * GET /openapi/v1/envs/{env}/appnamespaces/{publicNamespaceName}/instances
   */
  @GetMapping("/openapi/v1/envs/{env}/appnamespaces/{publicNamespaceName}/instances")
  public ResponseEntity<List<OpenNamespaceDTO>> getPublicAppNamespaceInstances(@PathVariable String env,
                                                                               @PathVariable String publicNamespaceName,
                                                                               @RequestParam(name = "page", defaultValue = "0") int page,
                                                                               @RequestParam(name = "size", defaultValue = "10") int size) {
    List<OpenNamespaceDTO> instances = namespaceService.getPublicAppNamespaceAllNamespaces(Env.valueOf(env), publicNamespaceName, page, size)
        .stream()
        .map(OpenApiBeanUtils::transformFromNamespaceDTO)
        .collect(Collectors.toList());
    return ResponseEntity.ok(instances);
  }

  /**
   * 获取关联的公共Namespace
   * GET /openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/public-association
   */
  @GetMapping("/openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/public-association")
  public ResponseEntity<OpenNamespaceDTO> getPublicNamespaceAssociation(@PathVariable String appId,
                                                                        @PathVariable String env,
                                                                        @PathVariable String clusterName,
                                                                        @PathVariable String namespaceName) {
    NamespaceBO namespaceBO = namespaceService.findPublicNamespaceForAssociatedNamespace(Env.valueOf(env), appId, clusterName, namespaceName);
    if (namespaceBO == null) {
      return ResponseEntity.notFound().build();
    }
    OpenNamespaceDTO namespace = OpenApiBeanUtils.transformFromNamespaceBO(namespaceBO);
    return ResponseEntity.ok(namespace);
  }

  // ======================= Namespace完整性检查和修复 =======================
  
  /**
   * 检查缺失的Namespace
   * GET /openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/integrity-check
   */
  @GetMapping("/openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/integrity-check")
  public ResponseEntity<List<String>> checkNamespaceIntegrity(@PathVariable String appId, 
                                                              @PathVariable String env, 
                                                              @PathVariable String clusterName) {
    Set<String> missingNamespaces = findMissingNamespaceNames(appId, env, clusterName);
    List<String> result = Lists.newArrayList(missingNamespaces);
    return ResponseEntity.ok(result);
  }

  /**
   * 删除关联的Namespace
   * DELETE /openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/links
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasDeleteNamespacePermission(#appId)")
  @DeleteMapping("/openapi/v1/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName:.+}/links")
  public ResponseEntity<Void> deleteNamespaceLinks(@PathVariable String appId, 
                                                   @PathVariable String env,
                                                   @PathVariable String clusterName, 
                                                   @PathVariable String namespaceName) {
    namespaceService.deleteNamespace(appId, Env.valueOf(env), clusterName, namespaceName);
    return ResponseEntity.noContent().build();
  }

  // ======================= 私有辅助方法 =======================
  
  /**
   * 查找缺失的Namespace名称
   */
  private Set<String> findMissingNamespaceNames(String appId, String env, String clusterName) {
    // 获取配置数据库中的Namespace列表
    List<OpenNamespaceDTO> configDbNamespaces = namespaceService.findNamespaces(appId, Env.valueOf(env), clusterName)
        .stream()
        .map(OpenApiBeanUtils::transformFromNamespaceDTO)
        .collect(Collectors.toList());
    List<AppNamespace> portalDbAppNamespaces = appNamespaceService.findByAppId(appId);

    Set<String> configDbNamespaceNames = configDbNamespaces.stream()
        .map(OpenNamespaceDTO::getNamespaceName)
        .collect(Collectors.toSet());

    Set<String> portalDbPrivateAppNamespaceNames = Sets.newHashSet();

    for (AppNamespace appNamespace : portalDbAppNamespaces) {
      if (!appNamespace.isPublic()) {
        portalDbPrivateAppNamespaceNames.add(appNamespace.getName());
      }
    }

    // 私有namespace应该全部存在
    return Sets.difference(portalDbPrivateAppNamespaceNames, configDbNamespaceNames);
  }

}
