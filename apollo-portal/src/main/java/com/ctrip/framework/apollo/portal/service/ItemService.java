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
package com.ctrip.framework.apollo.portal.service;


import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.dto.*;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.openapi.utils.UrlUtils;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.ItemAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.NamespaceAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.ReleaseAPI;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.txtresolver.ConfigTextResolver;
import com.ctrip.framework.apollo.portal.constant.TracerEventType;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {
  private static final Gson GSON = new Gson();

  private final UserInfoHolder userInfoHolder;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;
  private final AdminServiceAPI.ItemAPI itemAPI;
  private final AdminServiceAPI.ReleaseAPI releaseAPI;
  private final ConfigTextResolver fileTextResolver;
  private final ConfigTextResolver propertyResolver;

  public ItemService(
      final UserInfoHolder userInfoHolder,
      final NamespaceAPI namespaceAPI,
      final ItemAPI itemAPI,
      final ReleaseAPI releaseAPI,
      final @Qualifier("fileTextResolver") ConfigTextResolver fileTextResolver,
      final @Qualifier("propertyResolver") ConfigTextResolver propertyResolver) {
    this.userInfoHolder = userInfoHolder;
    this.namespaceAPI = namespaceAPI;
    this.itemAPI = itemAPI;
    this.releaseAPI = releaseAPI;
    this.fileTextResolver = fileTextResolver;
    this.propertyResolver = propertyResolver;
  }


  /**
   * parse config text and update config items
   *
   * @return parse result
   */
  public void updateConfigItemByText(NamespaceTextModel model) {
    String appId = model.getAppId();
    Env env = model.getEnv();
    String clusterName = model.getClusterName();
    String namespaceName = model.getNamespaceName();

    NamespaceDTO namespace = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    if (namespace == null) {
      throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    }
    long namespaceId = namespace.getId();

    // In case someone constructs an attack scenario
    if (model.getNamespaceId() != namespaceId) {
      throw BadRequestException.namespaceNotExists();
    }

    String configText = model.getConfigText();

    ConfigTextResolver resolver =
        model.getFormat() == ConfigFileFormat.Properties ? propertyResolver : fileTextResolver;

    ItemChangeSets changeSets = resolver.resolve(namespaceId, configText,
        itemAPI.findItems(appId, env, clusterName, namespaceName));
    if (changeSets.isEmpty()) {
      return;
    }

    String operator = model.getOperator();
    if (StringUtils.isBlank(operator)) {
      operator = userInfoHolder.getUser().getUserId();
    }
    changeSets.setDataChangeLastModifiedBy(operator);

    updateItems(appId, env, clusterName, namespaceName, changeSets);

    Tracer.logEvent(TracerEventType.MODIFY_NAMESPACE_BY_TEXT,
        String.format("%s+%s+%s+%s", appId, env, clusterName, namespaceName));
    Tracer.logEvent(TracerEventType.MODIFY_NAMESPACE, String.format("%s+%s+%s+%s", appId, env, clusterName, namespaceName));
  }

  public void updateItems(String appId, Env env, String clusterName, String namespaceName, ItemChangeSets changeSets){
    itemAPI.updateItemsByChangeSet(appId, env, clusterName, namespaceName, changeSets);
  }


  public ItemDTO createItem(String appId, Env env, String clusterName, String namespaceName, ItemDTO item) {
    NamespaceDTO namespace = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    if (namespace == null) {
      throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    }
    item.setNamespaceId(namespace.getId());

    ItemDTO itemDTO = itemAPI.createItem(appId, env, clusterName, namespaceName, item);
    Tracer.logEvent(TracerEventType.MODIFY_NAMESPACE, String.format("%s+%s+%s+%s", appId, env, clusterName, namespaceName));
    return itemDTO;
  }

  public ItemDTO createCommentItem(String appId, Env env, String clusterName, String namespaceName, ItemDTO item) {
    NamespaceDTO namespace = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    if (namespace == null) {
      throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    }
    item.setNamespaceId(namespace.getId());

    return itemAPI.createCommentItem(appId, env, clusterName, namespaceName, item);
  }

  public void updateItem(String appId, Env env, String clusterName, String namespaceName, ItemDTO item) {
    itemAPI.updateItem(appId, env, clusterName, namespaceName, item.getId(), item);
  }

  public void deleteItem(Env env, long itemId, String userId) {
    itemAPI.deleteItem(env, itemId, userId);
  }

  public List<ItemDTO> findItems(String appId, Env env, String clusterName, String namespaceName) {
    return itemAPI.findItems(appId, env, clusterName, namespaceName);
  }

  public List<ItemDTO> findDeletedItems(String appId, Env env, String clusterName, String namespaceName) {
    return itemAPI.findDeletedItems(appId, env, clusterName, namespaceName);
  }

  public ItemDTO loadItem(Env env, String appId, String clusterName, String namespaceName, String key) {
    if (UrlUtils.hasIllegalChar(key)) {
      return itemAPI.loadItemByEncodeKey(env, appId, clusterName, namespaceName, key);
    }
    return itemAPI.loadItem(env, appId, clusterName, namespaceName, key);
  }

  public ItemDTO loadItemById(Env env, long itemId) {
    ItemDTO item = itemAPI.loadItemById(env, itemId);
    if (item == null) {
      throw NotFoundException.itemNotFound(itemId);
    }
    return item;
  }

  public void syncItems(List<NamespaceIdentifier> comparedNamespaces, List<ItemDTO> sourceItems) {
    List<ItemDiffs> itemDiffs = compare(comparedNamespaces, sourceItems);
    for (ItemDiffs itemDiff : itemDiffs) {
      NamespaceIdentifier namespaceIdentifier = itemDiff.getNamespace();
      ItemChangeSets changeSets = itemDiff.getDiffs();
      changeSets.setDataChangeLastModifiedBy(userInfoHolder.getUser().getUserId());

      String appId = namespaceIdentifier.getAppId();
      Env env = namespaceIdentifier.getEnv();
      String clusterName = namespaceIdentifier.getClusterName();
      String namespaceName = namespaceIdentifier.getNamespaceName();

      itemAPI.updateItemsByChangeSet(appId, env, clusterName, namespaceName, changeSets);

      Tracer.logEvent(TracerEventType.SYNC_NAMESPACE, String.format("%s+%s+%s+%s", appId, env, clusterName, namespaceName));
    }
  }


  public void revokeItem(String appId, Env env, String clusterName, String namespaceName) {

    NamespaceDTO namespace = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    if (namespace == null) {
      throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    }
    long namespaceId = namespace.getId();

    Map<String, String> releaseItemDTOs = new HashMap<>();
    ReleaseDTO latestRelease = releaseAPI.loadLatestRelease(appId,env,clusterName,namespaceName);
    if (latestRelease != null) {
      releaseItemDTOs = GSON.fromJson(latestRelease.getConfigurations(), GsonType.CONFIG);
    }
    List<ItemDTO> baseItems = itemAPI.findItems(appId, env, clusterName, namespaceName);
    Map<String, ItemDTO> oldKeyMapItem = BeanUtils.mapByKey("key", baseItems);
    //remove comment and blank item map.
    oldKeyMapItem.remove("");

    //deleted items for comment
    Map<String, ItemDTO> deletedItemDTOs = findDeletedItems(appId, env, clusterName, namespaceName).stream()
            .filter(itemDTO -> !StringUtils.isEmpty(itemDTO.getKey()))
            .collect(Collectors.toMap(itemDTO -> itemDTO.getKey(), v -> v, (v1, v2) -> v2));

    ItemChangeSets changeSets = new ItemChangeSets();
    AtomicInteger lineNum = new AtomicInteger(1);
    releaseItemDTOs.forEach((key,value) -> {
      ItemDTO oldItem = oldKeyMapItem.get(key);
      if (oldItem == null) {
        ItemDTO deletedItemDto = deletedItemDTOs.computeIfAbsent(key, k -> new ItemDTO());
        int newLineNum = 0 == deletedItemDto.getLineNum() ? lineNum.get() : deletedItemDto.getLineNum();
        changeSets.addCreateItem(buildNormalItem(0L, namespaceId, key, value, deletedItemDto.getComment(), newLineNum));
      } else if (!StringUtils.equals(oldItem.getValue(), value) || lineNum.get() != oldItem.getLineNum()) {
        changeSets.addUpdateItem(buildNormalItem(oldItem.getId(), namespaceId, key, value, oldItem.getComment(), oldItem.getLineNum()));
      }
      oldKeyMapItem.remove(key);
      lineNum.set(lineNum.get() + 1);
    });
    oldKeyMapItem.forEach((key, value) -> changeSets.addDeleteItem(value));
    changeSets.setDataChangeLastModifiedBy(userInfoHolder.getUser().getUserId());

    updateItems(appId, env, clusterName, namespaceName, changeSets);

    String formatStr = String.format("%s+%s+%s+%s", appId, env, clusterName, namespaceName);
    Tracer.logEvent(TracerEventType.MODIFY_NAMESPACE_BY_TEXT, formatStr);
    Tracer.logEvent(TracerEventType.MODIFY_NAMESPACE, formatStr);
  }

  public List<ItemDiffs> compare(List<NamespaceIdentifier> comparedNamespaces, List<ItemDTO> sourceItems) {

    List<ItemDiffs> result = new LinkedList<>();

    for (NamespaceIdentifier namespace : comparedNamespaces) {

      ItemDiffs itemDiffs = new ItemDiffs(namespace);
      try {
        itemDiffs.setDiffs(parseChangeSets(namespace, sourceItems));
      } catch (BadRequestException e) {
        itemDiffs.setDiffs(new ItemChangeSets());
        itemDiffs.setExtInfo("该集群下没有名为 " + namespace.getNamespaceName() + " 的namespace");
      }
      result.add(itemDiffs);
    }

    return result;
  }

  public PageDTO<OpenItemDTO> findItemsByNamespace(String appId, Env env, String clusterName,
                                                   String namespaceName, int page, int size) {
    return itemAPI.findItemsByNamespace(appId, env, clusterName, namespaceName, page, size);
  }

  private long getNamespaceId(NamespaceIdentifier namespaceIdentifier) {
    String appId = namespaceIdentifier.getAppId();
    String clusterName = namespaceIdentifier.getClusterName();
    String namespaceName = namespaceIdentifier.getNamespaceName();
    Env env = namespaceIdentifier.getEnv();
    NamespaceDTO namespaceDTO;
    try {
      namespaceDTO = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
      }
      throw e;
    }
    return namespaceDTO.getId();
  }

  private ItemChangeSets parseChangeSets(NamespaceIdentifier namespace, List<ItemDTO> sourceItems) {
    ItemChangeSets changeSets = new ItemChangeSets();
    List<ItemDTO>
        targetItems =
        itemAPI.findItems(namespace.getAppId(), namespace.getEnv(),
            namespace.getClusterName(), namespace.getNamespaceName());

    long namespaceId = getNamespaceId(namespace);

    if (CollectionUtils.isEmpty(targetItems)) {//all source items is added
      int lineNum = 1;
      for (ItemDTO sourceItem : sourceItems) {
        changeSets.addCreateItem(buildItem(namespaceId, lineNum++, sourceItem));
      }
    } else {
      Map<String, ItemDTO> targetItemMap = BeanUtils.mapByKey("key", targetItems);
      String key, sourceValue, sourceComment;
      ItemDTO targetItem = null;
      int maxLineNum = targetItems.size();//append to last
      for (ItemDTO sourceItem : sourceItems) {
        key = sourceItem.getKey();
        sourceValue = sourceItem.getValue();
        sourceComment = sourceItem.getComment();
        targetItem = targetItemMap.get(key);

        if (targetItem == null) {//added items

          changeSets.addCreateItem(buildItem(namespaceId, ++maxLineNum, sourceItem));

        } else if (isModified(sourceValue, targetItem.getValue(), sourceComment,
            targetItem.getComment())) {//modified items
          targetItem.setValue(sourceValue);
          targetItem.setComment(sourceComment);
          changeSets.addUpdateItem(targetItem);
        }
      }
    }

    return changeSets;
  }

  private ItemDTO buildItem(long namespaceId, int lineNum, ItemDTO sourceItem) {
    ItemDTO createdItem = new ItemDTO();
    BeanUtils.copyEntityProperties(sourceItem, createdItem);
    createdItem.setLineNum(lineNum);
    createdItem.setNamespaceId(namespaceId);
    return createdItem;
  }

  private ItemDTO buildNormalItem(Long id, Long namespaceId, String key, String value, String comment, int lineNum) {
    ItemDTO item = new ItemDTO(key, value, comment, lineNum);
    item.setId(id);
    item.setNamespaceId(namespaceId);
    return item;
  }

  private boolean isModified(String sourceValue, String targetValue, String sourceComment, String targetComment) {

    if (!sourceValue.equals(targetValue)) {
      return true;
    }

    if (sourceComment == null) {
      return !StringUtils.isEmpty(targetComment);
    }
    if (targetComment != null) {
      return !sourceComment.equals(targetComment);
    }
    return false;
  }
}
