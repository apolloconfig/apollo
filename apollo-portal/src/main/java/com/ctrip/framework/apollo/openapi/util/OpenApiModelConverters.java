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
package com.ctrip.framework.apollo.openapi.util;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.openapi.model.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenAccessKeyDTO;
import com.ctrip.framework.apollo.openapi.model.OpenAppRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.model.OpenClusterNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenConsumerInfoDTO;
import com.ctrip.framework.apollo.openapi.model.OpenConsumerSummaryDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemDiffDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemExtendDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceExtendDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceIdentifier;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceRoleUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceTextModel;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceUsageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenOrganizationDto;
import com.ctrip.framework.apollo.openapi.model.OpenPermissionConditionDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseChangeDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDiffDTO;
import com.ctrip.framework.apollo.openapi.model.OpenUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenUserInfoDTO;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.entity.vo.AppRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.ClusterNamespaceRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceEnvRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceRolesAssignedUsers;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceUsage;
import com.ctrip.framework.apollo.portal.entity.vo.Organization;
import com.ctrip.framework.apollo.portal.entity.vo.PermissionCondition;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.entity.vo.Change;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerInfo;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Non-invasive converters for OpenAPI generated model classes.
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
    return openItemDTOs.stream().map(OpenApiModelConverters::toItemDTO)
        .collect(Collectors.toList());
  }

  public static List<OpenItemDTO> fromItemDTOs(List<ItemDTO> items) {
    if (CollectionUtils.isEmpty(items)) {
      return Collections.emptyList();
    }
    return items.stream().map(OpenApiModelConverters::fromItemDTO).collect(Collectors.toList());
  }

  public static OpenItemPageDTO fromItemPageDTO(PageDTO<ItemDTO> page) {
    Preconditions.checkArgument(page != null);
    OpenItemPageDTO result = new OpenItemPageDTO();
    result.setPage(page.getPage());
    result.setSize(page.getSize());
    result.setTotal(page.getTotal());
    result.setContent(fromItemDTOs(page.getContent()));
    return result;
  }

  public static OpenItemDiffDTO fromItemDiffs(ItemDiffs itemDiffs) {
    Preconditions.checkArgument(itemDiffs != null);
    OpenItemDiffDTO result = new OpenItemDiffDTO();
    result.setCode(0);
    result.setMessage(itemDiffs.getExtInfo());
    if (itemDiffs.getNamespace() != null) {
      result.setNamespace(fromNamespaceIdentifier(itemDiffs.getNamespace()));
    }

    ItemChangeSets diffs = itemDiffs.getDiffs();
    if (diffs == null) {
      result.setCreateItems(Collections.emptyList());
      result.setUpdateItems(Collections.emptyList());
      result.setDeleteItems(Collections.emptyList());
      return result;
    }
    result.setCreateItems(fromItemDTOs(diffs.getCreateItems()));
    result.setUpdateItems(fromItemDTOs(diffs.getUpdateItems()));
    result.setDeleteItems(fromItemDTOs(diffs.getDeleteItems()));
    return result;
  }

  public static List<OpenItemDiffDTO> fromItemDiffs(List<ItemDiffs> itemDiffs) {
    if (CollectionUtils.isEmpty(itemDiffs)) {
      return Collections.emptyList();
    }
    return itemDiffs.stream().map(OpenApiModelConverters::fromItemDiffs)
        .collect(Collectors.toList());
  }
  // endregion

  // region App/AppNamespace conversions
  public static OpenAppNamespaceDTO fromAppNamespace(AppNamespace appNamespace) {
    Preconditions.checkArgument(appNamespace != null);
    OpenAppNamespaceDTO result = BeanUtils.transform(OpenAppNamespaceDTO.class, appNamespace);
    result.setIsPublic(appNamespace.isPublic());
    return result;
  }

  public static AppNamespace toAppNamespace(OpenAppNamespaceDTO openAppNamespaceDTO) {
    Preconditions.checkArgument(openAppNamespaceDTO != null);
    AppNamespace result = BeanUtils.transform(AppNamespace.class, openAppNamespaceDTO);
    result.setPublic(Boolean.TRUE.equals(openAppNamespaceDTO.getIsPublic()));
    return result;
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

  public static List<OpenReleaseDTO> fromReleaseDTOs(List<ReleaseDTO> releases) {
    if (CollectionUtils.isEmpty(releases)) {
      return Collections.emptyList();
    }
    return releases.stream().map(OpenApiModelConverters::fromReleaseDTO)
        .collect(Collectors.toList());
  }

  public static OpenReleaseDiffDTO fromReleaseCompareResult(
      ReleaseCompareResult releaseCompareResult) {
    Preconditions.checkArgument(releaseCompareResult != null);
    OpenReleaseDiffDTO result = new OpenReleaseDiffDTO();
    if (CollectionUtils.isEmpty(releaseCompareResult.getChanges())) {
      result.setChanges(Collections.emptyList());
      return result;
    }
    result.setChanges(releaseCompareResult.getChanges().stream()
        .map(OpenApiModelConverters::fromReleaseCompareChange).collect(Collectors.toList()));
    return result;
  }

  private static OpenReleaseChangeDTO fromReleaseCompareChange(Change change) {
    OpenReleaseChangeDTO result = new OpenReleaseChangeDTO();
    result.setChangeType(change.getType().name());
    KVEntity oldEntity = change.getEntity().getFirstEntity();
    KVEntity newEntity = change.getEntity().getSecondEntity();
    result.setKey(oldEntity == null ? newEntity.getKey() : oldEntity.getKey());
    result.setOldValue(oldEntity == null ? null : oldEntity.getValue());
    result.setNewValue(newEntity == null ? null : newEntity.getValue());
    return result;
  }

  // endregion

  // region Namespace conversions
  public static OpenNamespaceDTO fromNamespaceBO(NamespaceBO namespaceBO) {
    Preconditions.checkArgument(namespaceBO != null);
    OpenNamespaceDTO openNamespaceDTO =
        BeanUtils.transform(OpenNamespaceDTO.class, namespaceBO.getBaseInfo());
    openNamespaceDTO.setFormat(namespaceBO.getFormat());
    openNamespaceDTO.setComment(namespaceBO.getComment());
    openNamespaceDTO.setIsPublic(namespaceBO.isPublic());
    OpenNamespaceExtendDTO namespaceExtend = new OpenNamespaceExtendDTO();
    namespaceExtend.setIsConfigHidden(namespaceBO.isConfigHidden());
    namespaceExtend.setParentAppId(namespaceBO.getParentAppId());
    namespaceExtend.setItemModifiedCnt(namespaceBO.getItemModifiedCnt());
    openNamespaceDTO.setExtendInfo(namespaceExtend);
    List<OpenItemDTO> items = new LinkedList<>();
    List<ItemBO> itemBOs = namespaceBO.getItems();
    if (!CollectionUtils.isEmpty(itemBOs)) {
      items.addAll(
          itemBOs.stream().map(OpenApiModelConverters::fromItemBO).collect(Collectors.toList()));
    }
    openNamespaceDTO.setItems(items);
    return openNamespaceDTO;
  }

  public static OpenItemDTO fromItemBO(ItemBO itemBO) {
    Preconditions.checkArgument(itemBO != null);
    OpenItemDTO openItemDTO = fromItemDTO(itemBO.getItem());
    OpenItemExtendDTO itemExtend = new OpenItemExtendDTO();
    itemExtend.setNamespaceId(itemBO.getItem().getNamespaceId());
    itemExtend.setIsModified(itemBO.isModified());
    itemExtend.setIsDeleted(itemBO.isDeleted());
    itemExtend.setIsNewlyAdded(itemBO.isNewlyAdded());
    itemExtend.setOldValue(itemBO.getOldValue());
    itemExtend.setNewValue(itemBO.getNewValue());
    openItemDTO.setExtendInfo(itemExtend);
    return openItemDTO;
  }

  public static List<OpenNamespaceDTO> fromNamespaceBOs(List<NamespaceBO> namespaceBOs) {
    if (CollectionUtils.isEmpty(namespaceBOs)) {
      return Collections.emptyList();
    }
    return namespaceBOs.stream().map(OpenApiModelConverters::fromNamespaceBO)
        .collect(Collectors.toCollection(LinkedList::new));
  }

  public static OpenNamespaceLockDTO fromNamespaceLockDTO(String namespaceName,
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

  public static OpenNamespaceDTO fromNamespaceDTO(NamespaceDTO namespaceDTO) {
    Preconditions.checkArgument(namespaceDTO != null);
    return BeanUtils.transform(OpenNamespaceDTO.class, namespaceDTO);
  }

  public static List<OpenNamespaceDTO> fromNamespaceDTOs(List<NamespaceDTO> namespaces) {
    if (CollectionUtils.isEmpty(namespaces)) {
      return Collections.emptyList();
    }
    return namespaces.stream().map(OpenApiModelConverters::fromNamespaceDTO)
        .collect(Collectors.toList());
  }

  public static OpenNamespaceUsageDTO fromNamespaceUsage(NamespaceUsage namespaceUsage) {
    Preconditions.checkArgument(namespaceUsage != null);
    return BeanUtils.transform(OpenNamespaceUsageDTO.class, namespaceUsage);
  }

  public static List<OpenNamespaceUsageDTO> fromNamespaceUsages(
      List<NamespaceUsage> namespaceUsages) {
    if (CollectionUtils.isEmpty(namespaceUsages)) {
      return Collections.emptyList();
    }
    return namespaceUsages.stream().map(OpenApiModelConverters::fromNamespaceUsage)
        .collect(Collectors.toList());
  }

  public static NamespaceTextModel toNamespaceTextModel(
      final OpenNamespaceTextModel openNamespaceTextModel) {
    Preconditions.checkArgument(openNamespaceTextModel != null);
    return BeanUtils.transform(NamespaceTextModel.class, openNamespaceTextModel);
  }

  public static List<NamespaceTextModel> toNamespaceTextModels(
      final List<OpenNamespaceTextModel> openNamespaceTextModels) {
    if (CollectionUtils.isEmpty(openNamespaceTextModels)) {
      return Collections.emptyList();
    }
    return openNamespaceTextModels.stream().map(OpenApiModelConverters::toNamespaceTextModel)
        .collect(Collectors.toList());
  }

  public static NamespaceIdentifier toNamespaceIdentifier(
      final OpenNamespaceIdentifier openNamespaceIdentifier) {
    Preconditions.checkArgument(openNamespaceIdentifier != null);
    NamespaceIdentifier namespaceIdentifier = new NamespaceIdentifier();
    namespaceIdentifier.setAppId(openNamespaceIdentifier.getAppId());
    namespaceIdentifier.setEnv(openNamespaceIdentifier.getEnv());
    namespaceIdentifier.setClusterName(openNamespaceIdentifier.getClusterName());
    namespaceIdentifier.setNamespaceName(openNamespaceIdentifier.getNamespaceName());
    return namespaceIdentifier;
  }

  public static List<NamespaceIdentifier> toNamespaceIdentifiers(
      final List<OpenNamespaceIdentifier> openNamespaceIdentifiers) {
    if (CollectionUtils.isEmpty(openNamespaceIdentifiers)) {
      return Collections.emptyList();
    }
    return openNamespaceIdentifiers.stream().map(OpenApiModelConverters::toNamespaceIdentifier)
        .collect(Collectors.toList());
  }

  public static OpenNamespaceIdentifier fromNamespaceIdentifier(
      final NamespaceIdentifier namespaceIdentifier) {
    Preconditions.checkArgument(namespaceIdentifier != null);
    OpenNamespaceIdentifier openNamespaceIdentifier = new OpenNamespaceIdentifier();
    openNamespaceIdentifier.setAppId(namespaceIdentifier.getAppId());
    openNamespaceIdentifier.setEnv(namespaceIdentifier.getEnv().toString());
    openNamespaceIdentifier.setClusterName(namespaceIdentifier.getClusterName());
    openNamespaceIdentifier.setNamespaceName(namespaceIdentifier.getNamespaceName());
    return openNamespaceIdentifier;
  }

  // endregion

  // region Gray release rule conversions
  public static OpenGrayReleaseRuleDTO fromGrayReleaseRuleDTO(
      GrayReleaseRuleDTO grayReleaseRuleDTO) {
    Preconditions.checkArgument(grayReleaseRuleDTO != null);
    return BeanUtils.transform(OpenGrayReleaseRuleDTO.class, grayReleaseRuleDTO);
  }

  public static GrayReleaseRuleDTO toGrayReleaseRuleDTO(
      OpenGrayReleaseRuleDTO openGrayReleaseRuleDTO) {
    Preconditions.checkArgument(openGrayReleaseRuleDTO != null);
    String appId = openGrayReleaseRuleDTO.getAppId();
    String branchName = openGrayReleaseRuleDTO.getBranchName();
    String clusterName = openGrayReleaseRuleDTO.getClusterName();
    String namespaceName = openGrayReleaseRuleDTO.getNamespaceName();
    GrayReleaseRuleDTO grayReleaseRuleDTO =
        new GrayReleaseRuleDTO(appId, clusterName, namespaceName, branchName);
    Set<OpenGrayReleaseRuleItemDTO> openGrayReleaseRuleItemDTOSet =
        openGrayReleaseRuleDTO.getRuleItems();
    if (!CollectionUtils.isEmpty(openGrayReleaseRuleItemDTOSet)) {
      openGrayReleaseRuleItemDTOSet.forEach(openGrayReleaseRuleItemDTO -> {
        String clientAppId = openGrayReleaseRuleItemDTO.getClientAppId();
        Set<String> clientIpList = openGrayReleaseRuleItemDTO.getClientIpList() != null
            ? new HashSet<>(openGrayReleaseRuleItemDTO.getClientIpList())
            : new HashSet<>();
        Set<String> clientLabelList = openGrayReleaseRuleItemDTO.getClientLabelList() != null
            ? new HashSet<>(openGrayReleaseRuleItemDTO.getClientLabelList())
            : new HashSet<>();
        GrayReleaseRuleItemDTO ruleItem =
            new GrayReleaseRuleItemDTO(clientAppId, clientIpList, clientLabelList);
        grayReleaseRuleDTO.addRuleItem(ruleItem);
      });
    }
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

  public static List<OpenOrganizationDto> fromOrganizations(
      final List<Organization> organizations) {
    if (CollectionUtils.isEmpty(organizations)) {
      return Collections.emptyList();
    }
    return organizations.stream().map(OpenApiModelConverters::fromOrganization)
        .collect(Collectors.toList());
  }
  // endregion

  // region Instance conversions
  public static OpenInstanceDTO fromInstanceDTO(final InstanceDTO instanceDTO) {
    Preconditions.checkArgument(instanceDTO != null);
    return BeanUtils.transform(OpenInstanceDTO.class, instanceDTO);
  }

  // newly added
  public static List<OpenInstanceDTO> fromInstanceDTOs(final List<InstanceDTO> instanceDTOs) {
    if (CollectionUtils.isEmpty(instanceDTOs)) {
      return Collections.emptyList();
    }
    return instanceDTOs.stream().map(OpenApiModelConverters::fromInstanceDTO)
        .collect(Collectors.toList());
  }

  public static OpenInstancePageDTO fromInstancePageDTO(final PageDTO<InstanceDTO> page) {
    Preconditions.checkArgument(page != null);
    OpenInstancePageDTO result = new OpenInstancePageDTO();
    result.setPage(page.getPage());
    result.setSize(page.getSize());
    result.setTotal(page.getTotal());
    result.setInstances(fromInstanceDTOs(page.getContent()));
    return result;
  }
  // endregion

  // region Env/Cluster info conversions
  // newly added
  public static OpenEnvClusterInfo fromEnvClusterInfo(final EnvClusterInfo envClusterInfo) {
    Preconditions.checkArgument(envClusterInfo != null);
    OpenEnvClusterInfo openEnvClusterInfo = new OpenEnvClusterInfo();
    openEnvClusterInfo.setEnv(envClusterInfo.getEnv().getName());
    openEnvClusterInfo.setClusters(fromClusterDTOs(envClusterInfo.getClusters()));
    return openEnvClusterInfo;
  }

  // newly added
  public static List<OpenEnvClusterInfo> fromEnvClusterInfos(
      final List<EnvClusterInfo> envClusterInfos) {
    if (CollectionUtils.isEmpty(envClusterInfos)) {
      return Collections.emptyList();
    }
    return envClusterInfos.stream().map(OpenApiModelConverters::fromEnvClusterInfo)
        .collect(Collectors.toList());
  }

  private static List<OpenClusterDTO> fromClusterDTOs(final List<ClusterDTO> clusters) {
    if (CollectionUtils.isEmpty(clusters)) {
      return Collections.emptyList();
    }
    return clusters.stream().map(OpenApiModelConverters::fromClusterDTO)
        .collect(Collectors.toList());
  }
  // endregion

  // region Permission and access key conversions
  public static OpenAccessKeyDTO fromAccessKeyDTO(final AccessKeyDTO accessKey) {
    Preconditions.checkArgument(accessKey != null);
    return BeanUtils.transform(OpenAccessKeyDTO.class, accessKey);
  }

  public static List<OpenAccessKeyDTO> fromAccessKeyDTOs(final List<AccessKeyDTO> accessKeys) {
    if (CollectionUtils.isEmpty(accessKeys)) {
      return Collections.emptyList();
    }
    return accessKeys.stream().map(OpenApiModelConverters::fromAccessKeyDTO)
        .collect(Collectors.toList());
  }

  public static OpenPermissionConditionDTO fromPermissionCondition(
      final PermissionCondition condition) {
    Preconditions.checkArgument(condition != null);
    OpenPermissionConditionDTO result = new OpenPermissionConditionDTO();
    result.setHasPermission(condition.hasPermission());
    return result;
  }

  public static OpenUserInfoDTO fromUserInfo(final UserInfo userInfo) {
    Preconditions.checkArgument(userInfo != null);
    return BeanUtils.transform(OpenUserInfoDTO.class, userInfo);
  }

  public static List<OpenUserInfoDTO> fromUserInfos(final List<UserInfo> userInfos) {
    if (CollectionUtils.isEmpty(userInfos)) {
      return Collections.emptyList();
    }
    return userInfos.stream().map(OpenApiModelConverters::fromUserInfo)
        .collect(Collectors.toList());
  }

  public static List<OpenUserInfoDTO> fromUserInfos(final Set<UserInfo> userInfos) {
    if (CollectionUtils.isEmpty(userInfos)) {
      return Collections.emptyList();
    }
    return userInfos.stream()
        .sorted(Comparator.comparing(UserInfo::getUserId, Comparator.nullsFirst(String::compareTo)))
        .map(OpenApiModelConverters::fromUserInfo).collect(Collectors.toList());
  }

  public static UserPO toUserPO(final OpenUserDTO user) {
    Preconditions.checkArgument(user != null);
    UserPO result = new UserPO();
    result.setUsername(user.getUsername());
    result.setUserDisplayName(user.getUserDisplayName());
    result.setPassword(user.getPassword());
    result.setEmail(user.getEmail());
    result.setEnabled(user.getEnabled() == null ? 0 : user.getEnabled());
    return result;
  }

  public static OpenConsumerInfoDTO fromConsumerInfo(final ConsumerInfo consumerInfo) {
    Preconditions.checkArgument(consumerInfo != null);
    OpenConsumerInfoDTO result = BeanUtils.transform(OpenConsumerInfoDTO.class, consumerInfo);
    result.setRateLimitEnabled(isRateLimitEnabled(consumerInfo));
    return result;
  }

  public static OpenConsumerSummaryDTO fromConsumerSummary(final ConsumerInfo consumerInfo) {
    Preconditions.checkArgument(consumerInfo != null);
    OpenConsumerSummaryDTO result = BeanUtils.transform(OpenConsumerSummaryDTO.class, consumerInfo);
    result.setRateLimitEnabled(isRateLimitEnabled(consumerInfo));
    return result;
  }

  public static List<OpenConsumerSummaryDTO> fromConsumerSummaries(
      final List<ConsumerInfo> consumerInfos) {
    if (CollectionUtils.isEmpty(consumerInfos)) {
      return Collections.emptyList();
    }
    return consumerInfos.stream().map(OpenApiModelConverters::fromConsumerSummary)
        .collect(Collectors.toList());
  }

  private static boolean isRateLimitEnabled(final ConsumerInfo consumerInfo) {
    Integer rateLimit = consumerInfo.getRateLimit();
    return rateLimit != null && rateLimit > 0;
  }

  public static OpenAppRoleUserDTO fromAppRolesAssignedUsers(
      final AppRolesAssignedUsers assignedUsers) {
    Preconditions.checkArgument(assignedUsers != null);
    OpenAppRoleUserDTO result = new OpenAppRoleUserDTO();
    result.setAppId(assignedUsers.getAppId());
    result.setMasterUsers(fromUserInfos(assignedUsers.getMasterUsers()));
    return result;
  }

  public static OpenNamespaceRoleUserDTO fromNamespaceRolesAssignedUsers(
      final NamespaceRolesAssignedUsers assignedUsers) {
    Preconditions.checkArgument(assignedUsers != null);
    OpenNamespaceRoleUserDTO result = new OpenNamespaceRoleUserDTO();
    result.setAppId(assignedUsers.getAppId());
    result.setNamespaceName(assignedUsers.getNamespaceName());
    result.setModifyRoleUsers(fromUserInfos(assignedUsers.getModifyRoleUsers()));
    result.setReleaseRoleUsers(fromUserInfos(assignedUsers.getReleaseRoleUsers()));
    return result;
  }

  public static OpenEnvNamespaceRoleUserDTO fromNamespaceEnvRolesAssignedUsers(
      final NamespaceEnvRolesAssignedUsers assignedUsers) {
    Preconditions.checkArgument(assignedUsers != null);
    OpenEnvNamespaceRoleUserDTO result = new OpenEnvNamespaceRoleUserDTO();
    result.setAppId(assignedUsers.getAppId());
    result.setNamespaceName(assignedUsers.getNamespaceName());
    result.setModifyRoleUsers(fromUserInfos(assignedUsers.getModifyRoleUsers()));
    result.setReleaseRoleUsers(fromUserInfos(assignedUsers.getReleaseRoleUsers()));
    result.setEnv(assignedUsers.getEnv().getName());
    return result;
  }

  public static OpenClusterNamespaceRoleUserDTO fromClusterNamespaceRolesAssignedUsers(
      final ClusterNamespaceRolesAssignedUsers assignedUsers) {
    Preconditions.checkArgument(assignedUsers != null);
    OpenClusterNamespaceRoleUserDTO result = new OpenClusterNamespaceRoleUserDTO();
    result.setAppId(assignedUsers.getAppId());
    result.setEnv(assignedUsers.getEnv());
    result.setCluster(assignedUsers.getCluster());
    result.setModifyRoleUsers(fromUserInfos(assignedUsers.getModifyRoleUsers()));
    result.setReleaseRoleUsers(fromUserInfos(assignedUsers.getReleaseRoleUsers()));
    return result;
  }
  // endregion

}
