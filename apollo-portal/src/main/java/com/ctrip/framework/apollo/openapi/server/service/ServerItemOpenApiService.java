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
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.openapi.api.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenPageDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author wxq
 */
@Service
public class ServerItemOpenApiService implements ItemOpenApiService {

  private final ItemService itemService;

  public ServerItemOpenApiService(ItemService itemService) {
    this.itemService = itemService;
  }

  @Override
  public OpenItemDTO getItem(String appId, String env, String clusterName, String namespaceName,
      String key) {
    ItemDTO itemDTO = itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    return itemDTO == null ? null : OpenApiBeanUtils.transformFromItemDTO(itemDTO);
  }

  @Override
  public OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {

    ItemDTO toCreate = OpenApiBeanUtils.transformToItemDTO(itemDTO);

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

  @Override
  public void updateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {
    ItemDTO toUpdateItem = itemService
        .loadItem(Env.valueOf(env), appId, clusterName, namespaceName, itemDTO.getKey());
    //protect. only value,type,comment,lastModifiedBy can be modified
    toUpdateItem.setComment(itemDTO.getComment());
    toUpdateItem.setType(itemDTO.getType());
    toUpdateItem.setValue(itemDTO.getValue());
    toUpdateItem.setDataChangeLastModifiedBy(itemDTO.getDataChangeLastModifiedBy());

    itemService.updateItem(appId, Env.valueOf(env), clusterName, namespaceName, toUpdateItem);
  }

  @Override
  public void createOrUpdateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO) {
    try {
      this.updateItem(appId, env, clusterName, namespaceName, itemDTO);
    } catch (Throwable ex) {
      if (ex instanceof HttpStatusCodeException) {
        // check createIfNotExists
        if (((HttpStatusCodeException) ex).getStatusCode().equals(HttpStatus.NOT_FOUND)) {
          this.createItem(appId, env, clusterName, namespaceName, itemDTO);
          return;
        }
      }
      throw ex;
    }
  }

  @Override
  public void removeItem(String appId, String env, String clusterName, String namespaceName,
      String key, String operator) {
    ItemDTO toDeleteItem = this.itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    this.itemService.deleteItem(Env.valueOf(env), toDeleteItem.getId(), operator);
  }

  @Override
  public OpenPageDTO<OpenItemDTO> findItemsByNamespace(String appId, String env, String clusterName,
                                                       String namespaceName, int page, int size) {
    PageDTO<OpenItemDTO> commonOpenItemDTOPage =
            this.itemService.findItemsByNamespace(appId, Env.valueOf(env), clusterName, namespaceName, page, size);

    return new OpenPageDTO<>(commonOpenItemDTOPage.getPage(), commonOpenItemDTOPage.getSize(),
            commonOpenItemDTOPage.getTotal(), commonOpenItemDTOPage.getContent());
  }
}
