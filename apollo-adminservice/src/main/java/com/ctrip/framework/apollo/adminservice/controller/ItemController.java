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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.adminservice.aop.PreAcquireNamespaceLock;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.service.CommitService;
import com.ctrip.framework.apollo.biz.service.ItemService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.biz.utils.ConfigChangeContentBuilder;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.ItemInfoDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ItemController {

  private final ItemService itemService;
  private final NamespaceService namespaceService;
  private final CommitService commitService;
  private final ReleaseService releaseService;
  private final BizConfig bizConfig;

  public ItemController(final ItemService itemService, final NamespaceService namespaceService, final CommitService commitService, final ReleaseService releaseService, final BizConfig bizConfig) {
    this.itemService = itemService;
    this.namespaceService = namespaceService;
    this.commitService = commitService;
    this.releaseService = releaseService;
    this.bizConfig = bizConfig;
  }

  @PreAcquireNamespaceLock
  @PostMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public ItemDTO create(@PathVariable("appId") String appId,
                        @PathVariable("clusterName") String clusterName,
                        @PathVariable("namespaceName") String namespaceName, @RequestBody ItemDTO dto) {
    Item entity = BeanUtils.transform(Item.class, dto);

    Item managedEntity = itemService.findOne(appId, clusterName, namespaceName, entity.getKey());
    if (managedEntity != null) {
      throw BadRequestException.itemAlreadyExists(entity.getKey());
    }

    if (bizConfig.isItemNumLimitEnabled()) {
      int itemCount = itemService.findNonEmptyItemCount(entity.getNamespaceId());
      if (itemCount >= bizConfig.itemNumLimit()) {
        throw new BadRequestException("The maximum number of items (" + bizConfig.itemNumLimit() + ") for this namespace has been reached. Current item count is " + itemCount + ".");
      }
    }

    entity = itemService.save(entity);
    dto = BeanUtils.transform(ItemDTO.class, entity);
    commitService.createCommit(appId, clusterName, namespaceName, new ConfigChangeContentBuilder().createItem(entity).build(),
        dto.getDataChangeLastModifiedBy()
    );

    return dto;
  }

  @PostMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/comment_items")
  public ItemDTO createComment(@PathVariable("appId") String appId,
                        @PathVariable("clusterName") String clusterName,
                        @PathVariable("namespaceName") String namespaceName, @RequestBody ItemDTO dto) {
    if (!StringUtils.isBlank(dto.getKey()) || !StringUtils.isBlank(dto.getValue())) {
      throw new BadRequestException("Comment item's key or value should be blank.");
    }
    if (StringUtils.isBlank(dto.getComment())) {
      throw new BadRequestException("Comment item's comment should not be blank.");
    }

    // check if comment existed
    List<Item> allItems = itemService.findItemsWithOrdered(appId, clusterName, namespaceName);
    for (Item item : allItems) {
      if (StringUtils.isBlank(item.getKey()) && StringUtils.isBlank(item.getValue()) &&
          Objects.equals(item.getComment(), dto.getComment())) {
        return BeanUtils.transform(ItemDTO.class, item);
      }
    }

    Item entity = BeanUtils.transform(Item.class, dto);
    entity = itemService.saveComment(entity);

    return BeanUtils.transform(ItemDTO.class, entity);
  }


  @PreAcquireNamespaceLock
  @PutMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{itemId}")
  public ItemDTO update(@PathVariable("appId") String appId,
                        @PathVariable("clusterName") String clusterName,
                        @PathVariable("namespaceName") String namespaceName,
                        @PathVariable("itemId") long itemId,
                        @RequestBody ItemDTO itemDTO) {
    Item managedEntity = itemService.findOne(itemId);
    if (managedEntity == null) {
      throw NotFoundException.itemNotFound(appId, clusterName, namespaceName, itemId);
    }

    Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
    // In case someone constructs an attack scenario
    if (namespace == null || namespace.getId() != managedEntity.getNamespaceId()) {
      throw BadRequestException.namespaceNotMatch();
    }

    Item entity = BeanUtils.transform(Item.class, itemDTO);

    ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();

    Item beforeUpdateItem = BeanUtils.transform(Item.class, managedEntity);

    //protect. only value,type,comment,lastModifiedBy can be modified
    managedEntity.setType(entity.getType());
    managedEntity.setValue(entity.getValue());
    managedEntity.setComment(entity.getComment());
    managedEntity.setDataChangeLastModifiedBy(entity.getDataChangeLastModifiedBy());

    entity = itemService.update(managedEntity);
    builder.updateItem(beforeUpdateItem, entity);
    itemDTO = BeanUtils.transform(ItemDTO.class, entity);

    if (builder.hasContent()) {
      commitService.createCommit(appId, clusterName, namespaceName, builder.build(), itemDTO.getDataChangeLastModifiedBy());
    }

    return itemDTO;
  }

  @PreAcquireNamespaceLock
  @DeleteMapping("/items/{itemId}")
  public void delete(@PathVariable("itemId") long itemId, @RequestParam String operator) {
    Item entity = itemService.findOne(itemId);
    if (entity == null) {
      throw NotFoundException.itemNotFound(itemId);
    }
    itemService.delete(entity.getId(), operator);

    Namespace namespace = namespaceService.findOne(entity.getNamespaceId());

    commitService.createCommit(namespace.getAppId(), namespace.getClusterName(), namespace.getNamespaceName(),
        new ConfigChangeContentBuilder().deleteItem(entity).build(), operator
    );

  }

  @GetMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items")
  public List<ItemDTO> findItems(@PathVariable("appId") String appId,
                                 @PathVariable("clusterName") String clusterName,
                                 @PathVariable("namespaceName") String namespaceName) {
    return BeanUtils.batchTransform(ItemDTO.class, itemService.findItemsWithOrdered(appId, clusterName, namespaceName));
  }

  @GetMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/deleted")
  public List<ItemDTO> findDeletedItems(@PathVariable("appId") String appId,
                                        @PathVariable("clusterName") String clusterName,
                                        @PathVariable("namespaceName") String namespaceName) {
    //get latest release time
    Release latestActiveRelease = releaseService.findLatestActiveRelease(appId, clusterName, namespaceName);
    List<Commit> commits;
    if (Objects.nonNull(latestActiveRelease)) {
      commits = commitService.find(appId, clusterName, namespaceName, latestActiveRelease.getDataChangeCreatedTime(), null);
    } else {
      commits = commitService.find(appId, clusterName, namespaceName, null);
    }

    if (Objects.nonNull(commits)) {
      List<Item> deletedItems = commits.stream()
          .map(item -> ConfigChangeContentBuilder.convertJsonString(item.getChangeSets()).getDeleteItems())
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
      return BeanUtils.batchTransform(ItemDTO.class, deletedItems);
    }
    return Collections.emptyList();
  }

  @GetMapping("/items-search/key-and-value")
  public PageDTO<ItemInfoDTO> getItemInfoBySearch(@RequestParam(value = "key", required = false) String key,
                                                  @RequestParam(value = "value", required = false) String value,
                                                  Pageable limit) {
    Page<ItemInfoDTO> pageItemInfoDTO = itemService.getItemInfoBySearch(key, value, limit);
    return new PageDTO<>(pageItemInfoDTO.getContent(), limit, pageItemInfoDTO.getTotalElements());
  }

  @GetMapping("/items/{itemId}")
  public ItemDTO get(@PathVariable("itemId") long itemId) {
    Item item = itemService.findOne(itemId);
    if (item == null) {
      throw NotFoundException.itemNotFound(itemId);
    }
    return BeanUtils.transform(ItemDTO.class, item);
  }

  @GetMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}")
  public ItemDTO get(@PathVariable("appId") String appId,
      @PathVariable("clusterName") String clusterName,
      @PathVariable("namespaceName") String namespaceName, @PathVariable("key") String key) {
    Item item = itemService.findOne(appId, clusterName, namespaceName, key);
    if (item == null) {
      throw NotFoundException.itemNotFound(appId, clusterName, namespaceName, key);
    }
    return BeanUtils.transform(ItemDTO.class, item);
  }

  @GetMapping("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/encodedItems/{key:.+}")
  public ItemDTO getByEncodedKey(@PathVariable("appId") String appId,
      @PathVariable("clusterName") String clusterName,
      @PathVariable("namespaceName") String namespaceName, @PathVariable("key") String key) {
    return this.get(appId, clusterName, namespaceName,
        new String(Base64.getUrlDecoder().decode(key.getBytes(StandardCharsets.UTF_8))));
  }

  @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items-with-page")
  public PageDTO<ItemDTO> findItemsByNamespace(@PathVariable("appId") String appId,
                                               @PathVariable("clusterName") String clusterName,
                                               @PathVariable("namespaceName") String namespaceName,
                                               Pageable pageable) {
    Page<Item> itemPage = itemService.findItemsByNamespace(appId, clusterName, namespaceName, pageable);

    List<ItemDTO> itemDTOS = BeanUtils.batchTransform(ItemDTO.class, itemPage.getContent());
    return new PageDTO<>(itemDTOS, pageable, itemPage.getTotalElements());
  }

}
