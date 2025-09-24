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
package com.ctrip.framework.apollo.openapi.util;

import com.ctrip.framework.apollo.common.dto.*;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.openapi.model.*;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseBO;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceSyncModel;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;
import com.ctrip.framework.apollo.portal.entity.vo.Organization;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiBeanUtils {

  private static final Gson GSON = new Gson();
  private static final Type TYPE = new TypeToken<Map<String, String>>() {}.getType();

  public static OpenItemDTO transformFromItemDTO(ItemDTO item) {
    Preconditions.checkArgument(item != null);
    return BeanUtils.transform(OpenItemDTO.class, item);
  }

  public static ItemDTO transformToItemDTO(OpenItemDTO openItemDTO) {
    Preconditions.checkArgument(openItemDTO != null);
    return BeanUtils.transform(ItemDTO.class, openItemDTO);
  }

  public static List<ItemDTO> transformToItemDTOs(List<OpenItemDTO> openItemDTOs) {
    if (CollectionUtils.isEmpty(openItemDTOs)) {
      return Collections.emptyList();
    }
    return openItemDTOs.stream()
        .map(OpenApiBeanUtils::transformToItemDTO)
        .collect(Collectors.toList());
  }

  public static List<OpenItemDTO> transformFromItemDTOs(List<ItemDTO> items) {
    if (CollectionUtils.isEmpty(items)) {
      return Collections.emptyList();
    }
    return items.stream()
        .map(OpenApiBeanUtils::transformFromItemDTO)
        .collect(Collectors.toList());
  }

  public static OpenAppNamespaceDTO transformToOpenAppNamespaceDTO(AppNamespace appNamespace) {
    Preconditions.checkArgument(appNamespace != null);
    return BeanUtils.transform(OpenAppNamespaceDTO.class, appNamespace);
  }

  public static AppNamespace transformToAppNamespace(OpenAppNamespaceDTO openAppNamespaceDTO) {
    Preconditions.checkArgument(openAppNamespaceDTO != null);
    return BeanUtils.transform(AppNamespace.class, openAppNamespaceDTO);
  }

  public static OpenReleaseDTO transformFromReleaseDTO(ReleaseDTO release) {
    Preconditions.checkArgument(release != null);

    OpenReleaseDTO openReleaseDTO = BeanUtils.transform(OpenReleaseDTO.class, release);

    Map<String, String> configs = GSON.fromJson(release.getConfigurations(), TYPE);

    openReleaseDTO.setConfigurations(configs);
    return openReleaseDTO;
  }

  public static OpenNamespaceDTO transformFromNamespaceBO(NamespaceBO namespaceBO) {
    Preconditions.checkArgument(namespaceBO != null);

    OpenNamespaceDTO openNamespaceDTO =
        BeanUtils.transform(OpenNamespaceDTO.class, namespaceBO.getBaseInfo());

    // app namespace info
    openNamespaceDTO.setFormat(namespaceBO.getFormat());
    openNamespaceDTO.setComment(namespaceBO.getComment());
    openNamespaceDTO.setIsPublic(namespaceBO.isPublic());

    // items
    List<OpenItemDTO> items = new LinkedList<>();
    List<ItemBO> itemBOs = namespaceBO.getItems();
    if (!CollectionUtils.isEmpty(itemBOs)) {
      items.addAll(itemBOs.stream().map(itemBO -> transformFromItemDTO(itemBO.getItem()))
              .collect(Collectors.toList()));
    }
    openNamespaceDTO.setItems(items);
    return openNamespaceDTO;

  }

  public static List<OpenNamespaceDTO> batchTransformFromNamespaceBOs(
      List<NamespaceBO> namespaceBOs) {
    if (CollectionUtils.isEmpty(namespaceBOs)) {
      return Collections.emptyList();
    }

    return namespaceBOs.stream()
            .map(OpenApiBeanUtils::transformFromNamespaceBO)
            .collect(Collectors.toCollection(LinkedList::new));
  }

  public static OpenNamespaceLockDTO transformFromNamespaceLockDTO(String namespaceName,
      NamespaceLockDTO namespaceLock) {
    OpenNamespaceLockDTO lock = new OpenNamespaceLockDTO();

    lock.setNamespaceName(namespaceName);

    if (namespaceLock == null) {
      lock.setIsLocked(false);
    } else {
      lock.setIsLocked(true);
      lock.setLockedBy(namespaceLock.getDataChangeCreatedBy());
    }

    return lock;
  }

  public static OpenNamespaceDTO transformFromNamespaceDTO(NamespaceDTO namespaceDTO) {
    Preconditions.checkArgument(namespaceDTO != null);
    
    return BeanUtils.transform(OpenNamespaceDTO.class, namespaceDTO);
  }

  public static OpenGrayReleaseRuleDTO transformFromGrayReleaseRuleDTO(
      GrayReleaseRuleDTO grayReleaseRuleDTO) {
    Preconditions.checkArgument(grayReleaseRuleDTO != null);

    return BeanUtils.transform(OpenGrayReleaseRuleDTO.class, grayReleaseRuleDTO);
  }

  public static GrayReleaseRuleDTO transformToGrayReleaseRuleDTO(
      OpenGrayReleaseRuleDTO openGrayReleaseRuleDTO) {
    Preconditions.checkArgument(openGrayReleaseRuleDTO != null);

    String appId = openGrayReleaseRuleDTO.getAppId();
    String branchName = openGrayReleaseRuleDTO.getBranchName();
    String clusterName = openGrayReleaseRuleDTO.getClusterName();
    String namespaceName = openGrayReleaseRuleDTO.getNamespaceName();

    GrayReleaseRuleDTO grayReleaseRuleDTO =
        new GrayReleaseRuleDTO(appId, clusterName, namespaceName, branchName);

    Set<OpenGrayReleaseRuleItemDTO> openGrayReleaseRuleItemDTOSet =
        new HashSet<>(openGrayReleaseRuleDTO.getRuleItems());
    openGrayReleaseRuleItemDTOSet.forEach(openGrayReleaseRuleItemDTO -> {
      String clientAppId = openGrayReleaseRuleItemDTO.getClientAppId();
      Set<String> clientIpList = new HashSet<>(openGrayReleaseRuleItemDTO.getClientIpList());
      Set<String> clientLabelList = new HashSet<>(openGrayReleaseRuleItemDTO.getClientLabelList());
      GrayReleaseRuleItemDTO ruleItem = new GrayReleaseRuleItemDTO(clientAppId, clientIpList, clientLabelList);
      grayReleaseRuleDTO.addRuleItem(ruleItem);
    });

    return grayReleaseRuleDTO;
  }

  public static List<OpenAppDTO> transformFromApps(final List<App> apps) {
    if (CollectionUtils.isEmpty(apps)) {
      return Collections.emptyList();
    }
    return apps.stream().map(OpenApiBeanUtils::transformFromApp).collect(Collectors.toList());
  }

  public static OpenAppDTO transformFromApp(final App app) {
    Preconditions.checkArgument(app != null);

    return BeanUtils.transform(OpenAppDTO.class, app);
  }

  public static OpenClusterDTO transformFromClusterDTO(ClusterDTO Cluster) {
    Preconditions.checkArgument(Cluster != null);
    return BeanUtils.transform(OpenClusterDTO.class, Cluster);
  }

  public static ClusterDTO transformToClusterDTO(OpenClusterDTO openClusterDTO) {
    Preconditions.checkArgument(openClusterDTO != null);
    return BeanUtils.transform(ClusterDTO.class, openClusterDTO);
  }

  public static OpenOrganizationDto transformFromOrganization(final Organization organization){
    Preconditions.checkArgument(organization != null);
    return BeanUtils.transform(OpenOrganizationDto.class, organization);
  }

  public static List<OpenOrganizationDto> transformFromOrganizations(final List<Organization> organizations){
    if (CollectionUtils.isEmpty(organizations)) {
      return Collections.emptyList();
    }
    return organizations.stream().map(OpenApiBeanUtils::transformFromOrganization).collect(Collectors.toList());
  }

  /**
   * 将InstanceDTO转换为OpenInstanceDTO
   */
  public static OpenInstanceDTO transformFromInstanceDTO(final InstanceDTO instanceDTO) {
    Preconditions.checkArgument(instanceDTO != null);
    return BeanUtils.transform(OpenInstanceDTO.class, instanceDTO);
  }

  /**
   * 批量转换InstanceDTO列表为OpenInstanceDTO列表
   */
  public static List<OpenInstanceDTO> transformFromInstanceDTOs(final List<InstanceDTO> instanceDTOs) {
    if (CollectionUtils.isEmpty(instanceDTOs)) {
      return Collections.emptyList();
    }
    return instanceDTOs.stream()
        .map(OpenApiBeanUtils::transformFromInstanceDTO)
        .collect(Collectors.toList());
  }

  /**
   * 将EnvClusterInfo转换为OpenEnvClusterInfo
   */
  public static OpenEnvClusterInfo transformFromEnvClusterInfo(final EnvClusterInfo envClusterInfo) {
    Preconditions.checkArgument(envClusterInfo != null);
    return BeanUtils.transform(OpenEnvClusterInfo.class, envClusterInfo);
  }

  /**
   * 批量转换EnvClusterInfo列表为OpenEnvClusterInfo列表
   */
  public static List<OpenEnvClusterInfo> transformFromEnvClusterInfos(final List<EnvClusterInfo> envClusterInfos) {
    if (CollectionUtils.isEmpty(envClusterInfos)) {
      return Collections.emptyList();
    }
    return envClusterInfos.stream()
            .map(OpenApiBeanUtils::transformFromEnvClusterInfo)
            .collect(Collectors.toList());
  }

  /**
   * 将OpenNamespaceTextModel转换为NamespaceTextModel
   */
  public static NamespaceTextModel transformToNamespaceTextModel(final OpenNamespaceTextModel openNamespaceTextModel) {
    Preconditions.checkArgument(openNamespaceTextModel != null);
    return BeanUtils.transform(NamespaceTextModel.class, openNamespaceTextModel);
  }

  /**
   * 批量转换OpenNamespaceTextModel列表为NamespaceTextModel列表
   */
  public static List<NamespaceTextModel> transformToNamespaceTextModels(final List<OpenNamespaceTextModel> openNamespaceTextModels) {
    if (CollectionUtils.isEmpty(openNamespaceTextModels)) {
      return Collections.emptyList();
    }
    return openNamespaceTextModels.stream()
            .map(OpenApiBeanUtils::transformToNamespaceTextModel)
            .collect(Collectors.toList());
  }

  /**
   * 将OpenNamespaceIdentifier转换为NamespaceIdentifier
   */
  public static NamespaceIdentifier transformToNamespaceIdentifier(final OpenNamespaceIdentifier openNamespaceIdentifier) {
    Preconditions.checkArgument(openNamespaceIdentifier != null);
    NamespaceIdentifier namespaceIdentifier = new NamespaceIdentifier();
    namespaceIdentifier.setAppId(openNamespaceIdentifier.getAppId());
    namespaceIdentifier.setEnv(openNamespaceIdentifier.getEnv());
    namespaceIdentifier.setClusterName(openNamespaceIdentifier.getClusterName());
    namespaceIdentifier.setNamespaceName(openNamespaceIdentifier.getNamespaceName());
    return namespaceIdentifier;
  }

  /**
   * 批量转换OpenNamespaceIdentifier列表为NamespaceIdentifier列表
   */
  public static List<NamespaceIdentifier> transformToNamespaceIdentifiers(final List<OpenNamespaceIdentifier> openNamespaceIdentifiers) {
    if (CollectionUtils.isEmpty(openNamespaceIdentifiers)) {
      return Collections.emptyList();
    }
    return openNamespaceIdentifiers.stream()
            .map(OpenApiBeanUtils::transformToNamespaceIdentifier)
            .collect(Collectors.toList());
  }

  /**
   * 将OpenNamespaceSyncModel转换为NamespaceSyncModel，同时转换其中的元素
   */
  public static NamespaceSyncModel transformToNamespaceSyncModel(final OpenNamespaceSyncModel openNamespaceSyncModel) {
    Preconditions.checkArgument(openNamespaceSyncModel != null);
    NamespaceSyncModel model = BeanUtils.transform(NamespaceSyncModel.class, openNamespaceSyncModel);
    
    // 转换同步到的命名空间列表
    if (openNamespaceSyncModel.getSyncToNamespaces() != null) {
      model.setSyncToNamespaces(transformToNamespaceIdentifiers(openNamespaceSyncModel.getSyncToNamespaces()));
    }
    
    // 转换同步项目
    if (openNamespaceSyncModel.getSyncItems() != null) {
      model.setSyncItems(transformToItemDTOs(openNamespaceSyncModel.getSyncItems()));
    }
    
    return model;
  }

  /**
   * 批量转换OpenNamespaceSyncModel列表为NamespaceSyncModel列表
   */
  public static List<NamespaceSyncModel> transformToNamespaceSyncModels(final List<OpenNamespaceSyncModel> openNamespaceSyncModels) {
    if (CollectionUtils.isEmpty(openNamespaceSyncModels)) {
      return Collections.emptyList();
    }
    return openNamespaceSyncModels.stream()
            .map(OpenApiBeanUtils::transformToNamespaceSyncModel)
            .collect(Collectors.toList());
  }

  /**
   * 将ItemDiffs转换为OpenItemDiffs
   */
  public static OpenItemDiffs transformFromItemDiffs(final ItemDiffs itemDiffs) {
    Preconditions.checkArgument(itemDiffs != null);
    OpenItemDiffs openItemDiffs = new OpenItemDiffs();
    
    // 转换命名空间标识符
    if (itemDiffs.getNamespace() != null) {
      openItemDiffs.setNamespace(transformFromNamespaceIdentifier(itemDiffs.getNamespace()));
    }
    
    // 转换差异项
    if (itemDiffs.getDiffs() != null) {
      openItemDiffs.setDiffs(transformFromItemChangeSets(itemDiffs.getDiffs()));
    }
    
    // 设置额外信息
    openItemDiffs.setExtInfo(itemDiffs.getExtInfo());
    
    return openItemDiffs;
  }
  
  /**
   * 批量转换ItemDiffs列表为OpenItemDiffs列表
   */
  public static List<OpenItemDiffs> transformFromItemDiffsList(final List<ItemDiffs> itemDiffsList) {
    if (CollectionUtils.isEmpty(itemDiffsList)) {
      return Collections.emptyList();
    }
    return itemDiffsList.stream()
            .map(OpenApiBeanUtils::transformFromItemDiffs)
            .collect(Collectors.toList());
  }
  
  /**
   * 将ItemChangeSets转换为OpenItemChangeSets
   */
  public static OpenItemChangeSets transformFromItemChangeSets(final ItemChangeSets itemChangeSets) {
    Preconditions.checkArgument(itemChangeSets != null);
    OpenItemChangeSets openItemChangeSets = new OpenItemChangeSets();
    
    // 转换新增项
    if (itemChangeSets.getCreateItems() != null) {
      openItemChangeSets.setCreateItems(transformFromItemDTOs(itemChangeSets.getCreateItems()));
    }
    
    // 转换更新项
    if (itemChangeSets.getUpdateItems() != null) {
      openItemChangeSets.setUpdateItems(transformFromItemDTOs(itemChangeSets.getUpdateItems()));
    }
    
    // 转换删除项
    if (itemChangeSets.getDeleteItems() != null) {
      openItemChangeSets.setDeleteItems(transformFromItemDTOs(itemChangeSets.getDeleteItems()));
    }
    
    return openItemChangeSets;
  }

  /**
   * 将NamespaceIdentifier转换为OpenNamespaceIdentifier
   */
  public static OpenNamespaceIdentifier transformFromNamespaceIdentifier(final NamespaceIdentifier namespaceIdentifier) {
    Preconditions.checkArgument(namespaceIdentifier != null);
    OpenNamespaceIdentifier openNamespaceIdentifier = new OpenNamespaceIdentifier();
    
    openNamespaceIdentifier.setAppId(namespaceIdentifier.getAppId());
    openNamespaceIdentifier.setEnv(namespaceIdentifier.getEnv().toString());
    openNamespaceIdentifier.setClusterName(namespaceIdentifier.getClusterName());
    openNamespaceIdentifier.setNamespaceName(namespaceIdentifier.getNamespaceName());
    
    return openNamespaceIdentifier;
  }


  /**
   * 将ReleaseBO转换为OpenReleaseBO
   */
  public static OpenReleaseBO transformFromReleaseBO(final ReleaseBO releaseBO) {
    Preconditions.checkArgument(releaseBO != null);
    OpenReleaseBO openReleaseBO = new OpenReleaseBO();

    openReleaseBO.setBaseInfo(transformFromReleaseDTO(releaseBO.getBaseInfo()));
    Set<com.ctrip.framework.apollo.portal.entity.bo.KVEntity> items = releaseBO.getItems();
    List<KVEntity> itemsList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(items)) {
      for (com.ctrip.framework.apollo.portal.entity.bo.KVEntity item : items) {
        KVEntity kvEntity = new KVEntity();
        kvEntity.setKey(item.getKey());
        kvEntity.setValue(item.getValue());
        itemsList.add(kvEntity);
      }
    }
    openReleaseBO.setItems(itemsList);
    return openReleaseBO;
  }
  
  /**
   * 批量转换ReleaseBO列表为OpenReleaseBO列表
   */
  public static List<OpenReleaseBO> transformFromReleaseBOs(final List<ReleaseBO> releaseBOs) {
    if (CollectionUtils.isEmpty(releaseBOs)) {
      return Collections.emptyList();
    }
    return releaseBOs.stream()
            .map(OpenApiBeanUtils::transformFromReleaseBO)
            .collect(Collectors.toList());
  }
}
