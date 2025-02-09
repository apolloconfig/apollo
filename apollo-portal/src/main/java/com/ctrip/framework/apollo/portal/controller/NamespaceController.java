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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.http.MultiResponseEntity;
import com.ctrip.framework.apollo.common.http.RichResponseEntity;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceUsage;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceCreationModel;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceDeletionEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ctrip.framework.apollo.common.utils.RequestPrecondition.checkModel;

@RestController
public class NamespaceController {

  private static final Logger logger = LoggerFactory.getLogger(NamespaceController.class);

  private final ApplicationEventPublisher publisher;
  private final UserInfoHolder userInfoHolder;
  private final NamespaceService namespaceService;
  private final AppNamespaceService appNamespaceService;
  private final RoleInitializationService roleInitializationService;
  private final PortalConfig portalConfig;
  private final UserPermissionValidator userPermissionValidator;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;

  public NamespaceController(
      final ApplicationEventPublisher publisher,
      final UserInfoHolder userInfoHolder,
      final NamespaceService namespaceService,
      final AppNamespaceService appNamespaceService,
      final RoleInitializationService roleInitializationService,
      final PortalConfig portalConfig,
      final UserPermissionValidator userPermissionValidator,
      final AdminServiceAPI.NamespaceAPI namespaceAPI) {
    this.publisher = publisher;
    this.userInfoHolder = userInfoHolder;
    this.namespaceService = namespaceService;
    this.appNamespaceService = appNamespaceService;
    this.roleInitializationService = roleInitializationService;
    this.portalConfig = portalConfig;
    this.userPermissionValidator = userPermissionValidator;
    this.namespaceAPI = namespaceAPI;
  }


  @GetMapping("/appnamespaces/public")
  public List<AppNamespace> findPublicAppNamespaces() {
    return appNamespaceService.findPublicAppNamespaces();
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces")
  public List<NamespaceBO> findNamespaces(@PathVariable String appId, @PathVariable String env,
                                          @PathVariable String clusterName) {

    List<NamespaceBO> namespaceBOs = namespaceService.findNamespaceBOs(appId, Env.valueOf(env), clusterName);

    for (NamespaceBO namespaceBO : namespaceBOs) {
      if (userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceBO.getBaseInfo().getNamespaceName())) {
        namespaceBO.hideItems();
      }
    }

    return namespaceBOs;
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName:.+}")
  public NamespaceBO findNamespace(@PathVariable String appId, @PathVariable String env,
                                   @PathVariable String clusterName, @PathVariable String namespaceName) {

    NamespaceBO namespaceBO = namespaceService.loadNamespaceBO(appId, Env.valueOf(env), clusterName, namespaceName);

    if (namespaceBO != null && userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      namespaceBO.hideItems();
    }

    return namespaceBO;
  }

  @GetMapping("/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/associated-public-namespace")
  public NamespaceBO findPublicNamespaceForAssociatedNamespace(@PathVariable String env,
                                                               @PathVariable String appId,
                                                               @PathVariable String namespaceName,
                                                               @PathVariable String clusterName) {

    return namespaceService.findPublicNamespaceForAssociatedNamespace(Env.valueOf(env), appId, clusterName, namespaceName);
  }

  @PreAuthorize(value = "@userPermissionValidator.hasCreateNamespacePermission(#appId)")
  @PostMapping("/apps/{appId}/namespaces")
  @ApolloAuditLog(type = OpType.CREATE, name = "Namespace.create")
  public ResponseEntity<Void> createNamespace(@PathVariable String appId,
                                              @RequestBody List<NamespaceCreationModel> models) {

    checkModel(!CollectionUtils.isEmpty(models));
    String operator = userInfoHolder.getUser().getUserId();

    for (NamespaceCreationModel model : models) {
      String namespaceName = model.getNamespace().getNamespaceName();
      roleInitializationService.initNamespaceRoles(appId, namespaceName, operator);
      roleInitializationService.initNamespaceEnvRoles(appId, namespaceName, operator);
      NamespaceDTO namespace = model.getNamespace();
      RequestPrecondition.checkArgumentsNotEmpty(model.getEnv(), namespace.getAppId(),
                                                 namespace.getClusterName(), namespace.getNamespaceName());

      try {
        namespaceService.createNamespace(Env.valueOf(model.getEnv()), namespace);
      } catch (Exception e) {
        logger.error("create namespace fail.", e);
        Tracer.logError(
                String.format("create namespace fail. (env=%s namespace=%s)", model.getEnv(),
                        namespace.getNamespaceName()), e);
      }
      namespaceService.assignNamespaceRoleToOperator(appId, namespaceName,userInfoHolder.getUser().getUserId());
    }

    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@userPermissionValidator.hasDeleteNamespacePermission(#appId)")
  @DeleteMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/linked-namespaces/{namespaceName:.+}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Namespace.deleteLinkedNamespace")
  public ResponseEntity<Void> deleteLinkedNamespace(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName) {

    namespaceService.deleteNamespace(appId, Env.valueOf(env), clusterName, namespaceName);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/linked-namespaces/{namespaceName}/usage")
  public List<NamespaceUsage> findLinkedNamespaceUsage(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName) {
    NamespaceUsage usage = namespaceService.getNamespaceUsageByEnv(appId, namespaceName, Env.valueOf(env), clusterName);
    return Lists.newArrayList(usage);
  }

  @GetMapping("/apps/{appId}/namespaces/{namespaceName}/usage")
  public List<NamespaceUsage> findNamespaceUsage(@PathVariable String appId, @PathVariable String namespaceName) {
    return namespaceService.getNamespaceUsageByAppId(appId, namespaceName);
  }

  @PreAuthorize(value = "@userPermissionValidator.hasDeleteNamespacePermission(#appId)")
  @DeleteMapping("/apps/{appId}/appnamespaces/{namespaceName:.+}")
  @ApolloAuditLog(type = OpType.DELETE, name = "AppNamespace.delete")
  public ResponseEntity<Void> deleteAppNamespace(@PathVariable String appId, @PathVariable String namespaceName) {

    AppNamespace appNamespace = appNamespaceService.deleteAppNamespace(appId, namespaceName);

    publisher.publishEvent(new AppNamespaceDeletionEvent(appNamespace));

    return ResponseEntity.ok().build();
  }

  @GetMapping("/apps/{appId}/appnamespaces/{namespaceName:.+}")
  public AppNamespaceDTO findAppNamespace(@PathVariable String appId, @PathVariable String namespaceName) {
    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);

    if (appNamespace == null) {
      throw BadRequestException.appNamespaceNotExists(appId, namespaceName);
    }

    return BeanUtils.transform(AppNamespaceDTO.class, appNamespace);
  }

  @PreAuthorize(value = "@userPermissionValidator.hasCreateAppNamespacePermission(#appId, #appNamespace)")
  @PostMapping("/apps/{appId}/appnamespaces")
  @ApolloAuditLog(type = OpType.CREATE, name = "AppNamespace.create")
  public AppNamespace createAppNamespace(@PathVariable String appId,
      @RequestParam(defaultValue = "true") boolean appendNamespacePrefix,
      @Valid @RequestBody AppNamespace appNamespace) {
    if (!InputValidator.isValidAppNamespace(appNamespace.getName())) {
      throw BadRequestException.invalidNamespaceFormat(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE + " & "
          + InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE);
    }

    AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(appNamespace, appendNamespacePrefix);

    if (portalConfig.canAppAdminCreatePrivateNamespace() || createdAppNamespace.isPublic()) {
      namespaceService.assignNamespaceRoleToOperator(appId, appNamespace.getName(),
          userInfoHolder.getUser().getUserId());
    }

    publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));

    return createdAppNamespace;
  }

  /**
   * env -> cluster -> cluster has not published namespace?
   * Example:
   * dev ->
   *  default -> true   (default cluster has not published namespace)
   *  customCluster -> false (customCluster cluster's all namespaces had published)
   */
  @GetMapping("/apps/{appId}/namespaces/publish_info")
  public Map<String, Map<String, Boolean>> getNamespacesPublishInfo(@PathVariable String appId) {
    return namespaceService.getNamespacesPublishInfo(appId);
  }

  @GetMapping("/envs/{env}/appnamespaces/{publicNamespaceName}/namespaces")
  public List<NamespaceDTO> getPublicAppNamespaceAllNamespaces(@PathVariable String env,
                                                               @PathVariable String publicNamespaceName,
                                                               @RequestParam(name = "page", defaultValue = "0") int page,
                                                               @RequestParam(name = "size", defaultValue = "10") int size) {

    return namespaceService.getPublicAppNamespaceAllNamespaces(Env.valueOf(env), publicNamespaceName, page, size);

  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/missing-namespaces")
  public MultiResponseEntity<String> findMissingNamespaces(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName) {

    MultiResponseEntity<String> response = MultiResponseEntity.ok();

    Set<String> missingNamespaces = findMissingNamespaceNames(appId, env, clusterName);

    for (String missingNamespace : missingNamespaces) {
      response.addResponseEntity(RichResponseEntity.ok(missingNamespace));
    }

    return response;
  }

  @PostMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/missing-namespaces")
  @ApolloAuditLog(type = OpType.CREATE, name = "Namespace.createMissingNamespaces")
  public ResponseEntity<Void> createMissingNamespaces(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName) {

    Set<String> missingNamespaces = findMissingNamespaceNames(appId, env, clusterName);

    for (String missingNamespace : missingNamespaces) {
      namespaceAPI.createMissingAppNamespace(Env.valueOf(env), findAppNamespace(appId, missingNamespace));
    }

    return ResponseEntity.ok().build();
  }

  private Set<String> findMissingNamespaceNames(String appId, String env, String clusterName) {
    List<AppNamespaceDTO> configDbAppNamespaces = namespaceAPI.getAppNamespaces(appId, Env.valueOf(env));
    List<NamespaceDTO> configDbNamespaces = namespaceService.findNamespaces(appId, Env.valueOf(env), clusterName);
    List<AppNamespace> portalDbAppNamespaces = appNamespaceService.findByAppId(appId);

    Set<String> configDbAppNamespaceNames = configDbAppNamespaces.stream().map(AppNamespaceDTO::getName)
            .collect(Collectors.toSet());
    Set<String> configDbNamespaceNames = configDbNamespaces.stream().map(NamespaceDTO::getNamespaceName)
        .collect(Collectors.toSet());

    Set<String> portalDbAllAppNamespaceNames = Sets.newHashSet();
    Set<String> portalDbPrivateAppNamespaceNames = Sets.newHashSet();

    for (AppNamespace appNamespace : portalDbAppNamespaces) {
      portalDbAllAppNamespaceNames.add(appNamespace.getName());
      if (!appNamespace.isPublic()) {
        portalDbPrivateAppNamespaceNames.add(appNamespace.getName());
      }
    }

    // AppNamespaces should be the same
    Set<String> missingAppNamespaceNames = Sets.difference(portalDbAllAppNamespaceNames, configDbAppNamespaceNames);
    // Private namespaces should all exist
    Set<String> missingNamespaceNames = Sets.difference(portalDbPrivateAppNamespaceNames, configDbNamespaceNames);

    return Sets.union(missingAppNamespaceNames, missingNamespaceNames);
  }
}
