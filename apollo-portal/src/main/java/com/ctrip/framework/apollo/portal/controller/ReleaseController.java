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

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseBO;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.listener.ConfigPublishEvent;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Validated
@RestController
public class ReleaseController {

  private final ReleaseService releaseService;
  private final ApplicationEventPublisher publisher;
  private final PortalConfig portalConfig;
  private final UserPermissionValidator userPermissionValidator;
  private final UserInfoHolder userInfoHolder;

  public ReleaseController(
      final ReleaseService releaseService,
      final ApplicationEventPublisher publisher,
      final PortalConfig portalConfig,
      final UserPermissionValidator userPermissionValidator,
      final UserInfoHolder userInfoHolder) {
    this.releaseService = releaseService;
    this.publisher = publisher;
    this.portalConfig = portalConfig;
    this.userPermissionValidator = userPermissionValidator;
    this.userInfoHolder = userInfoHolder;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/releases")
  public ReleaseDTO createRelease(@PathVariable String appId,
                                  @PathVariable String env, @PathVariable String clusterName,
                                  @PathVariable String namespaceName, @RequestBody NamespaceReleaseModel model) {
    model.setAppId(appId);
    model.setEnv(env);
    model.setClusterName(clusterName);
    model.setNamespaceName(namespaceName);

    if (model.isEmergencyPublish() && !portalConfig.isEmergencyPublishAllowed(Env.valueOf(env))) {
      throw new BadRequestException("Env: %s is not supported emergency publish now", env);
    }

    ReleaseDTO createdRelease = releaseService.publish(model);

    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(appId)
        .withCluster(clusterName)
        .withNamespace(namespaceName)
        .withReleaseId(createdRelease.getId())
        .setNormalPublishEvent(true)
        .setEnv(Env.valueOf(env));

    publisher.publishEvent(event);

    return createdRelease;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/releases")
  public ReleaseDTO createGrayRelease(@PathVariable String appId,
                                      @PathVariable String env, @PathVariable String clusterName,
                                      @PathVariable String namespaceName, @PathVariable String branchName,
                                      @RequestBody NamespaceReleaseModel model) {
    model.setAppId(appId);
    model.setEnv(env);
    model.setClusterName(branchName);
    model.setNamespaceName(namespaceName);

    if (model.isEmergencyPublish() && !portalConfig.isEmergencyPublishAllowed(Env.valueOf(env))) {
      throw new BadRequestException("Env: %s is not supported emergency publish now", env);
    }

    ReleaseDTO createdRelease = releaseService.publish(model);

    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(appId)
        .withCluster(clusterName)
        .withNamespace(namespaceName)
        .withReleaseId(createdRelease.getId())
        .setGrayPublishEvent(true)
        .setEnv(Env.valueOf(env));

    publisher.publishEvent(event);

    return createdRelease;
  }

  @GetMapping("/envs/{env}/releases/{releaseId}")
  public ReleaseDTO get(@PathVariable String env,
                        @PathVariable long releaseId) {
    ReleaseDTO release = releaseService.findReleaseById(Env.valueOf(env), releaseId);

    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    return release;
  }

  @GetMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/releases/all")
  public List<ReleaseBO> findAllReleases(@PathVariable String appId,
                                         @PathVariable String env,
                                         @PathVariable String clusterName,
                                         @PathVariable String namespaceName,
                                         @Valid @PositiveOrZero(message = "page should be positive or 0") @RequestParam(defaultValue = "0") int page,
                                         @Valid @Positive(message = "size should be positive number") @RequestParam(defaultValue = "5") int size) {
    if (userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return Collections.emptyList();
    }

    return releaseService.findAllReleases(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
  }

  @GetMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/releases/active")
  public List<ReleaseDTO> findActiveReleases(@PathVariable String appId,
                                             @PathVariable String env,
                                             @PathVariable String clusterName,
                                             @PathVariable String namespaceName,
                                             @Valid @PositiveOrZero(message = "page should be positive or 0") @RequestParam(defaultValue = "0") int page,
                                             @Valid @Positive(message = "size should be positive number") @RequestParam(defaultValue = "5") int size) {

    if (userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return Collections.emptyList();
    }

    return releaseService.findActiveReleases(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
  }

  @GetMapping(value = "/envs/{env}/releases/compare")
  public ReleaseCompareResult compareRelease(@PathVariable String env,
                                             @RequestParam long baseReleaseId,
                                             @RequestParam long toCompareReleaseId) {

    return releaseService.compare(Env.valueOf(env), baseReleaseId, toCompareReleaseId);
  }


  @PutMapping(path = "/envs/{env}/releases/{releaseId}/rollback")
  public void rollback(@PathVariable String env,
                       @PathVariable long releaseId,
                       @RequestParam(defaultValue = "-1") long toReleaseId) {
    ReleaseDTO release = releaseService.findReleaseById(Env.valueOf(env), releaseId);

    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }

    if (!userPermissionValidator.hasReleaseNamespacePermission(release.getAppId(), env, release.getClusterName(), release.getNamespaceName())) {
      throw new AccessDeniedException("Access is denied");
    }

    if (toReleaseId > -1) {
      releaseService.rollbackTo(Env.valueOf(env), releaseId, toReleaseId, userInfoHolder.getUser().getUserId());
    } else {
      releaseService.rollback(Env.valueOf(env), releaseId, userInfoHolder.getUser().getUserId());
    }

    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(release.getAppId())
        .withCluster(release.getClusterName())
        .withNamespace(release.getNamespaceName())
        .withPreviousReleaseId(releaseId)
        .setRollbackEvent(true)
        .setEnv(Env.valueOf(env));

    publisher.publishEvent(event);
  }
}
