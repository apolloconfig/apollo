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
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.NamespaceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.RoleType;
import com.ctrip.framework.apollo.portal.constant.TracerEventType;
import com.ctrip.framework.apollo.portal.enricher.adapter.BaseDtoUserInfoEnrichedAdapter;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceUsage;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NamespaceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceService.class);
  private static final Gson GSON = new Gson();
  private static final ExecutorService executorService = Executors.newFixedThreadPool(
      Runtime.getRuntime().availableProcessors() * 2
      , ApolloThreadFactory.create("NamespaceService", true));

  private final PortalConfig portalConfig;
  private final PortalSettings portalSettings;
  private final UserInfoHolder userInfoHolder;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;
  private final ItemService itemService;
  private final ReleaseService releaseService;
  private final AppNamespaceService appNamespaceService;
  private final InstanceService instanceService;
  private final NamespaceBranchService branchService;
  private final RolePermissionService rolePermissionService;
  private final AdditionalUserInfoEnrichService additionalUserInfoEnrichService;
  private final ClusterService clusterService;

  public NamespaceService(
      final PortalConfig portalConfig,
      final PortalSettings portalSettings,
      final UserInfoHolder userInfoHolder,
      final NamespaceAPI namespaceAPI,
      final ItemService itemService,
      final ReleaseService releaseService,
      final AppNamespaceService appNamespaceService,
      final InstanceService instanceService,
      final @Lazy NamespaceBranchService branchService,
      final RolePermissionService rolePermissionService,
      final AdditionalUserInfoEnrichService additionalUserInfoEnrichService,
      ClusterService clusterService) {
    this.portalConfig = portalConfig;
    this.portalSettings = portalSettings;
    this.userInfoHolder = userInfoHolder;
    this.namespaceAPI = namespaceAPI;
    this.itemService = itemService;
    this.releaseService = releaseService;
    this.appNamespaceService = appNamespaceService;
    this.instanceService = instanceService;
    this.branchService = branchService;
    this.rolePermissionService = rolePermissionService;
    this.additionalUserInfoEnrichService = additionalUserInfoEnrichService;
    this.clusterService = clusterService;
  }


  public NamespaceDTO createNamespace(Env env, NamespaceDTO namespace) {
    if (StringUtils.isEmpty(namespace.getDataChangeCreatedBy())) {
      namespace.setDataChangeCreatedBy(userInfoHolder.getUser().getUserId());
    }

    if (StringUtils.isEmpty(namespace.getDataChangeLastModifiedBy())) {
      namespace.setDataChangeLastModifiedBy(userInfoHolder.getUser().getUserId());
    }
    NamespaceDTO createdNamespace = namespaceAPI.createNamespace(env, namespace);

    Tracer.logEvent(TracerEventType.CREATE_NAMESPACE,
        String.format("%s+%s+%s+%s", namespace.getAppId(), env, namespace.getClusterName(),
            namespace.getNamespaceName()));
    return createdNamespace;
  }


  public List<NamespaceUsage> getNamespaceUsageByAppId(String appId, String namespaceName) {
    List<Env> envs = portalSettings.getActiveEnvs();
    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);
    List<NamespaceUsage> usages = new ArrayList<>();
    for (Env env : envs) {
      List<ClusterDTO> clusters = clusterService.findClusters(env, appId);
      for (ClusterDTO cluster : clusters) {
        String clusterName = cluster.getName();
        NamespaceUsage usage = this.getNamespaceUsageByEnv(appId, namespaceName, env, clusterName);
        if (appNamespace != null && appNamespace.isPublic()) {
          int associatedNamespace = this.getPublicAppNamespaceHasAssociatedNamespace(namespaceName, env);
          usage.setLinkedNamespaceCount(associatedNamespace);
        }

        if(usage.getLinkedNamespaceCount() > 0 || usage.getBranchInstanceCount() > 0 || usage.getInstanceCount() > 0) {
          usages.add(usage);
        }
      }
    }
    return usages;
  }

  public NamespaceUsage getNamespaceUsageByEnv(String appId, String namespaceName, Env env, String clusterName) {
    NamespaceUsage namespaceUsage = new NamespaceUsage(namespaceName, appId, clusterName, env.getName());
    int instanceCount = instanceService.getInstanceCountByNamespace(appId, env, clusterName, namespaceName);
    namespaceUsage.setInstanceCount(instanceCount);

    NamespaceDTO branchNamespace = branchService.findBranchBaseInfo(appId, env, clusterName, namespaceName);
    if(branchNamespace != null){
      String branchClusterName = branchNamespace.getClusterName();
      int branchInstanceCount = instanceService.getInstanceCountByNamespace(appId, env, branchClusterName, namespaceName);
      namespaceUsage.setBranchInstanceCount(branchInstanceCount);
    }
    return namespaceUsage;
  }

  @Transactional
  public void deleteNamespace(String appId, Env env, String clusterName, String namespaceName) {

    String operator = userInfoHolder.getUser().getUserId();
    namespaceAPI.deleteNamespace(env, appId, clusterName, namespaceName, operator);
  }

  public NamespaceDTO loadNamespaceBaseInfo(String appId, Env env, String clusterName,
      String namespaceName) {
    NamespaceDTO namespace = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    if (namespace == null) {
      throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    }
    return namespace;
  }

  /**
   * load cluster all namespace info with items
   */
  public List<NamespaceBO> findNamespaceBOs(String appId, Env env, String clusterName, boolean fillItemDetail, boolean includeDeletedItems) {

    List<NamespaceDTO> namespaces = namespaceAPI.findNamespaceByCluster(appId, env, clusterName);
    if (namespaces == null || namespaces.size() == 0) {
      throw BadRequestException.namespaceNotExists();
    }

    List<NamespaceBO> namespaceBOs = Collections.synchronizedList(new LinkedList<>());
    List<String> exceptionNamespaces = Collections.synchronizedList(new LinkedList<>());
    CountDownLatch latch = new CountDownLatch(namespaces.size());
    for (NamespaceDTO namespace : namespaces) {
      executorService.submit(() -> {
        NamespaceBO namespaceBO;
        try {
          namespaceBO = transformNamespace2BO(env, namespace, fillItemDetail, includeDeletedItems);
          namespaceBOs.add(namespaceBO);
        } catch (Exception e) {
          LOGGER.error("parse namespace error. app id:{}, env:{}, clusterName:{}, namespace:{}",
              appId, env, clusterName, namespace.getNamespaceName(), e);
          exceptionNamespaces.add(namespace.getNamespaceName());
        } finally {
          latch.countDown();
        }
      });

    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      //ignore
    }

    if(namespaceBOs.size() != namespaces.size()){
       throw new RuntimeException(String
           .format("Parse namespaces error, expected: %s, but actual: %s, cannot get those namespaces: %s", namespaces.size(), namespaceBOs.size(), exceptionNamespaces));
    }

    return namespaceBOs.stream()
        .sorted(Comparator.comparing(o -> o.getBaseInfo().getId()))
        .collect(Collectors.toList());
  }

  public List<NamespaceBO> findNamespaceBOs(String appId, Env env, String clusterName) {
    return findNamespaceBOs(appId, env, clusterName, true, true);
  }

  public List<NamespaceDTO> findNamespaces(String appId, Env env, String clusterName) {
    return namespaceAPI.findNamespaceByCluster(appId, env, clusterName);
  }

  /**
   * the returned content's size is not fixed. so please carefully used.
   */
  public PageDTO<NamespaceDTO> findNamespacesByItem(Env env, String itemKey, Pageable pageable) {
    return namespaceAPI.findByItem(env, itemKey, pageable.getPageNumber(), pageable.getPageSize());
  }

  public List<NamespaceDTO> getPublicAppNamespaceAllNamespaces(Env env, String publicNamespaceName,
      int page,
      int size) {
    return namespaceAPI.getPublicAppNamespaceAllNamespaces(env, publicNamespaceName, page, size);
  }

  public NamespaceBO loadNamespaceBO(String appId, Env env, String clusterName,
      String namespaceName, boolean fillItemDetail, boolean includeDeletedItems) {
    NamespaceDTO namespace = namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName);
    if (namespace == null) {
      throw BadRequestException.namespaceNotExists(appId, clusterName, namespaceName);
    }
    return transformNamespace2BO(env, namespace, fillItemDetail, includeDeletedItems);
  }

  public NamespaceBO loadNamespaceBO(String appId, Env env, String clusterName,
      String namespaceName) {
    return loadNamespaceBO(appId, env, clusterName, namespaceName, true, true);
  }

  public boolean publicAppNamespaceHasAssociatedNamespace(String publicNamespaceName, Env env) {
    return getPublicAppNamespaceHasAssociatedNamespace(publicNamespaceName, env) > 0;
  }

  public int getPublicAppNamespaceHasAssociatedNamespace(String publicNamespaceName, Env env) {
    return namespaceAPI.countPublicAppNamespaceAssociatedNamespaces(env, publicNamespaceName);
  }

  public NamespaceBO findPublicNamespaceForAssociatedNamespace(Env env, String appId,
      String clusterName, String namespaceName) {
    NamespaceDTO namespace =
        namespaceAPI
            .findPublicNamespaceForAssociatedNamespace(env, appId, clusterName, namespaceName);

    return transformNamespace2BO(env, namespace);
  }

  public Map<String, Map<String, Boolean>> getNamespacesPublishInfo(String appId) {
    Map<String, Map<String, Boolean>> result = Maps.newHashMap();

    Set<Env> envs = portalConfig.publishTipsSupportedEnvs();
    for (Env env : envs) {
      if (portalSettings.isEnvActive(env)) {
        result.put(env.toString(), namespaceAPI.getNamespacePublishInfo(env, appId));
      }
    }

    return result;
  }

  private NamespaceBO transformNamespace2BO(Env env, NamespaceDTO namespace, boolean fillItemDetail, boolean includeDeletedItems) {
    NamespaceBO namespaceBO = new NamespaceBO();
    namespaceBO.setBaseInfo(namespace);

    String appId = namespace.getAppId();
    String clusterName = namespace.getClusterName();
    String namespaceName = namespace.getNamespaceName();

    fillAppNamespaceProperties(namespaceBO);

    List<ItemBO> itemBOs = new LinkedList<>();
    namespaceBO.setItems(itemBOs);

    if (!fillItemDetail) {
      return namespaceBO;
    }

    //latest Release
    ReleaseDTO latestRelease;
    Map<String, String> releaseItems = new HashMap<>();
    latestRelease = releaseService.loadLatestRelease(appId, env, clusterName, namespaceName);
    if (latestRelease != null) {
      releaseItems = GSON.fromJson(latestRelease.getConfigurations(), GsonType.CONFIG);
    }

    //not Release config items
    List<ItemDTO> items = itemService.findItems(appId, env, clusterName, namespaceName);
    additionalUserInfoEnrichService
        .enrichAdditionalUserInfo(items, BaseDtoUserInfoEnrichedAdapter::new);
    int modifiedItemCnt = 0;
    for (ItemDTO itemDTO : items) {

      ItemBO itemBO = transformItem2BO(itemDTO, releaseItems);

      if (itemBO.isModified()) {
        modifiedItemCnt++;
      }

      itemBOs.add(itemBO);
    }

    if (includeDeletedItems) {
      //deleted items
      Map<String, ItemDTO> deletedItemDTOs = itemService.findDeletedItems(appId, env, clusterName, namespaceName).stream()
              .filter(itemDTO -> !StringUtils.isEmpty(itemDTO.getKey()))
              .collect(Collectors.toMap(itemDTO -> itemDTO.getKey(), v -> v, (v1, v2) -> v2));

      List<ItemBO> deletedItems = parseDeletedItems(items, releaseItems, deletedItemDTOs);
      itemBOs.addAll(deletedItems);
      modifiedItemCnt += deletedItems.size();
    }

    namespaceBO.setItemModifiedCnt(modifiedItemCnt);

    return namespaceBO;
  }

  private NamespaceBO transformNamespace2BO(Env env, NamespaceDTO namespace) {
    return transformNamespace2BO(env, namespace, true, true);
  }

  private void fillAppNamespaceProperties(NamespaceBO namespace) {

    final NamespaceDTO namespaceDTO = namespace.getBaseInfo();
    final String appId = namespaceDTO.getAppId();
    final String clusterName = namespaceDTO.getClusterName();
    final String namespaceName = namespaceDTO.getNamespaceName();
    //先从当前appId下面找,包含私有的和公共的
    AppNamespace appNamespace =
        appNamespaceService
            .findByAppIdAndName(appId, namespaceName);
    //再从公共的app namespace里面找
    if (appNamespace == null) {
      appNamespace = appNamespaceService.findPublicAppNamespace(namespaceName);
    }

    final String format;
    final boolean isPublic;
    if (appNamespace == null) {
      //dirty data
      LOGGER.warn("Dirty data, cannot find appNamespace by namespaceName [{}], appId = {}, cluster = {}, set it format to {}, make public", namespaceName, appId, clusterName, ConfigFileFormat.Properties.getValue());
      format = ConfigFileFormat.Properties.getValue();
      isPublic = true; // set to true, because public namespace allowed to delete by user
    } else {
      format = appNamespace.getFormat();
      isPublic = appNamespace.isPublic();
      namespace.setParentAppId(appNamespace.getAppId());
      namespace.setComment(appNamespace.getComment());
    }
    namespace.setFormat(format);
    namespace.setPublic(isPublic);
  }

  private List<ItemBO> parseDeletedItems(List<ItemDTO> newItems, Map<String, String> releaseItems, Map<String, ItemDTO> deletedItemDTOs) {
    Map<String, ItemDTO> newItemMap = BeanUtils.mapByKey("key", newItems);
    //remove comment and blank item map.
    newItemMap.remove("");

    List<ItemBO> deletedItems = new LinkedList<>();
    for (Map.Entry<String, String> entry : releaseItems.entrySet()) {
      String key = entry.getKey();
      if (newItemMap.get(key) == null) {
        ItemBO deletedItem = new ItemBO();

        deletedItem.setDeleted(true);
        ItemDTO deletedItemDto = deletedItemDTOs.computeIfAbsent(key, k -> new ItemDTO());
        deletedItemDto.setKey(key);
        String oldValue = entry.getValue();
        deletedItem.setItem(deletedItemDto);

        deletedItemDto.setValue(oldValue);
        deletedItem.setModified(true);
        deletedItem.setOldValue(oldValue);
        deletedItem.setNewValue("");
        deletedItems.add(deletedItem);
      }
    }
    return deletedItems;
  }

  private ItemBO transformItem2BO(ItemDTO itemDTO, Map<String, String> releaseItems) {
    String key = itemDTO.getKey();
    ItemBO itemBO = new ItemBO();
    itemBO.setItem(itemDTO);
    String newValue = itemDTO.getValue();
    String oldValue = releaseItems.get(key);
    //new item or modified
    if (!StringUtils.isEmpty(key) && (!newValue.equals(oldValue))) {
      itemBO.setModified(true);
      itemBO.setOldValue(oldValue == null ? "" : oldValue);
      itemBO.setNewValue(newValue);
    }
    return itemBO;
  }

  public void assignNamespaceRoleToOperator(String appId, String namespaceName, String operator) {
    //default assign modify、release namespace role to namespace creator

    rolePermissionService
        .assignRoleToUsers(
            RoleUtils.buildNamespaceRoleName(appId, namespaceName, RoleType.MODIFY_NAMESPACE),
            Sets.newHashSet(operator), operator);
    rolePermissionService
        .assignRoleToUsers(
            RoleUtils.buildNamespaceRoleName(appId, namespaceName, RoleType.RELEASE_NAMESPACE),
            Sets.newHashSet(operator), operator);
  }
}
