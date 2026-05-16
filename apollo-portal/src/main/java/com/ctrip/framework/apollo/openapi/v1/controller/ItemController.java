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

import static com.ctrip.framework.apollo.common.utils.RequestPrecondition.checkModel;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.ItemManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemDiffDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceIdentifier;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceSyncDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceTextModel;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.NamespaceTextSyntaxChecker;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController("openapiItemController")
public class ItemController implements ItemManagementApi {

  private static final int ITEM_COMMENT_MAX_LENGTH = 256;
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 50;
  private static final String HIDDEN_CONFIG_MESSAGE =
      "You are not this project's administrator, nor do you have edit or release permission for the namespace: ";

  private final ItemOpenApiService itemOpenApiService;
  private final UserService userService;
  private final UserInfoHolder userInfoHolder;
  private final UnifiedPermissionValidator unifiedPermissionValidator;

  public ItemController(ItemOpenApiService itemOpenApiService, UserService userService,
      UserInfoHolder userInfoHolder, UnifiedPermissionValidator unifiedPermissionValidator) {
    this.itemOpenApiService = itemOpenApiService;
    this.userService = userService;
    this.userInfoHolder = userInfoHolder;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
  }

  @Override
  public ResponseEntity<OpenItemDTO> getItem(String appId, String env, String clusterName,
      String namespaceName, String key) {
    if (shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok().build();
    }
    return ResponseEntity
        .ok(this.itemOpenApiService.getItem(appId, env, clusterName, namespaceName, key));
  }

  @Override
  public ResponseEntity<OpenItemDTO> getItemByEncodedKey(String appId, String env,
      String clusterName, String namespaceName, String key) {
    return getItem(appId, env, clusterName, namespaceName, decodeBase64(key));
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<OpenItemDTO> createItem(String appId, String env, String clusterName,
      String namespaceName, OpenItemDTO item, String operator) {
    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");
    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(item.getKey()),
        "key should not be null or empty");
    RequestPrecondition.checkArguments(!Objects.isNull(item.getValue()),
        "value should not be null");
    checkCommentLength(item.getComment());

    String resolvedOperator = resolveOperator(operator, item.getDataChangeCreatedBy());
    item.setDataChangeCreatedBy(resolvedOperator);
    item.setDataChangeLastModifiedBy(resolvedOperator);

    return ResponseEntity.ok(this.itemOpenApiService.createItem(appId, env, clusterName,
        namespaceName, item, resolvedOperator));
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> updateItem(String appId, String env, String clusterName,
      String namespaceName, String key, Boolean createIfNotExists, OpenItemDTO item,
      String operator) {
    updateItemInternal(appId, env, clusterName, namespaceName, key,
        Boolean.TRUE.equals(createIfNotExists), item, operator);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> updateItemByEncodedKey(String appId, String env, String clusterName,
      String namespaceName, String key, Boolean createIfNotExists, OpenItemDTO item,
      String operator) {
    updateItemInternal(appId, env, clusterName, namespaceName, decodeBase64(key),
        Boolean.TRUE.equals(createIfNotExists), item, operator);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> deleteItem(String appId, String env, String clusterName,
      String namespaceName, String key, String operator) {
    String resolvedOperator = resolveOperator(operator, null);
    this.itemOpenApiService.removeItem(appId, env, clusterName, namespaceName, key,
        resolvedOperator);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> deleteItemByEncodedKey(String appId, String env, String clusterName,
      String namespaceName, String key, String operator) {
    return deleteItem(appId, env, clusterName, namespaceName, decodeBase64(key), operator);
  }

  @Override
  public ResponseEntity<OpenItemPageDTO> findItemsByNamespace(String appId, String env,
      String clusterName, String namespaceName, Integer page, Integer size) {
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    int resolvedSize = size == null ? DEFAULT_SIZE : size;
    if (shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return ResponseEntity.ok(emptyPage(resolvedPage, resolvedSize));
    }
    return ResponseEntity.ok(this.itemOpenApiService.findItemsByNamespace(appId, env, clusterName,
        namespaceName, resolvedPage, resolvedSize));
  }

  @Override
  public ResponseEntity<List<OpenItemDTO>> findBranchItems(String appId, String env,
      String clusterName, String namespaceName, String branchName) {
    if (shouldHideConfigToCurrentUser(appId, env, branchName, namespaceName)) {
      return ResponseEntity.ok(Collections.emptyList());
    }
    return ResponseEntity
        .ok(this.itemOpenApiService.findBranchItems(appId, env, branchName, namespaceName));
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> batchUpdateItemsByText(String appId, String env, String clusterName,
      String namespaceName, OpenNamespaceTextModel model, String operator) {
    RequestPrecondition.checkArguments(model != null, "namespace text payload can not be empty");
    String resolvedOperator = resolveOperator(operator, model.getOperator());
    model.setOperator(resolvedOperator);
    this.itemOpenApiService.batchUpdateItemsByText(appId, env, clusterName, namespaceName, model,
        resolvedOperator);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<OpenItemDiffDTO>> compareItems(String appId, String env,
      String clusterName, String namespaceName, OpenNamespaceSyncDTO model) {
    checkModel(!isInvalid(model));
    List<OpenItemDiffDTO> itemDiffs =
        this.itemOpenApiService.compareItems(appId, env, clusterName, namespaceName, model);
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      hideConfigDiffsIfNeeded(itemDiffs);
    }
    return ResponseEntity.ok(itemDiffs);
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> syncItems(String appId, String env, String clusterName,
      String namespaceName, OpenNamespaceSyncDTO model, String operator) {
    checkModel(!isInvalid(model) && syncToNamespacesValid(appId, namespaceName, model));
    checkSyncPermissions(model);
    String resolvedOperator = resolveOperator(operator, null);
    this.itemOpenApiService.syncItems(appId, env, clusterName, namespaceName, model,
        resolvedOperator);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> syntaxCheck(String appId, String env, String clusterName,
      String namespaceName, OpenNamespaceTextModel model) {
    RequestPrecondition.checkArguments(model != null, "namespace text payload can not be empty");
    NamespaceTextModel namespaceTextModel = OpenApiModelConverters.toNamespaceTextModel(model);
    doSyntaxCheck(namespaceTextModel);
    return ResponseEntity.ok().build();
  }

  @PreAuthorize(
      value = "@unifiedPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @Override
  public ResponseEntity<Void> revertItems(String appId, String env, String clusterName,
      String namespaceName, String operator) {
    String resolvedOperator = resolveOperator(operator, null);
    this.itemOpenApiService.revertItems(appId, env, clusterName, namespaceName, resolvedOperator);
    return ResponseEntity.ok().build();
  }

  private void updateItemInternal(String appId, String env, String clusterName,
      String namespaceName, String key, boolean createIfNotExists, OpenItemDTO item,
      String operator) {
    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");
    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(item.getKey()),
        "key should not be null or empty");
    RequestPrecondition.checkArguments(item.getKey().equals(key),
        "Key in path and payload is not consistent");
    RequestPrecondition.checkArguments(!Objects.isNull(item.getValue()),
        "value should not be null");
    checkCommentLength(item.getComment());

    String payloadOperator = item.getDataChangeLastModifiedBy();
    if (createIfNotExists && StringUtils.isBlank(payloadOperator)) {
      payloadOperator = item.getDataChangeCreatedBy();
    }
    String resolvedOperator = resolveOperator(operator, payloadOperator);
    item.setDataChangeLastModifiedBy(resolvedOperator);
    if (createIfNotExists && StringUtils.isBlank(item.getDataChangeCreatedBy())) {
      item.setDataChangeCreatedBy(resolvedOperator);
    }

    if (createIfNotExists) {
      this.itemOpenApiService.createOrUpdateItem(appId, env, clusterName, namespaceName, item,
          resolvedOperator);
    } else {
      this.itemOpenApiService.updateItem(appId, env, clusterName, namespaceName, item,
          resolvedOperator);
    }
  }

  private void checkCommentLength(String comment) {
    if (!StringUtils.isEmpty(comment) && comment.length() > ITEM_COMMENT_MAX_LENGTH) {
      throw new BadRequestException("Comment length should not exceed %s characters",
          ITEM_COMMENT_MAX_LENGTH);
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

  private OpenItemPageDTO emptyPage(Integer page, Integer size) {
    OpenItemPageDTO result = new OpenItemPageDTO();
    result.setPage(page);
    result.setSize(size);
    result.setTotal(0L);
    result.setContent(Collections.emptyList());
    return result;
  }

  private void hideConfigDiffsIfNeeded(List<OpenItemDiffDTO> itemDiffs) {
    for (OpenItemDiffDTO diff : itemDiffs) {
      OpenNamespaceIdentifier namespace = diff.getNamespace();
      if (namespace == null) {
        continue;
      }

      if (unifiedPermissionValidator.shouldHideConfigToCurrentUser(namespace.getAppId(),
          namespace.getEnv(), namespace.getClusterName(), namespace.getNamespaceName())) {
        diff.setCreateItems(Collections.emptyList());
        diff.setUpdateItems(Collections.emptyList());
        diff.setDeleteItems(Collections.emptyList());
        diff.setMessage(HIDDEN_CONFIG_MESSAGE + namespace);
      }
    }
  }

  private boolean isInvalid(OpenNamespaceSyncDTO model) {
    if (model == null || model.getSyncToNamespaces() == null
        || model.getSyncToNamespaces().isEmpty() || model.getSyncItems() == null) {
      return true;
    }
    for (OpenNamespaceIdentifier namespaceIdentifier : model.getSyncToNamespaces()) {
      if (namespaceIdentifier == null || StringUtils.isContainEmpty(namespaceIdentifier.getEnv(),
          namespaceIdentifier.getClusterName(), namespaceIdentifier.getNamespaceName())) {
        return true;
      }
    }
    return false;
  }

  private boolean syncToNamespacesValid(String appId, String namespaceName,
      OpenNamespaceSyncDTO model) {
    for (OpenNamespaceIdentifier namespaceIdentifier : model.getSyncToNamespaces()) {
      if (Objects.equals(appId, namespaceIdentifier.getAppId())
          && Objects.equals(namespaceName, namespaceIdentifier.getNamespaceName())) {
        continue;
      }
      return false;
    }
    return true;
  }

  private void checkSyncPermissions(OpenNamespaceSyncDTO model) {
    OpenNamespaceIdentifier noPermissionNamespace = null;
    boolean hasPermission = true;
    for (OpenNamespaceIdentifier namespaceIdentifier : model.getSyncToNamespaces()) {
      hasPermission = unifiedPermissionValidator.hasModifyNamespacePermission(
          namespaceIdentifier.getAppId(), namespaceIdentifier.getEnv(),
          namespaceIdentifier.getClusterName(), namespaceIdentifier.getNamespaceName());
      if (!hasPermission) {
        noPermissionNamespace = namespaceIdentifier;
        break;
      }
    }
    if (!hasPermission) {
      throw new AccessDeniedException(String
          .format("You don't have the permission to modify namespace: %s", noPermissionNamespace));
    }
  }

  private String decodeBase64(String key) {
    try {
      return decodeBase64(key, Base64.getDecoder());
    } catch (IllegalArgumentException standardBase64Exception) {
      try {
        return decodeBase64(key, Base64.getUrlDecoder());
      } catch (IllegalArgumentException urlBase64Exception) {
        throw new BadRequestException("Invalid encoded key");
      }
    }
  }

  private String decodeBase64(String key, Base64.Decoder decoder) {
    return new String(decoder.decode(key), StandardCharsets.UTF_8);
  }

  void doSyntaxCheck(NamespaceTextModel model) {
    NamespaceTextSyntaxChecker.check(model);
  }
}
