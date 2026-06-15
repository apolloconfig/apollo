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

import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.openapi.api.InstanceManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.google.common.base.Splitter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController("openapiInstanceController")
public class InstanceController implements InstanceManagementApi {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_INSTANCE_PAGE_SIZE = 20;
  private static final Splitter RELEASE_ID_SPLITTER =
      Splitter.on(',').trimResults().omitEmptyStrings();

  private final InstanceService instanceService;
  private final ReleaseService releaseService;
  private final UnifiedPermissionValidator unifiedPermissionValidator;

  public InstanceController(final InstanceService instanceService,
      final ReleaseService releaseService,
      final UnifiedPermissionValidator unifiedPermissionValidator) {
    this.instanceService = instanceService;
    this.releaseService = releaseService;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
  }

  @Override
  public ResponseEntity<OpenInstancePageDTO> getByNamespace(String env, String appId,
      String clusterName, String namespaceName, Integer page, Integer size, String instanceAppId) {
    int resolvedPage = resolvePage(page);
    int resolvedSize = resolvePageSize(size);
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok(emptyInstancePage(resolvedPage, resolvedSize));
    }
    checkConfigReadAllowed(appId, env, clusterName, namespaceName);
    return ResponseEntity.ok(
        OpenApiModelConverters.fromInstancePageDTO(instanceService.getByNamespace(Env.valueOf(env),
            appId, clusterName, namespaceName, instanceAppId, resolvedPage, resolvedSize)));
  }

  @Override
  public ResponseEntity<OpenInstancePageDTO> getByRelease(String env, Long releaseId, Integer page,
      Integer size) {
    ReleaseDTO release = findReleaseOrThrow(Env.valueOf(env), releaseId);
    checkReleaseReadAllowed(env, release);
    return ResponseEntity.ok(OpenApiModelConverters.fromInstancePageDTO(instanceService
        .getByRelease(Env.valueOf(env), releaseId, resolvePage(page), resolvePageSize(size))));
  }

  @Override
  public ResponseEntity<List<OpenInstanceDTO>> getByReleasesAndNamespaceNotIn(String env,
      String appId, String clusterName, String namespaceName, String releaseIds) {
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    checkConfigReadAllowed(appId, env, clusterName, namespaceName);
    if (releaseIds == null || releaseIds.trim().isEmpty()) {
      throw new BadRequestException("releaseIds should not be empty");
    }

    Set<Long> releaseIdSet;
    try {
      releaseIdSet = RELEASE_ID_SPLITTER.splitToStream(releaseIds).map(Long::parseLong)
          .collect(Collectors.toSet());
    } catch (NumberFormatException ex) {
      throw new BadRequestException("releaseIds should be comma separated numbers");
    }
    if (releaseIdSet.isEmpty()) {
      throw new BadRequestException("releaseIds should not be empty");
    }
    return ResponseEntity.ok(OpenApiModelConverters.fromInstanceDTOs(instanceService
        .getByReleasesNotIn(Env.valueOf(env), appId, clusterName, namespaceName, releaseIdSet)));
  }

  @Override
  public ResponseEntity<Integer> getInstanceCountByNamespace(String env, String appId,
      String clusterName, String namespaceName) {
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok(0);
    }
    checkConfigReadAllowed(appId, env, clusterName, namespaceName);
    return ResponseEntity.ok(instanceService.getInstanceCountByNamespace(appId, Env.valueOf(env),
        clusterName, namespaceName));
  }

  private boolean shouldHideConfigToPortalUser(String appId, String env, String clusterName,
      String namespaceName) {
    return UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName);
  }

  private void checkConfigReadAllowed(String appId, String env, String clusterName,
      String namespaceName) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER_TOKEN.equals(authType) && unifiedPermissionValidator
        .shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      throw new AccessDeniedException("Access is denied");
    }
    if (UserIdentityConstants.CONSUMER.equals(authType) && !unifiedPermissionValidator
        .hasReleaseNamespacePermission(appId, env, clusterName, namespaceName)) {
      throw new AccessDeniedException("Access is denied");
    }
  }

  private ReleaseDTO findReleaseOrThrow(Env env, long releaseId) {
    ReleaseDTO release = releaseService.findReleaseById(env, releaseId);
    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    return release;
  }

  private void checkReleaseReadAllowed(String env, ReleaseDTO release) {
    if ((UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
        || UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType()))
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(release.getAppId(), env,
            release.getClusterName(), release.getNamespaceName())) {
      throw new AccessDeniedException("Access is denied");
    }
    checkConfigReadAllowed(release.getAppId(), env, release.getClusterName(),
        release.getNamespaceName());
  }

  private OpenInstancePageDTO emptyInstancePage(int page, int size) {
    return OpenApiModelConverters.fromInstancePageDTO(
        new PageDTO<>(Collections.<InstanceDTO>emptyList(), PageRequest.of(page, size), 0L));
  }

  private int resolvePage(Integer page) {
    return page == null ? DEFAULT_PAGE : page;
  }

  private int resolvePageSize(Integer size) {
    return size == null ? DEFAULT_INSTANCE_PAGE_SIZE : size;
  }
}
