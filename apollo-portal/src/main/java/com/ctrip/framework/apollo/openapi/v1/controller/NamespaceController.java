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
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.AppNamespaceManagementApi;
import com.ctrip.framework.apollo.openapi.api.NamespaceLockManagementApi;
import com.ctrip.framework.apollo.openapi.api.NamespaceManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenCreateNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceUsageDTO;
import com.ctrip.framework.apollo.openapi.server.service.NamespaceOpenApiService;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

@RestController("openapiNamespaceController")
public class NamespaceController
    implements NamespaceManagementApi, AppNamespaceManagementApi, NamespaceLockManagementApi {

  private final UserService userService;
  private final UserInfoHolder userInfoHolder;
  private final UnifiedPermissionValidator unifiedPermissionValidator;
  private final NamespaceOpenApiService namespaceOpenApiService;

  public NamespaceController(final UserService userService, final UserInfoHolder userInfoHolder,
      final UnifiedPermissionValidator unifiedPermissionValidator,
      NamespaceOpenApiService namespaceOpenApiService) {
    this.userService = userService;
    this.userInfoHolder = userInfoHolder;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
    this.namespaceOpenApiService = namespaceOpenApiService;
  }

  @Override
  public Optional<NativeWebRequest> getRequest() {
    return Optional.empty();
  }

  @Override
  public ResponseEntity<List<OpenAppNamespaceDTO>> getAppNamespaces() {
    return ResponseEntity
        .ok(filterReadableAppNamespaces(namespaceOpenApiService.getAppNamespaces()));
  }

  @Override
  public ResponseEntity<List<OpenAppNamespaceDTO>> getAppNamespacesByAppId(String appId) {
    requireReadApplicationPermissionForUserToken(appId);
    if (!unifiedPermissionValidator.hasReadApplicationPermission(appId)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    return ResponseEntity.ok(namespaceOpenApiService.getAppNamespacesByAppId(appId));
  }

  @Override
  public ResponseEntity<OpenAppNamespaceDTO> findAppNamespace(String appId, String namespaceName,
      Boolean extendInfo) {
    requireReadApplicationPermissionForUserToken(appId);
    if (!unifiedPermissionValidator.hasReadApplicationPermission(appId)) {
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.ok(namespaceOpenApiService.findAppNamespace(appId, namespaceName));
  }

  @Override
  public ResponseEntity<List<OpenNamespaceUsageDTO>> findAppNamespaceUsage(String appId,
      String namespaceName) {
    requireReadApplicationPermissionForUserToken(appId);
    if (!unifiedPermissionValidator.hasReadApplicationPermission(appId)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    return ResponseEntity.ok(namespaceOpenApiService.findAppNamespaceUsage(appId, namespaceName));
  }

  @PreAuthorize(value = "@openapiNamespaceController.canCreateAppNamespace(#appId, #appNamespace)")
  @ApolloAuditLog(type = OpType.CREATE, name = "AppNamespace.create")
  @Override
  public ResponseEntity<OpenAppNamespaceDTO> createAppNamespace(String appId,
      OpenAppNamespaceDTO appNamespace, String operator) {
    validateCreateAppNamespaceRequest(appId, appNamespace);
    String resolvedOperator = resolveOperator(operator, appNamespace.getDataChangeCreatedBy());
    appNamespace.setDataChangeCreatedBy(resolvedOperator);
    appNamespace.setDataChangeLastModifiedBy(resolvedOperator);
    return ResponseEntity
        .ok(namespaceOpenApiService.createAppNamespace(appId, appNamespace, resolvedOperator));
  }

  @PreAuthorize(value = "@unifiedPermissionValidator.hasDeleteNamespacePermission(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "AppNamespace.delete")
  @Override
  public ResponseEntity<Void> deleteAppNamespace(String appId, String namespaceName,
      String operator) {
    namespaceOpenApiService.deleteAppNamespace(appId, namespaceName,
        resolveOperator(operator, null));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<OpenNamespaceDTO>> getPublicAppNamespaceInstances(String env,
      String publicNamespaceName, Integer page, Integer size) {
    int resolvedPage = page == null ? 0 : page;
    int resolvedSize = size == null ? 10 : size;
    return ResponseEntity.ok(filterReadableNamespaces(null, env, null, namespaceOpenApiService
        .getPublicAppNamespaceInstances(env, publicNamespaceName, resolvedPage, resolvedSize)));
  }

  @Override
  public ResponseEntity<OpenNamespaceDTO> findNamespace(String appId, String env,
      String clusterName, String namespaceName, Boolean fillItemDetail, Boolean extendInfo) {
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok().build();
    }
    requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
    return ResponseEntity.ok(namespaceOpenApiService.findNamespace(appId, env, clusterName,
        namespaceName, Boolean.TRUE.equals(fillItemDetail), Boolean.TRUE.equals(extendInfo)));
  }

  @Override
  public ResponseEntity<List<OpenNamespaceDTO>> findNamespaces(String appId, String env,
      String clusterName, Boolean fillItemDetail, Boolean extendInfo) {
    return ResponseEntity.ok(filterReadableNamespaces(appId, env, clusterName,
        namespaceOpenApiService.findNamespaces(appId, env, clusterName,
            Boolean.TRUE.equals(fillItemDetail), Boolean.TRUE.equals(extendInfo))));
  }

  @Override
  public ResponseEntity<OpenNamespaceDTO> findPublicNamespaceForAssociatedNamespace(String env,
      String appId, String clusterName, String namespaceName, Boolean extendInfo) {
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok().build();
    }
    requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
    return ResponseEntity.ok(namespaceOpenApiService.findPublicNamespaceForAssociatedNamespace(env,
        appId, clusterName, namespaceName, Boolean.TRUE.equals(extendInfo)));
  }

  @Override
  public ResponseEntity<OpenNamespaceLockDTO> getNamespaceLock(String appId, String env,
      String clusterName, String namespaceName) {
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok().build();
    }
    requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
    return ResponseEntity
        .ok(namespaceOpenApiService.getNamespaceLock(appId, env, clusterName, namespaceName));
  }

  @PreAuthorize(value = "@openapiNamespaceController.canCreateNamespaces(#openCreateNamespaceDTO)")
  @ApolloAuditLog(type = OpType.CREATE, name = "Namespace.create")
  @Override
  public ResponseEntity<Void> createNamespaces(List<OpenCreateNamespaceDTO> openCreateNamespaceDTO,
      String operator) {
    requireCreateNamespacesPermissionForUserToken(openCreateNamespaceDTO);
    String resolvedOperator = resolveOperator(operator, null);
    namespaceOpenApiService.createNamespaces(openCreateNamespaceDTO, resolvedOperator);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(value = "@unifiedPermissionValidator.hasDeleteNamespacePermission(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "Namespace.deleteLinkedNamespace")
  @Override
  public ResponseEntity<Void> deleteNamespace(String appId, String env, String clusterName,
      String namespaceName, String operator) {
    requireDeleteNamespacePermissionForUserToken(appId, env, clusterName, namespaceName);
    String resolvedOperator = resolveOperator(operator, null);
    namespaceOpenApiService.deleteNamespace(appId, env, clusterName, namespaceName,
        resolvedOperator);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<OpenNamespaceUsageDTO>> findNamespaceUsage(String appId, String env,
      String clusterName, String namespaceName) {
    if (shouldHideConfigToPortalUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
    return ResponseEntity
        .ok(namespaceOpenApiService.findNamespaceUsage(appId, env, clusterName, namespaceName));
  }

  @Override
  public ResponseEntity<Map<String, Map<String, Boolean>>> getNamespacesReleaseStatus(
      String appId) {
    requireReadApplicationPermissionForUserToken(appId);
    if (!unifiedPermissionValidator.hasReadApplicationPermission(appId)) {
      return ResponseEntity.ok(Collections.emptyMap());
    }
    return ResponseEntity.ok(namespaceOpenApiService.getNamespacesReleaseStatus(appId));
  }

  @Override
  public ResponseEntity<List<String>> findMissingNamespaces(String appId, String env,
      String clusterName) {
    requireReadApplicationPermissionForUserToken(appId);
    if (!unifiedPermissionValidator.hasReadApplicationPermission(appId)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    return ResponseEntity
        .ok(namespaceOpenApiService.findMissingNamespaces(appId, env, clusterName));
  }

  @PreAuthorize(value = "@unifiedPermissionValidator.hasCreateNamespacePermission(#appId)")
  @ApolloAuditLog(type = OpType.CREATE, name = "Namespace.createMissingNamespaces")
  @Override
  public ResponseEntity<Void> createMissingNamespaces(String appId, String env, String clusterName,
      String operator) {
    requireCreateNamespacePermissionForUserToken(appId, env, clusterName, null);
    namespaceOpenApiService.createMissingNamespaces(appId, env, clusterName,
        resolveOperator(operator, null));
    return ResponseEntity.ok().build();
  }

  private void validateCreateAppNamespaceRequest(String appId, OpenAppNamespaceDTO appNamespace) {
    RequestPrecondition.checkArguments(appNamespace != null,
        "app namespace payload can not be empty");
    if (!Objects.equals(appId, appNamespace.getAppId())) {
      throw new BadRequestException("AppId not equal. AppId in path = %s, AppId in payload = %s",
          appId, appNamespace.getAppId());
    }
    RequestPrecondition.checkArgumentsNotEmpty(appNamespace.getAppId(), appNamespace.getName(),
        appNamespace.getFormat());

    if (!InputValidator.isValidAppNamespace(appNamespace.getName())) {
      throw BadRequestException
          .invalidNamespaceFormat(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE + " & "
              + InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE);
    }

    if (!ConfigFileFormat.isValidFormat(appNamespace.getFormat())) {
      throw BadRequestException.invalidNamespaceFormat(appNamespace.getFormat());
    }
  }

  public boolean canCreateAppNamespace(String appId, OpenAppNamespaceDTO appNamespace) {
    if (!UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      return unifiedPermissionValidator.hasCreateNamespacePermission(appId);
    }
    if (appNamespace == null) {
      return true;
    }

    AppNamespace permissionModel = new AppNamespace();
    permissionModel.setAppId(appNamespace.getAppId());
    permissionModel.setName(appNamespace.getName());
    permissionModel.setPublic(Boolean.TRUE.equals(appNamespace.getIsPublic()));
    return unifiedPermissionValidator.hasCreateAppNamespacePermission(appId, permissionModel);
  }

  public boolean canCreateNamespaces(List<OpenCreateNamespaceDTO> namespaces) {
    if (CollectionUtils.isEmpty(namespaces)) {
      return true;
    }
    boolean isUserToken =
        UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType());
    for (OpenCreateNamespaceDTO namespace : namespaces) {
      if (namespace == null || StringUtils.isBlank(namespace.getAppId())) {
        continue;
      }
      boolean hasPermission = isUserToken
          ? unifiedPermissionValidator.hasCreateNamespacePermission(namespace.getAppId(),
              namespace.getEnv(), namespace.getClusterName(), namespace.getAppNamespaceName())
          : unifiedPermissionValidator.hasCreateNamespacePermission(namespace.getAppId());
      if (!hasPermission) {
        return false;
      }
    }
    return true;
  }

  private List<OpenAppNamespaceDTO> filterReadableAppNamespaces(
      List<OpenAppNamespaceDTO> appNamespaces) {
    if (appNamespaces == null) {
      return Collections.emptyList();
    }
    return appNamespaces.stream()
        .filter(appNamespace -> appNamespace != null
            && unifiedPermissionValidator.hasReadApplicationPermission(appNamespace.getAppId()))
        .collect(Collectors.toList());
  }

  private List<OpenNamespaceDTO> filterReadableNamespaces(String appId, String env,
      String clusterName, List<OpenNamespaceDTO> namespaces) {
    if (namespaces == null) {
      return Collections.emptyList();
    }
    List<OpenNamespaceDTO> readableNamespaces = namespaces.stream()
        .filter(namespace -> namespace != null && !shouldDenyConfigReadToCurrentIdentity(
            StringUtils.isBlank(namespace.getAppId()) ? appId : namespace.getAppId(), env,
            StringUtils.isBlank(namespace.getClusterName()) ? clusterName
                : namespace.getClusterName(),
            namespace.getNamespaceName()))
        .collect(Collectors.toList());
    return readableNamespaces;
  }

  private boolean shouldHideConfigToPortalUser(String appId, String env, String clusterName,
      String namespaceName) {
    return UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName);
  }

  private boolean shouldDenyConfigReadToCurrentIdentity(String appId, String env,
      String clusterName, String namespaceName) {
    String authType = UserIdentityContextHolder.getAuthType();
    return (UserIdentityConstants.USER.equals(authType)
        || UserIdentityConstants.USER_TOKEN.equals(authType))
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName);
  }

  private void requireConfigReadForUserToken(String appId, String env, String clusterName,
      String namespaceName) {
    if (UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName)) {
      throw new AccessDeniedException("Access is denied");
    }
  }

  private void requireReadApplicationPermissionForUserToken(String appId) {
    if (UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        && !unifiedPermissionValidator.hasReadApplicationPermission(appId)) {
      throw new AccessDeniedException("Access is denied");
    }
  }

  private void requireCreateNamespacesPermissionForUserToken(
      List<OpenCreateNamespaceDTO> namespaces) {
    if (!UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        || CollectionUtils.isEmpty(namespaces)) {
      return;
    }
    for (OpenCreateNamespaceDTO namespace : namespaces) {
      if (namespace == null || StringUtils.isBlank(namespace.getAppId())) {
        continue;
      }
      requireCreateNamespacePermissionForUserToken(namespace.getAppId(), namespace.getEnv(),
          namespace.getClusterName(), namespace.getAppNamespaceName());
    }
  }

  private void requireCreateNamespacePermissionForUserToken(String appId, String env,
      String clusterName, String namespaceName) {
    if (UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        && !unifiedPermissionValidator.hasCreateNamespacePermission(appId, env, clusterName,
            namespaceName)) {
      throw new AccessDeniedException("Create namespace permission is required");
    }
  }

  private void requireDeleteNamespacePermissionForUserToken(String appId, String env,
      String clusterName, String namespaceName) {
    if (UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        && !unifiedPermissionValidator.hasDeleteNamespacePermission(appId, env, clusterName,
            namespaceName)) {
      throw new AccessDeniedException("Delete namespace permission is required");
    }
  }

  private String resolveOperator(String queryOperator, String payloadOperator) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(authType)
        || UserIdentityConstants.USER_TOKEN.equals(authType)) {
      UserInfo loginUser = userInfoHolder.getUser();
      if (loginUser == null || StringUtils.isBlank(loginUser.getUserId())) {
        throw new BadRequestException("Current user not found");
      }
      return loginUser.getUserId();
    }

    if (UserIdentityConstants.CONSUMER.equals(authType)) {
      String operator = StringUtils.isBlank(queryOperator) ? payloadOperator : queryOperator;
      RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(operator),
          "operator should not be null or empty");
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      return operator;
    }

    throw new BadRequestException("Unsupported auth type: %s", authType);
  }
}
