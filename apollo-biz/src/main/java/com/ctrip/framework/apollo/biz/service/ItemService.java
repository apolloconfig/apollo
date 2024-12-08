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
package com.ctrip.framework.apollo.biz.service;


import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Audit;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.repository.ItemRepository;
import com.ctrip.framework.apollo.common.dto.ItemInfoDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.utils.StringUtils;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ItemService {

  private static Pattern clusterPattern = Pattern.compile("[0-9]{14}-[a-zA-Z0-9]{16}");

  private final ItemRepository itemRepository;
  private final NamespaceService namespaceService;
  private final AuditService auditService;
  private final BizConfig bizConfig;

  public ItemService(
      final ItemRepository itemRepository,
      final @Lazy NamespaceService namespaceService,
      final AuditService auditService,
      final BizConfig bizConfig) {
    this.itemRepository = itemRepository;
    this.namespaceService = namespaceService;
    this.auditService = auditService;
    this.bizConfig = bizConfig;
  }


  @Transactional
  public Item delete(long id, String operator) {
    Item item = itemRepository.findById(id).orElse(null);
    if (item == null) {
      throw new IllegalArgumentException("item not exist. ID:" + id);
    }

    item.setDeleted(true);
    item.setDataChangeLastModifiedBy(operator);
    Item deletedItem = itemRepository.save(item);

    auditService.audit(Item.class.getSimpleName(), id, Audit.OP.DELETE, operator);
    return deletedItem;
  }

  @Transactional
  public int batchDelete(long namespaceId, String operator) {
    return itemRepository.deleteByNamespaceId(namespaceId, operator);

  }

  public Item findOne(String appId, String clusterName, String namespaceName, String key) {
    Namespace namespace = findNamespaceByAppIdAndClusterNameAndNamespaceName(appId, clusterName, namespaceName);
    return itemRepository.findByNamespaceIdAndKey(namespace.getId(), key);
  }

  public Item findLastOne(String appId, String clusterName, String namespaceName) {
    Namespace namespace = findNamespaceByAppIdAndClusterNameAndNamespaceName(appId, clusterName, namespaceName);
    return findLastOne(namespace.getId());
  }

  public Item findLastOne(long namespaceId) {
    return itemRepository.findFirst1ByNamespaceIdOrderByLineNumDesc(namespaceId);
  }

  public Item findOne(long itemId) {
    return itemRepository.findById(itemId).orElse(null);
  }

  public List<Item> findItemsWithoutOrdered(Long namespaceId) {
    List<Item> items = itemRepository.findByNamespaceId(namespaceId);
    if (items == null) {
      return Collections.emptyList();
    }
    return items;
  }

  public List<Item> findItemsWithoutOrdered(String appId, String clusterName, String namespaceName) {
    Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
    if (namespace != null) {
      return findItemsWithoutOrdered(namespace.getId());
    }
    return Collections.emptyList();
  }

  public List<Item> findItemsWithOrdered(Long namespaceId) {
    List<Item> items = itemRepository.findByNamespaceIdOrderByLineNumAsc(namespaceId);
    if (items == null) {
      return Collections.emptyList();
    }
    return items;
  }

  public List<Item> findItemsWithOrdered(String appId, String clusterName, String namespaceName) {
    Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
    if (namespace != null) {
      return findItemsWithOrdered(namespace.getId());
    }
    return Collections.emptyList();
  }

  public List<Item> findItemsModifiedAfterDate(long namespaceId, Date date) {
    return itemRepository.findByNamespaceIdAndDataChangeLastModifiedTimeGreaterThan(namespaceId, date);
  }

  public int findNonEmptyItemCount(long namespaceId) {
    return itemRepository.countByNamespaceIdAndFilterKeyEmpty(namespaceId);
  }

  public Page<Item> findItemsByKey(String key, Pageable pageable) {
    return itemRepository.findByKey(key, pageable);
  }

  public Page<Item> findItemsByNamespace(String appId, String clusterName, String namespaceName, Pageable pageable) {
    Namespace namespace = findNamespaceByAppIdAndClusterNameAndNamespaceName(appId, clusterName, namespaceName);
    return itemRepository.findByNamespaceId(namespace.getId(), pageable);
  }

  public Page<ItemInfoDTO> getItemInfoBySearch(String key, String value, Pageable limit) {
    Page<ItemInfoDTO> itemInfoDTOs;
    if (key.isEmpty() && !value.isEmpty()) {
      itemInfoDTOs = itemRepository.findItemsByValueLike(value, limit);
    } else if (value.isEmpty() && !key.isEmpty()) {
      itemInfoDTOs = itemRepository.findItemsByKeyLike(key, limit);
    } else {
      itemInfoDTOs = itemRepository.findItemsByKeyAndValueLike(key, value, limit);
    }
    return itemInfoDTOs;
  }

  @Transactional
  public Item save(Item entity) {
    checkItemKeyLength(entity.getKey());
    checkItemType(entity.getType());
    checkItemValueLength(entity.getNamespaceId(), entity.getValue());

    entity.setId(0);//protection

    if (entity.getLineNum() == 0) {
      Item lastItem = findLastOne(entity.getNamespaceId());
      int lineNum = lastItem == null ? 1 : lastItem.getLineNum() + 1;
      entity.setLineNum(lineNum);
    }

    Item item = itemRepository.save(entity);

    auditService.audit(Item.class.getSimpleName(), item.getId(), Audit.OP.INSERT,
                       item.getDataChangeCreatedBy());

    return item;
  }

  @Transactional
  public Item saveComment(Item entity) {
    entity.setKey("");
    entity.setValue("");
    entity.setId(0);//protection

    if (entity.getLineNum() == 0) {
      Item lastItem = findLastOne(entity.getNamespaceId());
      int lineNum = lastItem == null ? 1 : lastItem.getLineNum() + 1;
      entity.setLineNum(lineNum);
    }

    Item item = itemRepository.save(entity);

    auditService.audit(Item.class.getSimpleName(), item.getId(), Audit.OP.INSERT,
                       item.getDataChangeCreatedBy());

    return item;
  }

  @Transactional
  public Item update(Item item) {
    checkItemType(item.getType());
    checkItemValueLength(item.getNamespaceId(), item.getValue());
    Item managedItem = itemRepository.findById(item.getId()).orElse(null);
    BeanUtils.copyEntityProperties(item, managedItem);
    managedItem = itemRepository.save(managedItem);

    auditService.audit(Item.class.getSimpleName(), managedItem.getId(), Audit.OP.UPDATE,
                       managedItem.getDataChangeLastModifiedBy());

    return managedItem;
  }

  private boolean checkItemValueLength(long namespaceId, String value) {
    Namespace currentNamespace = namespaceService.findOne(namespaceId);
    int limit = getItemValueLengthLimit(currentNamespace);
    if(currentNamespace != null) {
      Matcher m = clusterPattern.matcher(currentNamespace.getClusterName());
      boolean isGray = m.matches();
      if (isGray) {
        limit = getGrayNamespaceItemValueLengthLimit(currentNamespace, limit);
      }
    }
    if (!StringUtils.isEmpty(value) && value.length() > limit) {
      throw new BadRequestException("value too long. length limit:" + limit);
    }
    return true;
  }

  private int getGrayNamespaceItemValueLengthLimit(Namespace grayNamespace, int grayNamespaceLimit) {
    Namespace parentNamespace = namespaceService.findParentNamespace(grayNamespace);
    if (parentNamespace != null) {
      int parentLimit = getItemValueLengthLimit(grayNamespace);
      if (parentLimit > grayNamespaceLimit) {
        return parentLimit;
      }
    }
    return grayNamespaceLimit;
  }

  private boolean checkItemKeyLength(String key) {
    if (!StringUtils.isEmpty(key) && key.length() > bizConfig.itemKeyLengthLimit()) {
      throw new BadRequestException("key too long. length limit:" + bizConfig.itemKeyLengthLimit());
    }
    return true;
  }

  private boolean checkItemType(int type) {
    if (type < 0 || type > 3) {
      throw new BadRequestException("type is invalid. type should be in [0, 3]. ");
    }
    return true;
  }

  private int getItemValueLengthLimit(Namespace namespace) {
    Map<Long, Integer> namespaceValueLengthOverride = bizConfig.namespaceValueLengthLimitOverride();
    if (namespaceValueLengthOverride != null && namespaceValueLengthOverride.containsKey(namespace.getId())) {
      return namespaceValueLengthOverride.get(namespace.getId());
    }

    Map<String, Integer> appIdValueLengthOverride = bizConfig.appIdValueLengthLimitOverride();
    if (appIdValueLengthOverride != null && appIdValueLengthOverride.containsKey(namespace.getAppId())) {
      return appIdValueLengthOverride.get(namespace.getAppId());
    }

    return bizConfig.itemValueLengthLimit();
  }

  private Namespace findNamespaceByAppIdAndClusterNameAndNamespaceName(String appId,
                                                                       String clusterName,
                                                                       String namespaceName) {
    Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
    if (namespace == null) {
      throw NotFoundException.namespaceNotFound(appId, clusterName, namespaceName);
    }
    return namespace;
  }

}
