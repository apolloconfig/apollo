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

import static com.ctrip.framework.apollo.common.utils.RequestPrecondition.checkModel;

import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceUsageDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.vo.LockInfo;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceUsage;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceDeletionEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Server-side Namespace OpenAPI implementation for generated management contracts.
 */
@Service
public class ServerNamespaceManagementOpenApiService implements NamespaceOpenApiService {

  private static final Logger logger =
      LoggerFactory.getLogger(ServerNamespaceManagementOpenApiService.class);

  private final AppNamespaceService appNamespaceService;
  private final ApplicationEventPublisher publisher;
  private final NamespaceService namespaceService;
  private final NamespaceLockService namespaceLockService;
  private final RoleInitializationService roleInitializationService;
  private final PortalConfig portalConfig;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;
  private final UnifiedPermissionValidator unifiedPermissionValidator;

  public ServerNamespaceManagementOpenApiService(AppNamespaceService appNamespaceService,
      ApplicationEventPublisher publisher, NamespaceService namespaceService,
      NamespaceLockService namespaceLockService,
      RoleInitializationService roleInitializationService, PortalConfig portalConfig,
      AdminServiceAPI.NamespaceAPI namespaceAPI,
      UnifiedPermissionValidator unifiedPermissionValidator) {
    this.appNamespaceService = appNamespaceService;
    this.publisher = publisher;
    this.namespaceService = namespaceService;
    this.namespaceLockService = namespaceLockService;
    this.roleInitializationService = roleInitializationService;
    this.portalConfig = portalConfig;
    this.namespaceAPI = namespaceAPI;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
  }

  @Override
  public OpenNamespaceDTO findNamespace(String appId, String env, String clusterName,
      String namespaceName, boolean fillItemDetail, boolean extendInfo) {
    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf(env), clusterName,
        namespaceName, fillItemDetail, extendInfo);
    hideItemsIfNeeded(appId, env, clusterName, namespaceName, namespaceBO);
    return toOpenNamespace(namespaceBO, extendInfo);
  }

  @Override
  public List<OpenNamespaceDTO> findNamespaces(String appId, String env, String clusterName,
      boolean fillItemDetail, boolean extendInfo) {
    List<NamespaceBO> namespaceBOs = namespaceService.findNamespaceBOs(appId, Env.valueOf(env),
        clusterName, fillItemDetail, extendInfo);
    for (NamespaceBO namespaceBO : namespaceBOs) {
      hideItemsIfNeeded(appId, env, clusterName, namespaceBO.getBaseInfo().getNamespaceName(),
          namespaceBO);
    }
    return namespaceBOs.stream().map(namespaceBO -> toOpenNamespace(namespaceBO, extendInfo))
        .collect(Collectors.toList());
  }

  @Override
  public OpenNamespaceDTO findPublicNamespaceForAssociatedNamespace(String env, String appId,
      String clusterName, String namespaceName, boolean extendInfo) {
    NamespaceBO namespaceBO = namespaceService.findPublicNamespaceForAssociatedNamespace(
        Env.valueOf(env), appId, clusterName, namespaceName);
    return toOpenNamespace(namespaceBO, extendInfo);
  }

  @Override
  public OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName,
      String namespaceName) {
    NamespaceDTO namespace =
        namespaceService.loadNamespaceBaseInfo(appId, Env.valueOf(env), clusterName, namespaceName);
    LockInfo lockInfo = namespaceLockService.getNamespaceLockInfo(appId, Env.valueOf(env),
        clusterName, namespaceName);

    OpenNamespaceLockDTO lock = new OpenNamespaceLockDTO();
    lock.setNamespaceName(namespace.getNamespaceName());
    lock.setLockedBy(lockInfo.getLockOwner());
    lock.setIsLocked(
        !com.ctrip.framework.apollo.core.utils.StringUtils.isBlank(lockInfo.getLockOwner()));
    lock.setIsEmergencyPublishAllowed(lockInfo.isEmergencyPublishAllowed());
    return lock;
  }

  @Override
  public OpenAppNamespaceDTO createAppNamespace(String appId, OpenAppNamespaceDTO appNamespaceDTO,
      String operator) {
    AppNamespace appNamespace = OpenApiModelConverters.toAppNamespace(appNamespaceDTO);
    appNamespace.setDataChangeCreatedBy(operator);
    appNamespace.setDataChangeLastModifiedBy(operator);
    AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(appNamespace,
        !Boolean.FALSE.equals(appNamespaceDTO.getAppendNamespacePrefix()), operator);

    if (portalConfig.canAppAdminCreatePrivateNamespace() || createdAppNamespace.isPublic()) {
      namespaceService.assignNamespaceRoleToOperator(appId, appNamespace.getName(), operator);
    }

    publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));
    return OpenApiModelConverters.fromAppNamespace(createdAppNamespace);
  }

  @Override
  public void createNamespaces(List<OpenCreateNamespaceDTO> namespaces, String operator) {
    checkModel(!CollectionUtils.isEmpty(namespaces));
    validateCreateNamespaceRequests(namespaces);

    List<String> failedNamespaces = new ArrayList<>();
    for (OpenCreateNamespaceDTO model : namespaces) {
      String appId = model.getAppId();
      String namespaceName = model.getAppNamespaceName();
      roleInitializationService.initNamespaceRoles(appId, namespaceName, operator);
      roleInitializationService.initNamespaceEnvRoles(appId, namespaceName, operator);

      NamespaceDTO namespace = new NamespaceDTO();
      namespace.setAppId(appId);
      namespace.setClusterName(model.getClusterName());
      namespace.setNamespaceName(namespaceName);
      namespace.setDataChangeCreatedBy(operator);
      namespace.setDataChangeLastModifiedBy(operator);

      try {
        namespaceService.createNamespace(Env.valueOf(model.getEnv()), namespace, operator);
        namespaceService.assignNamespaceRoleToOperator(appId, namespaceName, operator);
      } catch (Exception e) {
        logger.error("create namespace fail.", e);
        Tracer.logError(String.format("create namespace fail. (env=%s namespace=%s)",
            model.getEnv(), namespaceName), e);
        failedNamespaces
            .add(String.format("%s/%s/%s", model.getEnv(), model.getClusterName(), namespaceName));
      }
    }

    if (!failedNamespaces.isEmpty()) {
      throw new BadRequestException("create namespace failed for: %s",
          String.join(", ", failedNamespaces));
    }
  }

  @Override
  public void deleteNamespace(String appId, String env, String clusterName, String namespaceName,
      String operator) {
    namespaceService.deleteNamespace(appId, Env.valueOf(env), clusterName, namespaceName, operator);
  }

  @Override
  public List<OpenNamespaceUsageDTO> findNamespaceUsage(String appId, String env,
      String clusterName, String namespaceName) {
    NamespaceUsage usage = namespaceService.getNamespaceUsageByEnv(appId, namespaceName,
        Env.valueOf(env), clusterName);
    return OpenApiModelConverters.fromNamespaceUsages(Collections.singletonList(usage));
  }

  @Override
  public Map<String, Map<String, Boolean>> getNamespacesReleaseStatus(String appId) {
    return namespaceService.getNamespacesPublishInfo(appId);
  }

  @Override
  public List<String> findMissingNamespaces(String appId, String env, String clusterName) {
    return findMissingNamespaceNames(appId, env, clusterName).stream().sorted()
        .collect(Collectors.toList());
  }

  @Override
  public void createMissingNamespaces(String appId, String env, String clusterName,
      String operator) {
    Set<String> missingNamespaces = findMissingNamespaceNames(appId, env, clusterName);

    for (String missingNamespace : missingNamespaces) {
      namespaceAPI.createMissingAppNamespace(Env.valueOf(env),
          findAppNamespaceDTO(appId, missingNamespace));
    }
  }

  @Override
  public List<OpenAppNamespaceDTO> getAppNamespaces() {
    return appNamespaceService.findPublicAppNamespaces().stream()
        .map(OpenApiModelConverters::fromAppNamespace).collect(Collectors.toList());
  }

  @Override
  public List<OpenAppNamespaceDTO> getAppNamespacesByAppId(String appId) {
    return appNamespaceService.findByAppId(appId).stream()
        .map(OpenApiModelConverters::fromAppNamespace).collect(Collectors.toList());
  }

  @Override
  public OpenAppNamespaceDTO findAppNamespace(String appId, String namespaceName) {
    return OpenApiModelConverters.fromAppNamespace(findAppNamespaceEntity(appId, namespaceName));
  }

  @Override
  public void deleteAppNamespace(String appId, String namespaceName, String operator) {
    AppNamespace appNamespace =
        appNamespaceService.deleteAppNamespace(appId, namespaceName, operator);
    publisher.publishEvent(new AppNamespaceDeletionEvent(appNamespace));
  }

  @Override
  public List<OpenNamespaceUsageDTO> findAppNamespaceUsage(String appId, String namespaceName) {
    return OpenApiModelConverters
        .fromNamespaceUsages(namespaceService.getNamespaceUsageByAppId(appId, namespaceName));
  }

  @Override
  public List<OpenNamespaceDTO> getPublicAppNamespaceInstances(String env,
      String publicNamespaceName, int page, int size) {
    return OpenApiModelConverters.fromNamespaceDTOs(namespaceService
        .getPublicAppNamespaceAllNamespaces(Env.valueOf(env), publicNamespaceName, page, size));
  }

  private OpenNamespaceDTO toOpenNamespace(NamespaceBO namespaceBO, boolean extendInfo) {
    if (namespaceBO == null) {
      return null;
    }
    OpenNamespaceDTO namespaceDTO = OpenApiModelConverters.fromNamespaceBO(namespaceBO);
    if (!extendInfo) {
      namespaceDTO.setExtendInfo(null);
      if (!CollectionUtils.isEmpty(namespaceDTO.getItems())) {
        namespaceDTO.getItems().forEach(item -> item.setExtendInfo(null));
      }
    }
    return namespaceDTO;
  }

  private void hideItemsIfNeeded(String appId, String env, String clusterName, String namespaceName,
      NamespaceBO namespaceBO) {
    if (namespaceBO == null
        || !UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      return;
    }
    if (unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
        namespaceName)) {
      namespaceBO.hideItems();
    }
  }

  private Set<String> findMissingNamespaceNames(String appId, String env, String clusterName) {
    List<AppNamespaceDTO> configDbAppNamespaces =
        namespaceAPI.getAppNamespaces(appId, Env.valueOf(env));
    List<NamespaceDTO> configDbNamespaces =
        namespaceService.findNamespaces(appId, Env.valueOf(env), clusterName);
    List<AppNamespace> portalDbAppNamespaces = appNamespaceService.findByAppId(appId);

    Set<String> configDbAppNamespaceNames =
        configDbAppNamespaces.stream().map(AppNamespaceDTO::getName).collect(Collectors.toSet());
    Set<String> configDbNamespaceNames =
        configDbNamespaces.stream().map(NamespaceDTO::getNamespaceName).collect(Collectors.toSet());

    Set<String> portalDbAllAppNamespaceNames = Sets.newHashSet();
    Set<String> portalDbPrivateAppNamespaceNames = Sets.newHashSet();

    for (AppNamespace appNamespace : portalDbAppNamespaces) {
      portalDbAllAppNamespaceNames.add(appNamespace.getName());
      if (!appNamespace.isPublic()) {
        portalDbPrivateAppNamespaceNames.add(appNamespace.getName());
      }
    }

    Set<String> missingAppNamespaceNames =
        Sets.difference(portalDbAllAppNamespaceNames, configDbAppNamespaceNames);
    Set<String> missingNamespaceNames =
        Sets.difference(portalDbPrivateAppNamespaceNames, configDbNamespaceNames);

    return Sets.union(missingAppNamespaceNames, missingNamespaceNames);
  }

  private AppNamespaceDTO findAppNamespaceDTO(String appId, String namespaceName) {
    return BeanUtils.transform(AppNamespaceDTO.class, findAppNamespaceEntity(appId, namespaceName));
  }

  private AppNamespace findAppNamespaceEntity(String appId, String namespaceName) {
    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);
    if (appNamespace == null) {
      throw BadRequestException.appNamespaceNotExists(appId, namespaceName);
    }
    return appNamespace;
  }

  private void validateCreateNamespaceRequests(List<OpenCreateNamespaceDTO> namespaces) {
    for (OpenCreateNamespaceDTO model : namespaces) {
      checkModel(model != null);
      RequestPrecondition.checkArgumentsNotEmpty(model.getEnv(), model.getAppId(),
          model.getClusterName(), model.getAppNamespaceName());
    }
  }
}
