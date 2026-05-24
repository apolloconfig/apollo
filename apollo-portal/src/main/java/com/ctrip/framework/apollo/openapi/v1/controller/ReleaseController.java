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

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.ReleaseManagementApi;
import com.ctrip.framework.apollo.openapi.model.NamespaceGrayDelReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDiffDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceGrayDelReleaseModel;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.ConfigPublishEvent;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController("openapiReleaseController")
public class ReleaseController implements ReleaseManagementApi {

  private final ReleaseService releaseService;
  private final UserService userService;
  private final NamespaceBranchService namespaceBranchService;
  private final UnifiedPermissionValidator unifiedPermissionValidator;
  private final PortalConfig portalConfig;
  private final UserInfoHolder userInfoHolder;
  private final ApplicationEventPublisher publisher;

  public ReleaseController(final ReleaseService releaseService, final UserService userService,
      final NamespaceBranchService namespaceBranchService,
      final UnifiedPermissionValidator unifiedPermissionValidator, final PortalConfig portalConfig,
      final UserInfoHolder userInfoHolder, final ApplicationEventPublisher publisher) {
    this.releaseService = releaseService;
    this.userService = userService;
    this.namespaceBranchService = namespaceBranchService;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
    this.portalConfig = portalConfig;
    this.userInfoHolder = userInfoHolder;
    this.publisher = publisher;
  }

  @Override
  public ResponseEntity<OpenReleaseDiffDTO> compareRelease(String env, Long baseReleaseId,
      Long toCompareReleaseId) {
    Env targetEnv = Env.valueOf(env);
    ReleaseDTO baseRelease = findReleaseOrThrow(targetEnv, baseReleaseId);
    ReleaseDTO toCompareRelease = findReleaseOrThrow(targetEnv, toCompareReleaseId);
    checkReleaseReadAllowed(env, baseRelease);
    checkReleaseReadAllowed(env, toCompareRelease);
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseCompareResult(
        releaseService.compare(targetEnv, baseReleaseId, toCompareReleaseId)));
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<OpenReleaseDTO> createGrayDelRelease(String appId, String env,
      String clusterName, String namespaceName, String branchName,
      NamespaceGrayDelReleaseDTO namespaceGrayDelReleaseDTO, String operator) {
    RequestPrecondition.checkArguments(namespaceGrayDelReleaseDTO != null,
        "release payload can not be empty");
    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(namespaceGrayDelReleaseDTO.getReleaseTitle()),
        "Params(releaseTitle) can not be empty");
    RequestPrecondition.checkArguments(namespaceGrayDelReleaseDTO.getGrayDelKeys() != null,
        "Params(grayDelKeys) can not be null");

    String resolvedOperator = resolveOperator(operator, namespaceGrayDelReleaseDTO.getReleasedBy());
    boolean emergencyPublish =
        Boolean.TRUE.equals(namespaceGrayDelReleaseDTO.getIsEmergencyPublish());
    checkEmergencyPublishAllowedForUser(env, emergencyPublish);

    NamespaceGrayDelReleaseModel releaseModel = new NamespaceGrayDelReleaseModel();
    releaseModel.setAppId(appId);
    releaseModel.setEnv(env);
    releaseModel.setClusterName(branchName);
    releaseModel.setNamespaceName(namespaceName);
    releaseModel.setReleaseTitle(namespaceGrayDelReleaseDTO.getReleaseTitle());
    releaseModel.setReleaseComment(namespaceGrayDelReleaseDTO.getReleaseComment());
    releaseModel.setReleasedBy(resolvedOperator);
    releaseModel.setEmergencyPublish(emergencyPublish);
    releaseModel.setGrayDelKeys(new HashSet<>(namespaceGrayDelReleaseDTO.getGrayDelKeys()));

    ReleaseDTO createdRelease = releaseService.publish(releaseModel, resolvedOperator);
    publishEvent(appId, clusterName, namespaceName, createdRelease.getId(), Env.valueOf(env),
        PublishEventType.GRAY);
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTO(createdRelease));
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<OpenReleaseDTO> createGrayRelease(String appId, String env,
      String clusterName, String namespaceName, String branchName,
      NamespaceReleaseDTO namespaceReleaseDTO, String operator) {
    NamespaceReleaseModel releaseModel = toReleaseModel(appId, env, branchName, namespaceName,
        namespaceReleaseDTO, resolveOperator(operator, payloadReleasedBy(namespaceReleaseDTO)));
    checkEmergencyPublishAllowedForUser(env, releaseModel.isEmergencyPublish());

    ReleaseDTO createdRelease = releaseService.publish(releaseModel);
    publishEvent(appId, clusterName, namespaceName, createdRelease.getId(), Env.valueOf(env),
        PublishEventType.GRAY);
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTO(createdRelease));
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<OpenReleaseDTO> createRelease(String appId, String env, String clusterName,
      String namespaceName, NamespaceReleaseDTO namespaceReleaseDTO, String operator) {
    NamespaceReleaseModel releaseModel = toReleaseModel(appId, env, clusterName, namespaceName,
        namespaceReleaseDTO, resolveOperator(operator, payloadReleasedBy(namespaceReleaseDTO)));
    checkEmergencyPublishAllowedForUser(env, releaseModel.isEmergencyPublish());

    ReleaseDTO createdRelease = releaseService.publish(releaseModel);
    publishEvent(appId, clusterName, namespaceName, createdRelease.getId(), Env.valueOf(env),
        PublishEventType.NORMAL);
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTO(createdRelease));
  }

  @Override
  public ResponseEntity<List<OpenReleaseDTO>> findActiveReleases(String appId, String env,
      String clusterName, String namespaceName, Integer page, Integer size) {
    if (shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTOs(releaseService
        .findActiveReleases(appId, Env.valueOf(env), clusterName, namespaceName, page, size)));
  }

  @Override
  public ResponseEntity<OpenReleaseDTO> getReleaseById(String env, Long releaseId) {
    ReleaseDTO release = findReleaseOrThrow(Env.valueOf(env), releaseId);
    checkReleaseReadAllowed(env, release);
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTO(release));
  }

  @Override
  public ResponseEntity<OpenReleaseDTO> loadLatestActiveRelease(String appId, String env,
      String clusterName, String namespaceName) {
    if (shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok().build();
    }
    ReleaseDTO release =
        releaseService.loadLatestRelease(appId, Env.valueOf(env), clusterName, namespaceName);
    if (release == null) {
      throw NotFoundException
          .releaseNotFound(String.format("%s+%s+%s+%s", appId, env, clusterName, namespaceName));
    }
    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTO(release));
  }

  @Override
  public ResponseEntity<Void> rollback(String env, Long releaseId, String operator,
      Long toReleaseId) {
    ReleaseDTO release = releaseService.findReleaseById(Env.valueOf(env), releaseId);
    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    String resolvedOperator = resolveOperator(operator, null);

    if (!unifiedPermissionValidator.hasReleaseNamespacePermission(release.getAppId(), env,
        release.getClusterName(), release.getNamespaceName())) {
      throw new AccessDeniedException("Access is denied");
    }

    if (toReleaseId != null && toReleaseId > -1) {
      releaseService.rollbackTo(Env.valueOf(env), releaseId, toReleaseId, resolvedOperator);
    } else {
      releaseService.rollback(Env.valueOf(env), releaseId, resolvedOperator);
    }

    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(release.getAppId()).withCluster(release.getClusterName())
        .withNamespace(release.getNamespaceName()).withPreviousReleaseId(releaseId)
        .setRollbackEvent(true).setEnv(Env.valueOf(env));
    publisher.publishEvent(event);
    return ResponseEntity.ok().build();
  }

  private NamespaceReleaseModel toReleaseModel(String appId, String env, String clusterName,
      String namespaceName, NamespaceReleaseDTO releaseDTO, String releasedBy) {
    RequestPrecondition.checkArguments(releaseDTO != null, "release payload can not be empty");
    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(releaseDTO.getReleaseTitle()),
        "Params(releaseTitle) can not be empty");

    NamespaceReleaseModel releaseModel = new NamespaceReleaseModel();
    releaseModel.setAppId(appId);
    releaseModel.setEnv(env);
    releaseModel.setClusterName(clusterName);
    releaseModel.setNamespaceName(namespaceName);
    releaseModel.setReleaseTitle(releaseDTO.getReleaseTitle());
    releaseModel.setReleaseComment(releaseDTO.getReleaseComment());
    releaseModel.setReleasedBy(releasedBy);
    releaseModel.setEmergencyPublish(Boolean.TRUE.equals(releaseDTO.getIsEmergencyPublish()));
    return releaseModel;
  }

  private String payloadReleasedBy(NamespaceReleaseDTO releaseDTO) {
    return releaseDTO == null ? null : releaseDTO.getReleasedBy();
  }

  private void checkEmergencyPublishAllowedForUser(String env, boolean emergencyPublish) {
    if (emergencyPublish
        && UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
        && !portalConfig.isEmergencyPublishAllowed(Env.valueOf(env))) {
      throw new BadRequestException("Env: %s is not supported emergency publish now", env);
    }
  }

  private String resolveOperator(String queryOperator, String payloadOperator) {
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      UserInfo loginUser = userInfoHolder.getUser();
      if (loginUser == null || StringUtils.isBlank(loginUser.getUserId())) {
        throw new BadRequestException("Current user not found");
      }
      return loginUser.getUserId();
    }

    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      String operator = StringUtils.isBlank(queryOperator) ? payloadOperator : queryOperator;
      RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(operator),
          "operator should not be null or empty");
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      return operator;
    }

    throw new BadRequestException("Unsupported auth type: %s",
        UserIdentityContextHolder.getAuthType());
  }

  private boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
    return UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName);
  }

  private ReleaseDTO findReleaseOrThrow(Env env, long releaseId) {
    ReleaseDTO release = releaseService.findReleaseById(env, releaseId);
    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    return release;
  }

  private void checkReleaseReadAllowed(String env, ReleaseDTO release) {
    if (shouldHideConfigToCurrentUser(release.getAppId(), env, release.getClusterName(),
        release.getNamespaceName())) {
      throw new AccessDeniedException("Access is denied");
    }
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())
        && !unifiedPermissionValidator.hasReleaseNamespacePermission(release.getAppId(), env,
            release.getClusterName(), release.getNamespaceName())) {
      throw new AccessDeniedException("Access is denied");
    }
  }

  private void publishEvent(String appId, String clusterName, String namespaceName, long releaseId,
      Env env, PublishEventType eventType) {
    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(appId).withCluster(clusterName).withNamespace(namespaceName)
        .withReleaseId(releaseId).setEnv(env);
    if (eventType == PublishEventType.GRAY) {
      event.setGrayPublishEvent(true);
    } else {
      event.setNormalPublishEvent(true);
    }
    publisher.publishEvent(event);
  }

  private enum PublishEventType {
    NORMAL, GRAY
  }
}
