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
package com.ctrip.framework.apollo.adminservice.aop;


import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.service.ItemService;
import com.ctrip.framework.apollo.biz.service.NamespaceLockService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * unlock namespace if is redo operation.
 * --------------------------------------------
 * For example: If namespace has a item K1 = v1
 * --------------------------------------------
 * First operate: change k1 = v2 (lock namespace)
 * Second operate: change k1 = v1 (unlock namespace)
 */
@Aspect
@Component
public class NamespaceUnlockAspect {

  private static final Gson GSON = new Gson();

  private final NamespaceLockService namespaceLockService;
  private final NamespaceService namespaceService;
  private final ItemService itemService;
  private final ReleaseService releaseService;
  private final BizConfig bizConfig;

  public NamespaceUnlockAspect(
      final NamespaceLockService namespaceLockService,
      final NamespaceService namespaceService,
      final ItemService itemService,
      final ReleaseService releaseService,
      final BizConfig bizConfig) {
    this.namespaceLockService = namespaceLockService;
    this.namespaceService = namespaceService;
    this.itemService = itemService;
    this.releaseService = releaseService;
    this.bizConfig = bizConfig;
  }


  //create item
  @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, item, ..)")
  public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                ItemDTO item) {
    tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
  }

  //update item
  @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, itemId, item, ..)")
  public void requireLockAdvice(String appId, String clusterName, String namespaceName, long itemId,
                                ItemDTO item) {
    tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
  }

  //update by change set
  @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, changeSet, ..)")
  public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                ItemChangeSets changeSet) {
    tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
  }

  //delete item
  @After("@annotation(PreAcquireNamespaceLock) && args(itemId, operator, ..)")
  public void requireLockAdvice(long itemId, String operator) {
    Item item = itemService.findOne(itemId);
    if (item == null) {
      throw BadRequestException.itemNotExists(itemId);
    }
    tryUnlock(namespaceService.findOne(item.getNamespaceId()));
  }

  private void tryUnlock(Namespace namespace) {
    if (bizConfig.isNamespaceLockSwitchOff()) {
      return;
    }

    if (!isModified(namespace)) {
      namespaceLockService.unlock(namespace.getId());
    }

  }

  boolean isModified(Namespace namespace) {
    Release release = releaseService.findLatestActiveRelease(namespace);
    List<Item> items = itemService.findItemsWithoutOrdered(namespace.getId());

    if (release == null) {
      return hasNormalItems(items);
    }

    Map<String, String> releasedConfiguration = GSON.fromJson(release.getConfigurations(), GsonType.CONFIG);
    Map<String, String> configurationFromItems = generateConfigurationFromItems(namespace, items);

    MapDifference<String, String> difference = Maps.difference(releasedConfiguration, configurationFromItems);

    return !difference.areEqual();

  }

  private boolean hasNormalItems(List<Item> items) {
    for (Item item : items) {
      if (!StringUtils.isEmpty(item.getKey())) {
        return true;
      }
    }

    return false;
  }

  private Map<String, String> generateConfigurationFromItems(Namespace namespace, List<Item> namespaceItems) {

    Map<String, String> configurationFromItems = Maps.newHashMap();

    Namespace parentNamespace = namespaceService.findParentNamespace(namespace);
    //parent namespace
    if (parentNamespace == null) {
      generateMapFromItems(namespaceItems, configurationFromItems);
    } else {//child namespace
      Release parentRelease = releaseService.findLatestActiveRelease(parentNamespace);
      if (parentRelease != null) {
        configurationFromItems = GSON.fromJson(parentRelease.getConfigurations(), GsonType.CONFIG);
      }
      generateMapFromItems(namespaceItems, configurationFromItems);
    }

    return configurationFromItems;
  }

  private Map<String, String> generateMapFromItems(List<Item> items, Map<String, String> configurationFromItems) {
    for (Item item : items) {
      String key = item.getKey();
      if (StringUtils.isBlank(key)) {
        continue;
      }
      configurationFromItems.put(key, item.getValue());
    }

    return configurationFromItems;
  }

}
