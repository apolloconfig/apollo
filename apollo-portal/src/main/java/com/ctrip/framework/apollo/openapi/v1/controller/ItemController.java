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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenPageDTO;
import com.ctrip.framework.apollo.openapi.server.service.ServerItemOpenApiService;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceSyncModel;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.spi.UserService;

/**
 * RESTful API Controller for Configuration Items
 * 
 * Base URL: /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}
 * 
 * @author Apollo Team
 */
@Validated
@RestController("openapiItemController")
@RequestMapping("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}")
public class ItemController {

  private final ItemService itemService;
  private final UserService userService;
  private final ItemOpenApiService itemOpenApiService;
  private final ServerItemOpenApiService serverItemOpenApiService;
  private final UserPermissionValidator userPermissionValidator;

  private static final int ITEM_COMMENT_MAX_LENGTH = 256;

  public ItemController(final ItemService itemService, final UserService userService,
      ItemOpenApiService itemOpenApiService, ServerItemOpenApiService serverItemOpenApiService,
      UserPermissionValidator userPermissionValidator) {
    this.itemService = itemService;
    this.userService = userService;
    this.itemOpenApiService = itemOpenApiService;
    this.serverItemOpenApiService = serverItemOpenApiService;
    this.userPermissionValidator = userPermissionValidator;
  }

  /**
   * 获取单个配置项
   * GET /items/{key}
   */
  @GetMapping(value = "/items/{key:.+}")
  public ResponseEntity<OpenItemDTO> getItem(@PathVariable String appId, @PathVariable String env, 
      @PathVariable String clusterName, @PathVariable String namespaceName, @PathVariable String key) {
    OpenItemDTO item = this.itemOpenApiService.getItem(appId, env, clusterName, namespaceName, key);
    if (item == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(item);
  }

  /**
   * 获取命名空间下的所有配置项（分页）
   * GET /items
   */
  @GetMapping(value = "/items", params = {"!key"})
  public ResponseEntity<OpenPageDTO<OpenItemDTO>> listItems(@PathVariable String appId, @PathVariable String env,
                                                       @PathVariable String clusterName, @PathVariable String namespaceName,
                                                       @Valid @PositiveOrZero(message = "page should be positive or 0")
                                                     @RequestParam(defaultValue = "0") int page,
                                                       @Valid @Positive(message = "size should be positive number")
                                                     @RequestParam(defaultValue = "50") int size,
                                                     @RequestParam(defaultValue = "lineNum") String sort) {
    if ("enhanced".equals(sort) || "lastModifiedTime".equals(sort)) {
      List<OpenItemDTO> items = serverItemOpenApiService.findItemsWithOrder(appId, env, clusterName, namespaceName, sort);
      // 手动分页
      int start = page * size;
      int end = Math.min(start + size, items.size());
      List<OpenItemDTO> pageItems = start < items.size() ? items.subList(start, end) : java.util.Collections.emptyList();
      OpenPageDTO<OpenItemDTO> pageDTO = new OpenPageDTO<>(page, size, items.size(), pageItems);
      return ResponseEntity.ok(pageDTO);
    }
    OpenPageDTO<OpenItemDTO> result = this.itemOpenApiService.findItemsByNamespace(appId, env, clusterName, namespaceName, page, size);
    return ResponseEntity.ok(result);
  }

  /**
   * 通过查询参数获取配置项（支持编码的key）
   * GET /items?key={key}&encoded={true|false}
   */
  @GetMapping(value = "/items")
  public ResponseEntity<OpenItemDTO> getItemByQuery(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName, 
      @RequestParam String key, @RequestParam(defaultValue = "false") boolean encoded) {
    String actualKey = encoded ? new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))) : key;
    return getItem(appId, env, clusterName, namespaceName, actualKey);
  }

  /**
   * 创建新的配置项
   * POST /items
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/items")
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
    return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
  }

  /**
   * 更新配置项（完整更新）
   * PUT /items/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/items/{key:.+}")
  public ResponseEntity<Void> updateItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestBody OpenItemDTO item,
                         @RequestParam(defaultValue = "false") boolean upsert) {

    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeLastModifiedBy()),
        "key and dataChangeLastModifiedBy can not be empty");

    RequestPrecondition.checkArguments(item.getKey().equals(key), "Key in path and payload is not consistent");
    RequestPrecondition.checkArguments(!Objects.isNull(item.getValue()), "value should not be null");

    if (userService.findByUserId(item.getDataChangeLastModifiedBy()) == null) {
      throw BadRequestException.userNotExists(item.getDataChangeLastModifiedBy());
    }

    if (!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > ITEM_COMMENT_MAX_LENGTH) {
      throw new BadRequestException("Comment length should not exceed %s characters", ITEM_COMMENT_MAX_LENGTH);
    }

    if (upsert) {
      if (StringUtils.isEmpty(item.getDataChangeCreatedBy())) {
        throw new BadRequestException("dataChangeCreatedBy is required when upsert is true");
      }
      this.itemOpenApiService.createOrUpdateItem(appId, env, clusterName, namespaceName, item);
      return ResponseEntity.ok().build();
    } else {
      this.itemOpenApiService.updateItem(appId, env, clusterName, namespaceName, item);
      return ResponseEntity.noContent().build();
    }
  }

  /**
   * 部分更新配置项
   * PATCH /items/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PatchMapping(value = "/items/{key:.+}")
  public ResponseEntity<Void> patchItem(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName,
      @PathVariable String key, @RequestBody OpenItemDTO item) {
    // 对于PATCH，只更新提供的字段
    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");
    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(item.getDataChangeLastModifiedBy()),
        "dataChangeLastModifiedBy can not be empty");
    
    if (userService.findByUserId(item.getDataChangeLastModifiedBy()) == null) {
      throw BadRequestException.userNotExists(item.getDataChangeLastModifiedBy());
    }
    
    this.itemOpenApiService.updateItem(appId, env, clusterName, namespaceName, item);
    return ResponseEntity.noContent().build();
  }

  /**
   * 删除配置项
   * DELETE /items/{key}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @DeleteMapping(value = "/items/{key:.+}")
  public ResponseEntity<Void> deleteItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestParam String operator) {

    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }

    ItemDTO toDeleteItem = itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    if (toDeleteItem == null) {
      throw NotFoundException.itemNotFound(appId, clusterName, namespaceName, key);
    }

    this.itemOpenApiService.removeItem(appId, env, clusterName, namespaceName, key, operator);
    return ResponseEntity.noContent().build();
  }

  /**
   * 通过文本批量修改配置项
   * PUT /items:batchUpdate
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/items:batchUpdate", consumes = {"application/json"})
  public ResponseEntity<Void> batchUpdateItemsByText(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @RequestBody NamespaceTextModel model) {
    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(model.getOperator()),
        "operator should not be null or empty");

    if (userService.findByUserId(model.getOperator()) == null) {
      throw BadRequestException.userNotExists(model.getOperator());
    }

    serverItemOpenApiService.modifyItemsByText(appId, env, clusterName, namespaceName, model);
    return ResponseEntity.noContent().build();
  }

  /**
   * 获取分支下的配置项
   * GET /branches/{branchName}/items
   */
  @GetMapping(value = "/branches/{branchName}/items")
  public ResponseEntity<List<OpenItemDTO>> getBranchItems(@PathVariable String appId, @PathVariable String env,
                                           @PathVariable String clusterName, @PathVariable String namespaceName,
                                           @PathVariable String branchName) {
    List<OpenItemDTO> items = serverItemOpenApiService.findBranchItems(appId, env, clusterName, namespaceName, branchName);
    return ResponseEntity.ok(items);
  }

  /**
   * 对比命名空间配置差异
   * POST /items:compare
   */
  @PostMapping(value = "/items:compare", consumes = {"application/json"})
  public ResponseEntity<List<ItemDiffs>> compareItems(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName, @RequestBody NamespaceSyncModel model) {
    RequestPrecondition.checkArguments(!model.isInvalid(), "model is invalid");

    List<ItemDiffs> itemDiffs = serverItemOpenApiService.diff(model);

    for (ItemDiffs diff : itemDiffs) {
      NamespaceIdentifier namespace = diff.getNamespace();
      if (namespace == null) {
        continue;
      }

      if (userPermissionValidator
          .shouldHideConfigToCurrentUser(namespace.getAppId(), namespace.getEnv().getName(),
              namespace.getClusterName(), namespace.getNamespaceName())) {
        diff.setDiffs(new ItemChangeSets());
        diff.setExtInfo("You are not this project's administrator, nor you have edit or release permission for the namespace: " + namespace);
      }
    }

    return ResponseEntity.ok(itemDiffs);
  }

  /**
   * 同步配置项到多个命名空间
   * POST /items:sync
   */
  @PostMapping(value = "/items:sync", consumes = {"application/json"})
  public ResponseEntity<Void> syncItems(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName, @RequestBody NamespaceSyncModel model) {
    RequestPrecondition.checkArguments(!model.isInvalid() && model.syncToNamespacesValid(appId, namespaceName),
        "model is invalid");
    
    NamespaceIdentifier noPermissionNamespace = null;
    // 检查用户是否拥有每个命名空间的修改权限
    boolean hasPermission = true;
    for (NamespaceIdentifier namespaceIdentifier : model.getSyncToNamespaces()) {
      // 一旦用户没有其中一个命名空间的修改权限，就中断循环
      hasPermission = userPermissionValidator.hasModifyNamespacePermission(
          namespaceIdentifier.getAppId(),
          namespaceIdentifier.getEnv().getName(),
          namespaceIdentifier.getClusterName(),
          namespaceIdentifier.getNamespaceName()
      );
      if (!hasPermission) {
        noPermissionNamespace = namespaceIdentifier;
        break;
      }
    }
    
    if (hasPermission) {
      serverItemOpenApiService.syncItems(model);
      return ResponseEntity.accepted().build();
    }
    
    throw new AccessDeniedException(String.format("You don't have the permission to modify namespace: %s", noPermissionNamespace));
  }

  /**
   * 验证配置文本语法
   * POST /items:validate
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/items:validate", consumes = {"application/json"})
  public ResponseEntity<Void> validateItems(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName, @RequestBody NamespaceTextModel model) {

    serverItemOpenApiService.syntaxCheckText(model);

    return ResponseEntity.ok().build();
  }

  /**
   * 撤销配置项更改
   * POST /items:revert
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping("/items:revert")
  public ResponseEntity<Void> revertItems(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
      @PathVariable String namespaceName) {
    serverItemOpenApiService.revokeItems(appId, env, clusterName, namespaceName);
    return ResponseEntity.accepted().build();
  }

}