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

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenPageDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Validated
@RestController("openapiItemController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ItemController {

  private final ItemService itemService;
  private final UserService userService;
  private final ItemOpenApiService itemOpenApiService;

  private static final int ITEM_COMMENT_MAX_LENGTH = 256;

  public ItemController(final ItemService itemService, final UserService userService,
      ItemOpenApiService itemOpenApiService) {
    this.itemService = itemService;
    this.userService = userService;
    this.itemOpenApiService = itemOpenApiService;
  }

  @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public OpenItemDTO getItem(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
      @PathVariable String namespaceName, @PathVariable String key) {
    return this.itemOpenApiService.getItem(appId, env, clusterName, namespaceName, key);
  }

  @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public OpenItemDTO getItemByEncodedKey(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName,
      @PathVariable String namespaceName, @PathVariable String key) {
    return this.getItem(appId, env, clusterName, namespaceName,
        new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))));
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PostMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public OpenItemDTO createItem(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @RequestBody OpenItemDTO item) {

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeCreatedBy()),
        "key and dataChangeCreatedBy should not be null or empty");

    if (userService.findByUserId(item.getDataChangeCreatedBy()) == null) {
      throw BadRequestException.userNotExists(item.getDataChangeCreatedBy());
    }

    if (!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > ITEM_COMMENT_MAX_LENGTH) {
      throw new BadRequestException("Comment length should not exceed %s characters", ITEM_COMMENT_MAX_LENGTH);
    }

    return this.itemOpenApiService.createItem(appId, env, clusterName, namespaceName, item);
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public void updateItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestBody OpenItemDTO item,
                         @RequestParam(defaultValue = "false") boolean createIfNotExists) {

    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeLastModifiedBy()),
        "key and dataChangeLastModifiedBy can not be empty");

    RequestPrecondition.checkArguments(item.getKey().equals(key), "Key in path and payload is not consistent");

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
      this.itemOpenApiService.createOrUpdateItem(appId, env, clusterName, namespaceName, item);
    } else {
      this.itemOpenApiService.updateItem(appId, env, clusterName, namespaceName, item);
    }
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @PutMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public void updateItemByEncodedKey(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName,
      @PathVariable String key, @RequestBody OpenItemDTO item,
      @RequestParam(defaultValue = "false") boolean createIfNotExists) {
    this.updateItem(appId, env, clusterName, namespaceName,
        new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))), item,
        createIfNotExists);
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @DeleteMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public void deleteItem(@PathVariable String appId, @PathVariable String env,
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
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
  @DeleteMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public void deleteItemByEncodedKey(@PathVariable String appId, @PathVariable String env,
      @PathVariable String clusterName, @PathVariable String namespaceName,
      @PathVariable String key, @RequestParam String operator) {
    this.deleteItem(appId, env, clusterName, namespaceName,
        new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8))), operator);
  }

  @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public OpenPageDTO<OpenItemDTO> findItemsByNamespace(@PathVariable String appId, @PathVariable String env,
                                                       @PathVariable String clusterName, @PathVariable String namespaceName,
                                                       @Valid @PositiveOrZero(message = "page should be positive or 0")
                                                     @RequestParam(defaultValue = "0") int page,
                                                       @Valid @Positive(message = "size should be positive number")
                                                     @RequestParam(defaultValue = "50") int size) {
    return this.itemOpenApiService.findItemsByNamespace(appId, env, clusterName, namespaceName, page, size);
  }

}
