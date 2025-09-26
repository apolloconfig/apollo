/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * Non-invasive converters for OpenAPI generated model classes.
 * This class mirrors/OpenApiBeanUtils functions but targets com.ctrip.framework.apollo.openapi.model.* types.
 */
public final class OpenApiModelConverters {

  private static final Gson GSON = new Gson();
  private static final Type TYPE = new TypeToken<Map<String, String>>() {}.getType();

  private OpenApiModelConverters() {}

  // region Item conversions
  public static OpenItemDTO fromItemDTO(ItemDTO item) {
    Preconditions.checkArgument(item != null);
    return BeanUtils.transform(OpenItemDTO.class, item);
  }

  public static ItemDTO toItemDTO(OpenItemDTO openItemDTO) {
    Preconditions.checkArgument(openItemDTO != null);
    return BeanUtils.transform(ItemDTO.class, openItemDTO);
  }

  public static List<ItemDTO> toItemDTOs(List<OpenItemDTO> openItemDTOs) {
    if (CollectionUtils.isEmpty(openItemDTOs)) {
      return Collections.emptyList();
    }
    return openItemDTOs.stream().map(OpenApiModelConverters::toItemDTO).collect(Collectors.toList());
  }

  public static List<OpenItemDTO> fromItemDTOs(List<ItemDTO> items) {
    if (CollectionUtils.isEmpty(items)) {
      return Collections.emptyList();
    }
    return items.stream().map(OpenApiModelConverters::fromItemDTO).collect(Collectors.toList());
  }
  // endregion

  // region App/AppNamespace conversions
  public static OpenAppNamespaceDTO fromAppNamespace(AppNamespace appNamespace) {
    Preconditions.checkArgument(appNamespace != null);
    return BeanUtils.transform(OpenAppNamespaceDTO.class, appNamespace);
  }

  public static AppNamespace toAppNamespace(OpenAppNamespaceDTO openAppNamespaceDTO) {
    Preconditions.checkArgument(openAppNamespaceDTO != null);
    return BeanUtils.transform(AppNamespace.class, openAppNamespaceDTO);
  }

  public static List<OpenAppDTO> fromApps(final List<App> apps) {
    if (CollectionUtils.isEmpty(apps)) {
      return Collections.emptyList();
    }
    return apps.stream().map(OpenApiModelConverters::fromApp).collect(Collectors.toList());
  }

  public static OpenAppDTO fromApp(final App app) {
    Preconditions.checkArgument(app != null);
    return BeanUtils.transform(OpenAppDTO.class, app);
  }
  // endregion

  // region Release conversions
  public static OpenReleaseDTO fromReleaseDTO(ReleaseDTO release) {
    Preconditions.checkArgument(release != null);
    OpenReleaseDTO openReleaseDTO = BeanUtils.transform(OpenReleaseDTO.class, release);
    Map<String, String> configs = GSON.fromJson(release.getConfigurations(), TYPE);
    openReleaseDTO.setConfigurations(configs);
    return openReleaseDTO;
  }

  public static OpenReleaseBO fromReleaseBO(final ReleaseBO releaseBO) {
    Preconditions.checkArgument(releaseBO != null);
    OpenReleaseBO openReleaseBO = new OpenReleaseBO();
    openReleaseBO.setBaseInfo(fromReleaseDTO(releaseBO.getBaseInfo()));
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

  public static List<OpenReleaseBO> fromReleaseBOs(final List<ReleaseBO> releaseBOs) {
    if (CollectionUtils.isEmpty(releaseBOs)) {
      return Collections.emptyList();
    }
    return releaseBOs.stream().map(OpenApiModelConverters::fromReleaseBO).collect(Collectors.toList());
  }
  // endregion

  // region Namespace conversions
  public static OpenNamespaceDTO fromNamespaceBO(NamespaceBO namespaceBO) {
    Preconditions.checkArgument(namespaceBO != null);
    OpenNamespaceDTO openNamespaceDTO = BeanUtils.transform(OpenNamespaceDTO.class, namespaceBO.getBaseInfo());
    openNamespaceDTO.setFormat(namespaceBO.getFormat());
    openNamespaceDTO.setComment(namespaceBO.getComment());
    openNamespaceDTO.setIsPublic(namespaceBO.isPublic());
    List<OpenItemDTO> items = new LinkedList<>();
    List<ItemBO> itemBOs = namespaceBO.getItems();
    if (!CollectionUtils.isEmpty(itemBOs)) {
      items.addAll(itemBOs.stream().map(itemBO -> fromItemDTO(itemBO.getItem())).collect(Collectors.toList()));
    }
    openNamespaceDTO.setItems(items);
    return openNamespaceDTO;
  }

  public static List<OpenNamespaceDTO> fromNamespaceBOs(List<NamespaceBO> namespaceBOs) {
    if (CollectionUtils.isEmpty(namespaceBOs)) {
      return Collections.emptyList();
    }
    return namespaceBOs.stream().map(OpenApiModelConverters::fromNamespaceBO).collect(Collectors.toCollection(LinkedList::new));
  }

  public static OpenNamespaceLockDTO fromNamespaceLockDTO(String namespaceName, NamespaceLockDTO namespaceLock) {
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

  public static OpenNamespaceDTO fromNamespaceDTO(NamespaceDTO namespaceDTO) {
    Preconditions.checkArgument(namespaceDTO != null);
    return BeanUtils.transform(OpenNamespaceDTO.class, namespaceDTO);
  }

  public static NamespaceTextModel toNamespaceTextModel(final OpenNamespaceTextModel openNamespaceTextModel) {
    Preconditions.checkArgument(openNamespaceTextModel != null);
    return BeanUtils.transform(NamespaceTextModel.class, openNamespaceTextModel);
  }

  public static List<NamespaceTextModel> toNamespaceTextModels(final List<OpenNamespaceTextModel> openNamespaceTextModels) {
    if (CollectionUtils.isEmpty(openNamespaceTextModels)) {
      return Collections.emptyList();
    }
    return openNamespaceTextModels.stream().map(OpenApiModelConverters::toNamespaceTextModel).collect(Collectors.toList());
  }

  public static NamespaceIdentifier toNamespaceIdentifier(final OpenNamespaceIdentifier openNamespaceIdentifier) {
    Preconditions.checkArgument(openNamespaceIdentifier != null);
    NamespaceIdentifier namespaceIdentifier = new NamespaceIdentifier();
    namespaceIdentifier.setAppId(openNamespaceIdentifier.getAppId());
    namespaceIdentifier.setEnv(openNamespaceIdentifier.getEnv());
    namespaceIdentifier.setClusterName(openNamespaceIdentifier.getClusterName());
    namespaceIdentifier.setNamespaceName(openNamespaceIdentifier.getNamespaceName());
    return namespaceIdentifier;
  }

  public static List<NamespaceIdentifier> toNamespaceIdentifiers(final List<OpenNamespaceIdentifier> openNamespaceIdentifiers) {
    if (CollectionUtils.isEmpty(openNamespaceIdentifiers)) {
      return Collections.emptyList();
    }
    return openNamespaceIdentifiers.stream().map(OpenApiModelConverters::toNamespaceIdentifier).collect(Collectors.toList());
  }

  public static OpenNamespaceIdentifier fromNamespaceIdentifier(final NamespaceIdentifier namespaceIdentifier) {
    Preconditions.checkArgument(namespaceIdentifier != null);
    OpenNamespaceIdentifier openNamespaceIdentifier = new OpenNamespaceIdentifier();
    openNamespaceIdentifier.setAppId(namespaceIdentifier.getAppId());
    openNamespaceIdentifier.setEnv(namespaceIdentifier.getEnv().toString());
    openNamespaceIdentifier.setClusterName(namespaceIdentifier.getClusterName());
    openNamespaceIdentifier.setNamespaceName(namespaceIdentifier.getNamespaceName());
    return openNamespaceIdentifier;
  }

  public static NamespaceSyncModel toNamespaceSyncModel(final OpenNamespaceSyncModel openNamespaceSyncModel) {
    Preconditions.checkArgument(openNamespaceSyncModel != null);
    NamespaceSyncModel model = BeanUtils.transform(NamespaceSyncModel.class, openNamespaceSyncModel);
    if (openNamespaceSyncModel.getSyncToNamespaces() != null) {
      model.setSyncToNamespaces(toNamespaceIdentifiers(openNamespaceSyncModel.getSyncToNamespaces()));
    }
    if (openNamespaceSyncModel.getSyncItems() != null) {
      model.setSyncItems(toItemDTOs(openNamespaceSyncModel.getSyncItems()));
    }
    return model;
  }

  public static List<NamespaceSyncModel> toNamespaceSyncModels(final List<OpenNamespaceSyncModel> openNamespaceSyncModels) {
    if (CollectionUtils.isEmpty(openNamespaceSyncModels)) {
      return Collections.emptyList();
    }
    return openNamespaceSyncModels.stream().map(OpenApiModelConverters::toNamespaceSyncModel).collect(Collectors.toList());
  }
  // endregion

  // region Gray release rule conversions
  public static OpenGrayReleaseRuleDTO fromGrayReleaseRuleDTO(GrayReleaseRuleDTO grayReleaseRuleDTO) {
    Preconditions.checkArgument(grayReleaseRuleDTO != null);
    return BeanUtils.transform(OpenGrayReleaseRuleDTO.class, grayReleaseRuleDTO);
  }

  public static GrayReleaseRuleDTO toGrayReleaseRuleDTO(OpenGrayReleaseRuleDTO openGrayReleaseRuleDTO) {
    Preconditions.checkArgument(openGrayReleaseRuleDTO != null);
    String appId = openGrayReleaseRuleDTO.getAppId();
    String branchName = openGrayReleaseRuleDTO.getBranchName();
    String clusterName = openGrayReleaseRuleDTO.getClusterName();
    String namespaceName = openGrayReleaseRuleDTO.getNamespaceName();
    GrayReleaseRuleDTO grayReleaseRuleDTO = new GrayReleaseRuleDTO(appId, clusterName, namespaceName, branchName);
    Set<OpenGrayReleaseRuleItemDTO> openGrayReleaseRuleItemDTOSet = new HashSet<>(openGrayReleaseRuleDTO.getRuleItems());
    openGrayReleaseRuleItemDTOSet.forEach(openGrayReleaseRuleItemDTO -> {
      String clientAppId = openGrayReleaseRuleItemDTO.getClientAppId();
      Set<String> clientIpList = new HashSet<>(openGrayReleaseRuleItemDTO.getClientIpList());
      Set<String> clientLabelList = new HashSet<>(openGrayReleaseRuleItemDTO.getClientLabelList());
      GrayReleaseRuleItemDTO ruleItem = new GrayReleaseRuleItemDTO(clientAppId, clientIpList, clientLabelList);
      grayReleaseRuleDTO.addRuleItem(ruleItem);
    });
    return grayReleaseRuleDTO;
  }
  // endregion

  // region Cluster conversions
  public static OpenClusterDTO fromClusterDTO(ClusterDTO cluster) {
    Preconditions.checkArgument(cluster != null);
    return BeanUtils.transform(OpenClusterDTO.class, cluster);
  }

  public static ClusterDTO toClusterDTO(OpenClusterDTO openClusterDTO) {
    Preconditions.checkArgument(openClusterDTO != null);
    return BeanUtils.transform(ClusterDTO.class, openClusterDTO);
  }
  // endregion

  // region Organization conversions
  public static OpenOrganizationDto fromOrganization(final Organization organization) {
    Preconditions.checkArgument(organization != null);
    return BeanUtils.transform(OpenOrganizationDto.class, organization);
  }

  public static List<OpenOrganizationDto> fromOrganizations(final List<Organization> organizations) {
    if (CollectionUtils.isEmpty(organizations)) {
      return Collections.emptyList();
    }
    return organizations.stream().map(OpenApiModelConverters::fromOrganization).collect(Collectors.toList());
  }
  // endregion

  // region Instance conversions
  public static OpenInstanceDTO fromInstanceDTO(final InstanceDTO instanceDTO) {
    Preconditions.checkArgument(instanceDTO != null);
    return BeanUtils.transform(OpenInstanceDTO.class, instanceDTO);
  }

  public static List<OpenInstanceDTO> fromInstanceDTOs(final List<InstanceDTO> instanceDTOs) {
    if (CollectionUtils.isEmpty(instanceDTOs)) {
      return Collections.emptyList();
    }
    return instanceDTOs.stream().map(OpenApiModelConverters::fromInstanceDTO).collect(Collectors.toList());
  }
  // endregion

  // region Env/Cluster info conversions
  public static OpenEnvClusterInfo fromEnvClusterInfo(final EnvClusterInfo envClusterInfo) {
    Preconditions.checkArgument(envClusterInfo != null);
    return BeanUtils.transform(OpenEnvClusterInfo.class, envClusterInfo);
  }

  public static List<OpenEnvClusterInfo> fromEnvClusterInfos(final List<EnvClusterInfo> envClusterInfos) {
    if (CollectionUtils.isEmpty(envClusterInfos)) {
      return Collections.emptyList();
    }
    return envClusterInfos.stream().map(OpenApiModelConverters::fromEnvClusterInfo).collect(Collectors.toList());
  }
  // endregion

  // region Item diffs/change sets
  public static OpenItemChangeSets fromItemChangeSets(final ItemChangeSets itemChangeSets) {
    Preconditions.checkArgument(itemChangeSets != null);
    OpenItemChangeSets openItemChangeSets = new OpenItemChangeSets();
    if (itemChangeSets.getCreateItems() != null) {
      openItemChangeSets.setCreateItems(fromItemDTOs(itemChangeSets.getCreateItems()));
    }
    if (itemChangeSets.getUpdateItems() != null) {
      openItemChangeSets.setUpdateItems(fromItemDTOs(itemChangeSets.getUpdateItems()));
    }
    if (itemChangeSets.getDeleteItems() != null) {
      openItemChangeSets.setDeleteItems(fromItemDTOs(itemChangeSets.getDeleteItems()));
    }
    return openItemChangeSets;
  }

  public static OpenItemDiffs fromItemDiffs(final ItemDiffs itemDiffs) {
    Preconditions.checkArgument(itemDiffs != null);
    OpenItemDiffs openItemDiffs = new OpenItemDiffs();
    if (itemDiffs.getNamespace() != null) {
      openItemDiffs.setNamespace(fromNamespaceIdentifier(itemDiffs.getNamespace()));
    }
    if (itemDiffs.getDiffs() != null) {
      openItemDiffs.setDiffs(fromItemChangeSets(itemDiffs.getDiffs()));
    }
    openItemDiffs.setExtInfo(itemDiffs.getExtInfo());
    return openItemDiffs;
  }

  public static List<OpenItemDiffs> fromItemDiffsList(final List<ItemDiffs> itemDiffsList) {
    if (CollectionUtils.isEmpty(itemDiffsList)) {
      return Collections.emptyList();
    }
    return itemDiffsList.stream().map(OpenApiModelConverters::fromItemDiffs).collect(Collectors.toList());
  }
  // endregion
}

