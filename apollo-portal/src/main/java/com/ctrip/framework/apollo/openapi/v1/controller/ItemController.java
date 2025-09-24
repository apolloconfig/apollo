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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.*;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;


@Validated
@RestController("openapiItemController")
@RequestMapping("/openapi/v1")
public class ItemController {

  private final UserService userService;
  private final ItemOpenApiService itemOpenApiService;
  private final ConsumerPermissionValidator consumerPermissionValidator;

  private static final int ITEM_COMMENT_MAX_LENGTH = 256;

  public ItemController(UserService userService, ItemOpenApiService itemOpenApiService,
                        ConsumerPermissionValidator consumerPermissionValidator) {
    this.userService = userService;
    this.itemOpenApiService = itemOpenApiService;
    this.consumerPermissionValidator = consumerPermissionValidator;
  }

  /**
   * 获取单个配置项
   * GET openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}
   */
  @GetMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public ResponseEntity<OpenItemDTO> getItem(@PathVariable String appId, @PathVariable String env, 
      @PathVariable String clusterName, @PathVariable String namespaceName, @PathVariable String key) {
    OpenItemDTO item = this.itemOpenApiService.getItem(appId, env, clusterName, namespaceName, key);
    if (item == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(item);
  }

  /**
   * 通过查询参数获取配置项（支持编码的key）
   * GET openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items?key={key}&encoded={true|false}
   */
  @GetMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public ResponseEntity<OpenItemDTO> getItemByEncodedKey(@PathVariable String appId, @PathVariable String env,
                                                         @PathVariable String clusterName,
                                                         @PathVariable String namespaceName, @PathVariable String key) {
    return this.getItem(appId, env, clusterName, namespaceName,
            new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))));
  }

  /**
   * 创建新的配置项
   * POST openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public ResponseEntity<OpenItemDTO> createItem(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @RequestBody OpenItemDTO item) {

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeCreatedBy()),
        "key and dataChangeCreatedBy should not be null or empty");

    RequestPrecondition.checkArguments(!Objects.isNull(item.getValue()), "value should not be null");

    if (userService.findByUserId(item.getDataChangeCreatedBy()) == null) {
      throw BadRequestException.userNotExists(item.getDataChangeCreatedBy());
    }

    if (!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > ITEM_COMMENT_MAX_LENGTH) {
      throw new BadRequestException("Comment length should not exceed %s characters", ITEM_COMMENT_MAX_LENGTH);
    }

    OpenItemDTO createdItem = this.itemOpenApiService.createItem(appId, env, clusterName, namespaceName, item);
    return ResponseEntity.ok(createdItem);
  }

  /**
   * 更新配置项
   * PUT openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public ResponseEntity<Void> updateItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestBody OpenItemDTO item,
                         @RequestParam(defaultValue = "false") boolean createIfNotExists) {

    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeLastModifiedBy()),
        "key and dataChangeLastModifiedBy can not be empty");
    RequestPrecondition.checkArguments(!Objects.isNull(item.getType()), "type should not be null");
    RequestPrecondition.checkArguments(Objects.equals(item.getKey(), key), "Key in path and payload is not consistent");
    RequestPrecondition.checkArguments(!Objects.isNull(item.getValue()), "value should not be null");

    if (userService.findByUserId(item.getDataChangeLastModifiedBy()) == null) {
      throw BadRequestException.userNotExists(item.getDataChangeLastModifiedBy());
    }

    if (!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > ITEM_COMMENT_MAX_LENGTH) {
      throw new BadRequestException("Comment length should not exceed %s characters", ITEM_COMMENT_MAX_LENGTH);
    }

    if (createIfNotExists) {
      if (StringUtils.isEmpty(item.getDataChangeCreatedBy())) {
        throw new BadRequestException("dataChangeCreatedBy is required when createIfNotExists is true");
      }
      itemOpenApiService.createOrUpdateItem(appId, env, clusterName, namespaceName, item);
    } else {
      itemOpenApiService.updateItem(appId, env, clusterName, namespaceName, item);
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * 通过编码的key更新配置项
   * PUT openapi/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public ResponseEntity<Void> updateItemByEncodedKey(@PathVariable String appId, @PathVariable String env,
                                     @PathVariable String clusterName, @PathVariable String namespaceName,
                                     @PathVariable String key, @RequestBody OpenItemDTO item,
                                     @RequestParam(defaultValue = "false") boolean createIfNotExists) {
    return this.updateItem(appId, env, clusterName, namespaceName,
              new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))), item,
              createIfNotExists);
  }

  /**
   * 删除配置项
   * DELETE openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @DeleteMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public ResponseEntity<Void> deleteItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestParam(value = "operator", required = false) String operator) {

    OpenItemDTO toDeleteItem = itemOpenApiService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    if (toDeleteItem == null) {
      throw NotFoundException.itemNotFound(appId, clusterName, namespaceName, key);
    }

    this.itemOpenApiService.removeItem(appId, env, clusterName, namespaceName, key);
    return ResponseEntity.noContent().build();
  }

  /**
   * 通过编码的key删除配置项
   * DELETE openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @DeleteMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public ResponseEntity<Void> deleteItemByEncodedKey(@PathVariable String appId, @PathVariable String env,
                                     @PathVariable String clusterName, @PathVariable String namespaceName,
                                     @PathVariable String key, @RequestParam(value = "operator", required = false) String operator) {
    return this.deleteItem(appId, env, clusterName, namespaceName,
            new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))), operator);
  }

  /**
   * 通过namespace查询item
   * GET openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items
   */
  @GetMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public OpenPageDTOOpenItemDTO findItemsByNamespace(@PathVariable String appId, @PathVariable String env,
                                                       @PathVariable String clusterName, @PathVariable String namespaceName,
                                                       @Valid @PositiveOrZero(message = "page should be positive or 0")
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @Valid @Positive(message = "size should be positive number")
                                                       @RequestParam(defaultValue = "50") int size) {
    return this.itemOpenApiService.findItemsByNamespace(appId, env, clusterName, namespaceName, page, size);
  }

  /**
   * 通过文本批量修改配置项
   * PUT openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items:batchUpdate
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items:batchUpdate", consumes = {"application/json"})
  public ResponseEntity<Void> batchUpdateItemsByText(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @RequestBody OpenNamespaceTextModel model) {
    itemOpenApiService.modifyItemsByText(appId, env, clusterName, namespaceName, model);
    return ResponseEntity.noContent().build();
  }

  /**
   * 获取分支下的配置项
   * GET openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/items
   */
  @GetMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/items")
  public ResponseEntity<List<OpenItemDTO>> getBranchItems(@PathVariable String appId, @PathVariable String env,
                                           @PathVariable String clusterName, @PathVariable String namespaceName,
                                           @PathVariable String branchName) {
    List<OpenItemDTO> items = itemOpenApiService.findBranchItems(appId, env, clusterName, namespaceName, branchName);
    return ResponseEntity.ok(items);
  }

  /**
   * 对比命名空间配置差异
   * POST openapi/v1/namespaces/items:compare
   */
  @PostMapping(value = "namespaces/items:compare", consumes = {"application/json"})
  public ResponseEntity<List<OpenItemDiffs>> compareItems(@RequestBody OpenNamespaceSyncModel model) {
    RequestPrecondition.checkArguments(checkNamespaceSyncModel(model), "model is invalid");
    List<OpenItemDiffs> itemDiffs = itemOpenApiService.diff(model);

    for (OpenItemDiffs diff : itemDiffs) {
      OpenNamespaceIdentifier namespace = diff.getNamespace();
      if (namespace == null) {
        continue;
      }

      if (consumerPermissionValidator
          .shouldHideConfigToCurrentUser(namespace.getAppId(), namespace.getEnv(),
              namespace.getClusterName(), namespace.getNamespaceName())) {
        diff.setDiffs(new OpenItemChangeSets());
        diff.setExtInfo("You are not this project's administrator, nor you have edit or release permission for the namespace: " + namespace);
      }
    }

    return ResponseEntity.ok(itemDiffs);
  }

  /**
   * 同步配置项到多个命名空间
   * POST openapi/v1/apps/{appId}/namespaces/{namespaceName}/items:sync
   */
  @PostMapping(value = "/apps/{appId}/namespaces/{namespaceName}/items:sync", consumes = {"application/json"})
  public ResponseEntity<Void> syncItems(@PathVariable String appId, @PathVariable String namespaceName, @RequestBody OpenNamespaceSyncModel model) {
    RequestPrecondition.checkArguments(checkNamespaceSyncModel(model) && syncToNamespacesValid(appId, namespaceName, model),
        "model is invalid");
    
    OpenNamespaceIdentifier noPermissionNamespace = null;
    // 检查用户是否拥有每个命名空间的修改权限
    boolean hasPermission = true;
    for (OpenNamespaceIdentifier namespaceIdentifier : model.getSyncToNamespaces()) {
      // 一旦用户没有其中一个命名空间的修改权限，就中断循环
      hasPermission = consumerPermissionValidator.hasModifyNamespacePermission(
          namespaceIdentifier.getAppId(),
          namespaceIdentifier.getEnv(),
          namespaceIdentifier.getClusterName(),
          namespaceIdentifier.getNamespaceName()
      );
      if (!hasPermission) {
        noPermissionNamespace = namespaceIdentifier;
        break;
      }
    }
    
    if (hasPermission) {
      itemOpenApiService.syncItems(model);
      return ResponseEntity.accepted().build();
    }
    
    throw new AccessDeniedException(String.format("You don't have the permission to modify namespace: %s", noPermissionNamespace));
  }

  /**
   * 验证配置文本语法
   * POST openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items:validate
   */

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items:validate", consumes = {"application/json"})
  public ResponseEntity<Void> validateItems(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName, @RequestBody OpenNamespaceTextModel model) {

    itemOpenApiService.syntaxCheckText(model);

    return ResponseEntity.ok().build();
  }

  /**
   * 撤销配置项更改
   * POST openapi/v1/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items:revert
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping("/apps/{appId}/env/{env}/clusters/{clusterName}/namespaces/{namespaceName}/items:revert")
  public ResponseEntity<Void> revertItems(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
      @PathVariable String namespaceName) {
    itemOpenApiService.revokeItems(appId, env, clusterName, namespaceName);
    return ResponseEntity.accepted().build();
  }


  private boolean checkNamespaceSyncModel(OpenNamespaceSyncModel namespaceSyncModel) {
    if (CollectionUtils.isEmpty(namespaceSyncModel.getSyncToNamespaces()) || CollectionUtils.isEmpty(namespaceSyncModel.getSyncItems())) {
      return false;
    }
    for (OpenNamespaceIdentifier namespaceIdentifier : namespaceSyncModel.getSyncToNamespaces()) {
      if (checkNamespaceIdentifier(namespaceIdentifier)) {
        return false;
      }
    }
    return true;
  }

  private boolean checkNamespaceIdentifier(OpenNamespaceIdentifier namespaceIdentifier) {
    return StringUtils.isContainEmpty(namespaceIdentifier.getEnv(), namespaceIdentifier.getClusterName(), namespaceIdentifier.getClusterName());
  }

  public boolean syncToNamespacesValid(String appId, String namespaceName, OpenNamespaceSyncModel namespaceSyncModel) {
    for (OpenNamespaceIdentifier namespaceIdentifier : namespaceSyncModel.getSyncToNamespaces()) {
      if (appId.equals(namespaceIdentifier.getAppId()) && namespaceName.equals(
              namespaceIdentifier.getNamespaceName())) {
        continue;
      }
      return false;
    }
    return true;
  }
}