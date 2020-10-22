package com.ctrip.framework.apollo.openapi.v1.controller;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.http.HttpServletRequest;


@RestController("openapiItemController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ItemController {

  private final ItemService itemService;
  private final UserService userService;

  public ItemController(final ItemService itemService, final UserService userService) {
    this.itemService = itemService;
    this.userService = userService;
  }

  @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public OpenItemDTO getItem(@PathVariable String appId, @PathVariable String env, @PathVariable String clusterName,
      @PathVariable String namespaceName, @PathVariable String key) {

    ItemDTO itemDTO = itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);

    return itemDTO == null ? null : OpenApiBeanUtils.transformFromItemDTO(itemDTO);
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

    if(!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > 64){
      throw new BadRequestException("Comment length should not exceed 64 characters");
    }

    ItemDTO toCreate = OpenApiBeanUtils.transformToItemDTO(item);

    //protect
    toCreate.setLineNum(0);
    toCreate.setId(0);
    toCreate.setDataChangeLastModifiedBy(toCreate.getDataChangeCreatedBy());
    toCreate.setDataChangeLastModifiedTime(null);
    toCreate.setDataChangeCreatedTime(null);

    ItemDTO createdItem = itemService.createItem(appId, Env.valueOf(env),
        clusterName, namespaceName, toCreate);
    return OpenApiBeanUtils.transformFromItemDTO(createdItem);
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

    if(!StringUtils.isEmpty(item.getComment()) && item.getComment().length() > 64){
      throw new BadRequestException("Comment length should not exceed 64 characters");
    }

    try {
      ItemDTO toUpdateItem = itemService
          .loadItem(Env.valueOf(env), appId, clusterName, namespaceName, item.getKey());
      //protect. only value,comment,lastModifiedBy can be modified
      toUpdateItem.setComment(item.getComment());
      toUpdateItem.setValue(item.getValue());
      toUpdateItem.setDataChangeLastModifiedBy(item.getDataChangeLastModifiedBy());

      itemService.updateItem(appId, Env.valueOf(env), clusterName, namespaceName, toUpdateItem);
    } catch (Throwable ex) {
      if (ex instanceof HttpStatusCodeException) {
        // check createIfNotExists
        if (((HttpStatusCodeException) ex).getStatusCode().equals(HttpStatus.NOT_FOUND) && createIfNotExists) {
          createItem(appId, env, clusterName, namespaceName, item, request);
          return;
        }
      }
      throw ex;
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

    itemService.deleteItem(Env.valueOf(env), toDeleteItem.getId(), operator);
  }

}
