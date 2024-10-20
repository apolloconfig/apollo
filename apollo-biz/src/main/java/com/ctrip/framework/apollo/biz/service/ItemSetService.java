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
import com.ctrip.framework.apollo.biz.utils.ConfigChangeContentBuilder;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service
public class ItemSetService {

  private final AuditService auditService;
  private final CommitService commitService;
  private final ItemService itemService;
  private final NamespaceService namespaceService;
  private final BizConfig bizConfig;

  public ItemSetService(
      final AuditService auditService,
      final CommitService commitService,
      final ItemService itemService,
      final NamespaceService namespaceService,
      final BizConfig bizConfig) {
    this.auditService = auditService;
    this.commitService = commitService;
    this.itemService = itemService;
    this.namespaceService = namespaceService;
    this.bizConfig = bizConfig;
  }

  @Transactional
  public ItemChangeSets updateSet(Namespace namespace, ItemChangeSets changeSets) {
    return updateSet(namespace.getAppId(), namespace.getClusterName(), namespace.getNamespaceName(), changeSets);
  }

  @Transactional
  public ItemChangeSets updateSet(String appId, String clusterName,
                                  String namespaceName, ItemChangeSets changeSet) {
    Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);

    if (namespace == null) {
      throw NotFoundException.namespaceNotFound(appId, clusterName, namespaceName);
    }

    if (bizConfig.isItemNumLimitEnabled()) {
      int itemCount = itemService.findNonEmptyItemCount(namespace.getId());
      int createItemCount = (int) changeSet.getCreateItems().stream().filter(item -> !StringUtils.isEmpty(item.getKey())).count();
      int deleteItemCount = (int) changeSet.getDeleteItems().stream().filter(item -> !StringUtils.isEmpty(item.getKey())).count();
      itemCount = itemCount + createItemCount - deleteItemCount;
      if (itemCount > bizConfig.itemNumLimit()) {
        throw new BadRequestException("The maximum number of items (" + bizConfig.itemNumLimit() + ") for this namespace has been reached. Current item count is " + itemCount + ".");
      }
    }

    String operator = changeSet.getDataChangeLastModifiedBy();
    ConfigChangeContentBuilder configChangeContentBuilder = new ConfigChangeContentBuilder();

    if (!CollectionUtils.isEmpty(changeSet.getCreateItems())) {
      this.doCreateItems(changeSet.getCreateItems(), namespace, operator, configChangeContentBuilder);
      auditService.audit("ItemSet", null, Audit.OP.INSERT, operator);
    }

    if (!CollectionUtils.isEmpty(changeSet.getUpdateItems())) {
      this.doUpdateItems(changeSet.getUpdateItems(), namespace, operator, configChangeContentBuilder);
      auditService.audit("ItemSet", null, Audit.OP.UPDATE, operator);
    }

    if (!CollectionUtils.isEmpty(changeSet.getDeleteItems())) {
      this.doDeleteItems(changeSet.getDeleteItems(), namespace, operator, configChangeContentBuilder);
      auditService.audit("ItemSet", null, Audit.OP.DELETE, operator);
    }

    if (configChangeContentBuilder.hasContent()) {
      commitService.createCommit(appId, clusterName, namespaceName, configChangeContentBuilder.build(),
                                 changeSet.getDataChangeLastModifiedBy());
    }

    return changeSet;
  }

  private void doDeleteItems(List<ItemDTO> toDeleteItems, Namespace namespace, String operator,
                             ConfigChangeContentBuilder configChangeContentBuilder) {

    for (ItemDTO item : toDeleteItems) {
      Item deletedItem = itemService.delete(item.getId(), operator);
      if (deletedItem.getNamespaceId() != namespace.getId()) {
        throw BadRequestException.namespaceNotMatch();
      }

      configChangeContentBuilder.deleteItem(deletedItem);
    }
  }

  private void doUpdateItems(List<ItemDTO> toUpdateItems, Namespace namespace, String operator,
                             ConfigChangeContentBuilder configChangeContentBuilder) {

    for (ItemDTO item : toUpdateItems) {
      Item entity = BeanUtils.transform(Item.class, item);

      Item managedItem = itemService.findOne(entity.getId());
      if (managedItem == null) {
        throw NotFoundException.itemNotFound(entity.getKey());
      }
      if (managedItem.getNamespaceId() != namespace.getId()) {
        throw BadRequestException.namespaceNotMatch();
      }
      Item beforeUpdateItem = BeanUtils.transform(Item.class, managedItem);

      //protect. only value,type,comment,lastModifiedBy can be modified
      managedItem.setType(entity.getType());
      managedItem.setValue(entity.getValue());
      managedItem.setComment(entity.getComment());
      managedItem.setLineNum(entity.getLineNum());
      managedItem.setDataChangeLastModifiedBy(operator);

      Item updatedItem = itemService.update(managedItem);
      configChangeContentBuilder.updateItem(beforeUpdateItem, updatedItem);
    }
  }

  private void doCreateItems(List<ItemDTO> toCreateItems, Namespace namespace, String operator,
                             ConfigChangeContentBuilder configChangeContentBuilder) {

    for (ItemDTO item : toCreateItems) {
      if (item.getNamespaceId() != namespace.getId()) {
        throw BadRequestException.namespaceNotMatch();
      }

      Item entity = BeanUtils.transform(Item.class, item);
      entity.setDataChangeCreatedBy(operator);
      entity.setDataChangeLastModifiedBy(operator);
      Item createdItem = itemService.save(entity);
      configChangeContentBuilder.createItem(createdItem);
    }
  }

}
