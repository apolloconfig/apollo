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
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.NamespaceBranchManagementApi;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.ConfigPublishEvent;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController("openapiNamespaceBranchController")
public class NamespaceBranchController implements NamespaceBranchManagementApi {

  private final UnifiedPermissionValidator unifiedPermissionValidator;
  private final ReleaseService releaseService;
  private final NamespaceBranchService namespaceBranchService;
  private final UserService userService;
  private final UserInfoHolder userInfoHolder;
  private final PortalConfig portalConfig;
  private final ApplicationEventPublisher publisher;

  public NamespaceBranchController(final UnifiedPermissionValidator unifiedPermissionValidator,
      final ReleaseService releaseService, final NamespaceBranchService namespaceBranchService,
      final UserService userService, final UserInfoHolder userInfoHolder,
      final PortalConfig portalConfig, final ApplicationEventPublisher publisher) {
    this.unifiedPermissionValidator = unifiedPermissionValidator;
    this.releaseService = releaseService;
    this.namespaceBranchService = namespaceBranchService;
    this.userService = userService;
    this.userInfoHolder = userInfoHolder;
    this.portalConfig = portalConfig;
    this.publisher = publisher;
  }

  @PreAuthorize(
      value = "@openapiNamespaceBranchController.canCreateBranch(#appId, #env, #clusterName, #namespaceName)")
  @ApolloAuditLog(type = OpType.CREATE, name = "NamespaceBranch.create")
  @Override
  public ResponseEntity<OpenNamespaceDTO> createBranch(String appId, String env, String clusterName,
      String namespaceName, String operator) {
    String resolvedOperator = resolveOperator(operator, null);
    NamespaceDTO namespaceDTO = namespaceBranchService.createBranch(appId, Env.valueOf(env),
        clusterName, namespaceName, resolvedOperator);
    return namespaceDTO == null ? ResponseEntity.ok().build()
        : ResponseEntity.ok(OpenApiModelConverters.fromNamespaceDTO(namespaceDTO));
  }

  @Override
  @ApolloAuditLog(type = OpType.DELETE, name = "NamespaceBranch.delete")
  public ResponseEntity<Void> deleteBranch(String env, String appId, String clusterName,
      String namespaceName, String branchName, String operator) {
    if (!canDeleteBranch(appId, env, clusterName, namespaceName, branchName)) {
      throw new AccessDeniedException(
          "Forbidden operation. Caused by: 1.you don't have release permission "
              + "or 2. you don't have modification permission "
              + "or 3. you have modification permission but branch has been released");
    }
    String resolvedOperator = resolveOperator(operator, null);
    namespaceBranchService.deleteBranch(appId, Env.valueOf(env), clusterName, namespaceName,
        branchName, resolvedOperator);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<OpenNamespaceDTO> findBranch(String appId, String env, String clusterName,
      String namespaceName, Boolean extendInfo) {
    requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
    NamespaceBO namespaceBO =
        namespaceBranchService.findBranch(appId, Env.valueOf(env), clusterName, namespaceName);
    if (namespaceBO == null) {
      return ResponseEntity.ok().build();
    }
    if (shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      namespaceBO.hideItems();
    }
    return ResponseEntity.ok(OpenApiModelConverters.fromNamespaceBO(namespaceBO));
  }

  @Override
  public ResponseEntity<OpenGrayReleaseRuleDTO> getBranchGrayRules(String appId, String env,
      String clusterName, String namespaceName, String branchName) {
    requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
    GrayReleaseRuleDTO grayReleaseRuleDTO = namespaceBranchService.findBranchGrayRules(appId,
        Env.valueOf(env), clusterName, namespaceName, branchName);
    return grayReleaseRuleDTO == null ? ResponseEntity.ok().build()
        : ResponseEntity.ok(OpenApiModelConverters.fromGrayReleaseRuleDTO(grayReleaseRuleDTO));
  }

  @PreAuthorize(
      value = "@openapiNamespaceBranchController.canMergeBranch(#appId, #env, #clusterName, #namespaceName)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "NamespaceBranch.merge")
  @Override
  public ResponseEntity<OpenReleaseDTO> merge(String appId, String env, String clusterName,
      String namespaceName, String branchName, Boolean deleteBranch,
      NamespaceReleaseDTO namespaceReleaseDTO) {
    return mergeBranchInternal(appId, env, clusterName, namespaceName, branchName, deleteBranch,
        namespaceReleaseDTO, null);
  }

  @PreAuthorize(
      value = "@openapiNamespaceBranchController.canMergeBranch(#appId, #env, #clusterName, #namespaceName)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "NamespaceBranch.merge")
  @Override
  public ResponseEntity<OpenReleaseDTO> mergeBranch(String env, String appId, String clusterName,
      String namespaceName, String branchName, Boolean deleteBranch,
      NamespaceReleaseDTO namespaceReleaseDTO, String operator) {
    return mergeBranchInternal(appId, env, clusterName, namespaceName, branchName, deleteBranch,
        namespaceReleaseDTO, operator);
  }

  @PreAuthorize(
      value = "@openapiNamespaceBranchController.canUpdateBranchRules(#appId, #env, #clusterName, #namespaceName)")
  @ApolloAuditLog(type = OpType.UPDATE, name = "NamespaceBranch.updateBranchRules")
  @Override
  public ResponseEntity<Void> updateBranchRules(String appId, String env, String clusterName,
      String namespaceName, String branchName, OpenGrayReleaseRuleDTO openGrayReleaseRuleDTO,
      String operator) {
    RequestPrecondition.checkArguments(openGrayReleaseRuleDTO != null,
        "gray release rule payload can not be empty");
    String resolvedOperator = resolveOperator(operator, null);

    openGrayReleaseRuleDTO.setAppId(appId);
    openGrayReleaseRuleDTO.setClusterName(clusterName);
    openGrayReleaseRuleDTO.setNamespaceName(namespaceName);
    openGrayReleaseRuleDTO.setBranchName(branchName);

    namespaceBranchService.updateBranchGrayRules(appId, Env.valueOf(env), clusterName,
        namespaceName, branchName,
        OpenApiModelConverters.toGrayReleaseRuleDTO(openGrayReleaseRuleDTO), resolvedOperator);
    return ResponseEntity.ok().build();
  }

  public boolean canCreateBranch(String appId, String env, String clusterName,
      String namespaceName) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(authType)
        || UserIdentityConstants.USER_TOKEN.equals(authType)) {
      return unifiedPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
          namespaceName);
    }
    if (UserIdentityConstants.CONSUMER.equals(authType)) {
      return unifiedPermissionValidator.hasCreateNamespacePermission(appId);
    }
    return false;
  }

  public boolean canMergeBranch(String appId, String env, String clusterName,
      String namespaceName) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(authType)) {
      return unifiedPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
          namespaceName);
    }
    if (UserIdentityConstants.CONSUMER.equals(authType)
        || UserIdentityConstants.USER_TOKEN.equals(authType)) {
      return unifiedPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName,
          namespaceName);
    }
    return false;
  }

  public boolean canUpdateBranchRules(String appId, String env, String clusterName,
      String namespaceName) {
    String authType = UserIdentityContextHolder.getAuthType();
    if (UserIdentityConstants.USER.equals(authType)) {
      return unifiedPermissionValidator.hasOperateNamespacePermission(appId, env, clusterName,
          namespaceName);
    }
    if (UserIdentityConstants.CONSUMER.equals(authType)
        || UserIdentityConstants.USER_TOKEN.equals(authType)) {
      return unifiedPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName,
          namespaceName);
    }
    return false;
  }

  private ResponseEntity<OpenReleaseDTO> mergeBranchInternal(String appId, String env,
      String clusterName, String namespaceName, String branchName, Boolean deleteBranch,
      NamespaceReleaseDTO namespaceReleaseDTO, String operator) {
    RequestPrecondition.checkArguments(namespaceReleaseDTO != null,
        "release payload can not be empty");
    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(namespaceReleaseDTO.getReleaseTitle()),
        "Params(releaseTitle) can not be empty");

    String resolvedOperator = resolveOperator(operator, namespaceReleaseDTO.getReleasedBy());
    boolean emergencyPublish = Boolean.TRUE.equals(namespaceReleaseDTO.getIsEmergencyPublish());
    checkEmergencyPublishAllowedForUser(env, emergencyPublish);

    ReleaseDTO createdRelease = namespaceBranchService.merge(appId, Env.valueOf(env), clusterName,
        namespaceName, branchName, namespaceReleaseDTO.getReleaseTitle(),
        namespaceReleaseDTO.getReleaseComment(), emergencyPublish,
        deleteBranch == null || deleteBranch, resolvedOperator);

    ConfigPublishEvent event = ConfigPublishEvent.instance();
    event.withAppId(appId).withCluster(clusterName).withNamespace(namespaceName)
        .withReleaseId(createdRelease.getId()).setMergeEvent(true).setEnv(Env.valueOf(env));
    publisher.publishEvent(event);

    return ResponseEntity.ok(OpenApiModelConverters.fromReleaseDTO(createdRelease));
  }

  private boolean canDeleteBranch(String appId, String env, String clusterName,
      String namespaceName, String branchName) {
    boolean hasReleasePermission = unifiedPermissionValidator.hasReleaseNamespacePermission(appId,
        env, clusterName, namespaceName);
    boolean hasModifyPermission = unifiedPermissionValidator.hasModifyNamespacePermission(appId,
        env, clusterName, namespaceName);
    return hasReleasePermission || (hasModifyPermission && releaseService.loadLatestRelease(appId,
        Env.valueOf(env), branchName, namespaceName) == null);
  }

  private void checkEmergencyPublishAllowedForUser(String env, boolean emergencyPublish) {
    if (emergencyPublish
        && UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
        && !portalConfig.isEmergencyPublishAllowed(Env.valueOf(env))) {
      throw new BadRequestException("Env: %s is not supported emergency publish now", env);
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

  private boolean shouldHideConfigToCurrentUser(String appId, String env, String clusterName,
      String namespaceName) {
    return UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())
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
}
