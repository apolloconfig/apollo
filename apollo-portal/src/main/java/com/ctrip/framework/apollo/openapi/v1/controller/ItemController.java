/*
 * Copyright 2021 Apollo Authors
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
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.openapi.api.ItemOpenApiService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
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


@RestController("openapiItemController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ItemController {

  private final ItemService itemService;
  private final UserService userService;
  private final ItemOpenApiService itemOpenApiService;

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

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#request, #appId, #namespaceName, #env)")
  @PostMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public OpenItemDTO createItem(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @RequestBody OpenItemDTO item, HttpServletRequest request) {

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeCreatedBy()),
        "key and dataChangeCreatedBy should not be null or empty");

    if (userService.findByUserId(item.getDataChangeCreatedBy()) == null) {
      throw new BadRequestException("User " + item.getDataChangeCreatedBy() + " doesn't exist!");
    }

    if(!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > 256){
      throw new BadRequestException("Comment length should not exceed 256 characters");
    }

    return this.itemOpenApiService.createItem(appId, env, clusterName, namespaceName, item);
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#request, #appId, #namespaceName, #env)")
  @PutMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public void updateItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestBody OpenItemDTO item,
                         @RequestParam(defaultValue = "false") boolean createIfNotExists, HttpServletRequest request) {

    RequestPrecondition.checkArguments(item != null, "item payload can not be empty");

    RequestPrecondition.checkArguments(
        !StringUtils.isContainEmpty(item.getKey(), item.getDataChangeLastModifiedBy()),
        "key and dataChangeLastModifiedBy can not be empty");

    RequestPrecondition.checkArguments(item.getKey().equals(key), "Key in path and payload is not consistent");

    if (userService.findByUserId(item.getDataChangeLastModifiedBy()) == null) {
      throw new BadRequestException("user(dataChangeLastModifiedBy) not exists");
    }

    if(!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > 256){
      throw new BadRequestException("Comment length should not exceed 256 characters");
    }

    if (createIfNotExists) {
      this.itemOpenApiService.createOrUpdateItem(appId, env, clusterName, namespaceName, item);
    } else {
      this.itemOpenApiService.updateItem(appId, env, clusterName, namespaceName, item);
    }
  }


  @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#request, #appId, #namespaceName, #env)")
  @DeleteMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public void deleteItem(@PathVariable String appId, @PathVariable String env,
                         @PathVariable String clusterName, @PathVariable String namespaceName,
                         @PathVariable String key, @RequestParam String operator,
                         HttpServletRequest request) {

    if (userService.findByUserId(operator) == null) {
      throw new BadRequestException("user(operator) not exists");
    }

    ItemDTO toDeleteItem = itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    if (toDeleteItem == null){
      throw new BadRequestException("item not exists");
    }

    this.itemOpenApiService.removeItem(appId, env, clusterName, namespaceName, key, operator);
  }

}
