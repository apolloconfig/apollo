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
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.openapi.model.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemDiffDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceSyncDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceTextModel;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Server-side Item OpenAPI service implementation.
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
    ItemDTO itemDTO =
        itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    return itemDTO == null ? null : OpenApiModelConverters.fromItemDTO(itemDTO);
  }

  @Override
  public OpenItemDTO createItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO, String operator) {

    ItemDTO toCreate = OpenApiModelConverters.toItemDTO(itemDTO);

    // protect
    toCreate.setLineNum(0);
    toCreate.setId(0);
    toCreate.setDataChangeCreatedBy(operator);
    toCreate.setDataChangeLastModifiedBy(operator);
    toCreate.setDataChangeLastModifiedTime(null);
    toCreate.setDataChangeCreatedTime(null);

    ItemDTO createdItem =
        itemService.createItem(appId, Env.valueOf(env), clusterName, namespaceName, toCreate);
    return createdItem == null ? null : OpenApiModelConverters.fromItemDTO(createdItem);
  }

  @Override
  public void updateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO, String operator) {
    ItemDTO toUpdateItem =
        itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, itemDTO.getKey());
    if (toUpdateItem == null) {
      throw NotFoundException.itemNotFound(appId, clusterName, namespaceName, itemDTO.getKey());
    }
    // protect. only value,type,comment,lastModifiedBy can be modified
    toUpdateItem.setComment(itemDTO.getComment());
    toUpdateItem.setType(itemDTO.getType());
    toUpdateItem.setValue(itemDTO.getValue());
    toUpdateItem.setDataChangeLastModifiedBy(operator);

    itemService.updateItem(appId, Env.valueOf(env), clusterName, namespaceName, toUpdateItem);
  }

  @Override
  public void createOrUpdateItem(String appId, String env, String clusterName, String namespaceName,
      OpenItemDTO itemDTO, String operator) {
    ItemDTO existing =
        itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, itemDTO.getKey());
    if (existing == null) {
      try {
        this.createItem(appId, env, clusterName, namespaceName, itemDTO, operator);
      } catch (RuntimeException ex) {
        if (!isItemAlreadyExists(ex, itemDTO.getKey())) {
          throw ex;
        }
        this.updateItem(appId, env, clusterName, namespaceName, itemDTO, operator);
      }
      return;
    }
    this.updateItem(appId, env, clusterName, namespaceName, itemDTO, operator);
  }

  @Override
  public void removeItem(String appId, String env, String clusterName, String namespaceName,
      String key, String operator) {
    ItemDTO toDeleteItem =
        this.itemService.loadItem(Env.valueOf(env), appId, clusterName, namespaceName, key);
    if (toDeleteItem == null) {
      throw NotFoundException.itemNotFound(appId, clusterName, namespaceName, key);
    }
    this.itemService.deleteItem(Env.valueOf(env), toDeleteItem.getId(), operator);
  }

  @Override
  public OpenItemPageDTO findItemsByNamespace(String appId, String env, String clusterName,
      String namespaceName, int page, int size) {
    PageDTO<com.ctrip.framework.apollo.openapi.dto.OpenItemDTO> items = this.itemService
        .findItemsByNamespace(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
    return OpenApiModelConverters.fromLegacyOpenItemPageDTO(items);
  }

  @Override
  public List<OpenItemDTO> findBranchItems(String appId, String env, String branchName,
      String namespaceName) {
    return OpenApiModelConverters
        .fromItemDTOs(itemService.findItems(appId, Env.valueOf(env), branchName, namespaceName));
  }

  @Override
  public void batchUpdateItemsByText(String appId, String env, String clusterName,
      String namespaceName, OpenNamespaceTextModel model, String operator) {
    NamespaceTextModel namespaceTextModel = OpenApiModelConverters.toNamespaceTextModel(model);
    namespaceTextModel.setAppId(appId);
    namespaceTextModel.setEnv(env);
    namespaceTextModel.setClusterName(clusterName);
    namespaceTextModel.setNamespaceName(namespaceName);
    namespaceTextModel.setOperator(operator);
    itemService.updateConfigItemByText(namespaceTextModel, operator);
  }

  @Override
  public List<OpenItemDiffDTO> compareItems(String appId, String env, String clusterName,
      String namespaceName, OpenNamespaceSyncDTO model) {
    List<NamespaceIdentifier> syncToNamespaces =
        OpenApiModelConverters.toNamespaceIdentifiers(model.getSyncToNamespaces());
    List<ItemDTO> syncItems = OpenApiModelConverters.toItemDTOs(model.getSyncItems());
    List<ItemDiffs> itemDiffs = itemService.compare(syncToNamespaces, syncItems);
    return OpenApiModelConverters.fromItemDiffs(itemDiffs);
  }

  @Override
  public void syncItems(String appId, String env, String clusterName, String namespaceName,
      OpenNamespaceSyncDTO model, String operator) {
    itemService.syncItems(
        OpenApiModelConverters.toNamespaceIdentifiers(model.getSyncToNamespaces()),
        OpenApiModelConverters.toItemDTOs(model.getSyncItems()), operator);
  }

  @Override
  public void revertItems(String appId, String env, String clusterName, String namespaceName,
      String operator) {
    itemService.revokeItem(appId, Env.valueOf(env), clusterName, namespaceName, operator);
  }

  private boolean isItemAlreadyExists(RuntimeException ex, String key) {
    String expectedMessage = BadRequestException.itemAlreadyExists(key).getMessage();
    if (ex instanceof BadRequestException) {
      return expectedMessage.equals(ex.getMessage());
    }
    if (ex instanceof HttpStatusCodeException statusException
        && statusException.getStatusCode() == HttpStatus.BAD_REQUEST) {
      return statusException.getResponseBodyAsString().contains(expectedMessage);
    }
    return false;
  }
}
