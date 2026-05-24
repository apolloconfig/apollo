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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenEnvClusterInfo;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDiffDTO;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.vo.EnvClusterInfo;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.enums.ChangeType;
import com.google.common.collect.Lists;
import java.util.Collections;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;

public class OpenApiModelConvertersTest {

  @Test
  public void fromEnvClusterInfoShouldKeepPortalNavTreeFields() {
    ClusterDTO cluster = createCluster("default", "someAppId", "default cluster");

    EnvClusterInfo envClusterInfo = new EnvClusterInfo(Env.DEV);
    envClusterInfo.setClusters(Lists.newArrayList(cluster));

    OpenEnvClusterInfo result = OpenApiModelConverters.fromEnvClusterInfo(envClusterInfo);

    assertEquals("DEV", result.getEnv());
    assertEquals(1, result.getClusters().size());
    assertEquals("default", result.getClusters().get(0).getName());
    assertEquals("someAppId", result.getClusters().get(0).getAppId());
    assertEquals("default cluster", result.getClusters().get(0).getComment());
  }

  @Test
  public void fromEnvClusterInfoShouldHandleEmptyClusterList() {
    EnvClusterInfo envClusterInfo = new EnvClusterInfo(Env.DEV);
    envClusterInfo.setClusters(Lists.newArrayList());

    OpenEnvClusterInfo result = OpenApiModelConverters.fromEnvClusterInfo(envClusterInfo);

    assertEquals("DEV", result.getEnv());
    assertEquals(0, result.getClusters().size());
  }

  @Test
  public void fromEnvClusterInfoShouldHandleMultipleClusters() {
    ClusterDTO defaultCluster = createCluster("default", "someAppId", "default cluster");
    ClusterDTO featureCluster = createCluster("feature", "someAppId", "feature cluster");

    EnvClusterInfo envClusterInfo = new EnvClusterInfo(Env.DEV);
    envClusterInfo.setClusters(Lists.newArrayList(defaultCluster, featureCluster));

    OpenEnvClusterInfo result = OpenApiModelConverters.fromEnvClusterInfo(envClusterInfo);

    assertEquals("DEV", result.getEnv());
    assertEquals(2, result.getClusters().size());
    assertEquals("default", result.getClusters().get(0).getName());
    assertEquals("someAppId", result.getClusters().get(0).getAppId());
    assertEquals("default cluster", result.getClusters().get(0).getComment());
    assertEquals("feature", result.getClusters().get(1).getName());
    assertEquals("someAppId", result.getClusters().get(1).getAppId());
    assertEquals("feature cluster", result.getClusters().get(1).getComment());
  }

  @Test
  public void fromEnvClusterInfoShouldHandleNullClusters() {
    EnvClusterInfo envClusterInfo = new EnvClusterInfo(Env.DEV);
    envClusterInfo.setClusters(null);

    OpenEnvClusterInfo result = OpenApiModelConverters.fromEnvClusterInfo(envClusterInfo);

    assertEquals("DEV", result.getEnv());
    assertEquals(0, result.getClusters().size());
  }

  @Test
  public void fromNamespaceBOShouldExposeNamespaceAndItemExtendInfo() {
    NamespaceDTO baseInfo = new NamespaceDTO();
    baseInfo.setId(100L);
    baseInfo.setAppId("sample-app");
    baseInfo.setClusterName("default");
    baseInfo.setNamespaceName("application");

    ItemDTO item = new ItemDTO("timeout", "200", "comment", 1);
    item.setNamespaceId(100L);
    ItemBO itemBO = new ItemBO();
    itemBO.setItem(item);
    itemBO.setModified(true);
    itemBO.setNewlyAdded(true);
    itemBO.setDeleted(false);
    itemBO.setOldValue("100");
    itemBO.setNewValue("200");

    NamespaceBO namespaceBO = new NamespaceBO();
    namespaceBO.setBaseInfo(baseInfo);
    namespaceBO.setFormat("properties");
    namespaceBO.setComment("namespace comment");
    namespaceBO.setPublic(true);
    namespaceBO.setParentAppId("public-app");
    namespaceBO.setItemModifiedCnt(1);
    namespaceBO.setConfigHidden(true);
    namespaceBO.setItems(Collections.singletonList(itemBO));

    OpenNamespaceDTO result = OpenApiModelConverters.fromNamespaceBO(namespaceBO);

    assertNotNull(result.getExtendInfo());
    assertEquals(true, result.getExtendInfo().getIsConfigHidden());
    assertEquals("public-app", result.getExtendInfo().getParentAppId());
    assertEquals(1, result.getExtendInfo().getItemModifiedCnt());
    assertEquals(1, result.getItems().size());
    assertNotNull(result.getItems().get(0).getExtendInfo());
    assertEquals(100L, result.getItems().get(0).getExtendInfo().getNamespaceId());
    assertEquals(true, result.getItems().get(0).getExtendInfo().getIsModified());
    assertEquals(true, result.getItems().get(0).getExtendInfo().getIsNewlyAdded());
    assertEquals(false, result.getItems().get(0).getExtendInfo().getIsDeleted());
    assertEquals("100", result.getItems().get(0).getExtendInfo().getOldValue());
    assertEquals("200", result.getItems().get(0).getExtendInfo().getNewValue());
  }

  @Test
  public void appNamespaceConvertersShouldPreserveGeneratedIsPublicFlag() {
    OpenAppNamespaceDTO openAppNamespace = new OpenAppNamespaceDTO();
    openAppNamespace.setAppId("provider-app");
    openAppNamespace.setName("public.namespace");
    openAppNamespace.setFormat("properties");
    openAppNamespace.setIsPublic(true);

    AppNamespace appNamespace = OpenApiModelConverters.toAppNamespace(openAppNamespace);

    assertEquals(true, appNamespace.isPublic());

    OpenAppNamespaceDTO result = OpenApiModelConverters.fromAppNamespace(appNamespace);

    assertEquals(true, result.getIsPublic());
  }

  @Test
  public void fromReleaseDTOShouldParseConfigurationsIntoGeneratedMap() {
    ReleaseDTO release = new ReleaseDTO();
    release.setId(123L);
    release.setAppId("sample-app");
    release.setClusterName("default");
    release.setNamespaceName("application");
    release.setName("release-1");
    release.setConfigurations("{\"timeout\":\"200\",\"feature.enabled\":\"true\"}");

    OpenReleaseDTO result = OpenApiModelConverters.fromReleaseDTO(release);

    assertEquals(123L, result.getId());
    assertEquals("sample-app", result.getAppId());
    assertEquals("default", result.getClusterName());
    assertEquals("application", result.getNamespaceName());
    assertEquals("200", result.getConfigurations().get("timeout"));
    assertEquals("true", result.getConfigurations().get("feature.enabled"));
  }

  @Test
  public void fromReleaseCompareResultShouldExposeFlatReleaseChanges() {
    ReleaseCompareResult compareResult = new ReleaseCompareResult();
    compareResult.addEntityPair(ChangeType.MODIFIED, new KVEntity("timeout", "100"),
        new KVEntity("timeout", "200"));
    compareResult.addEntityPair(ChangeType.ADDED, new KVEntity("enabled", ""),
        new KVEntity("enabled", "true"));

    OpenReleaseDiffDTO result = OpenApiModelConverters.fromReleaseCompareResult(compareResult);

    assertEquals(2, result.getChanges().size());
    assertEquals("MODIFIED", result.getChanges().get(0).getChangeType());
    assertEquals("timeout", result.getChanges().get(0).getKey());
    assertEquals("100", result.getChanges().get(0).getOldValue());
    assertEquals("200", result.getChanges().get(0).getNewValue());
    assertEquals("ADDED", result.getChanges().get(1).getChangeType());
    assertEquals("enabled", result.getChanges().get(1).getKey());
    assertEquals("", result.getChanges().get(1).getOldValue());
    assertEquals("true", result.getChanges().get(1).getNewValue());
  }

  @Test
  public void fromInstancePageDTOShouldExposeGeneratedInstancesField() {
    InstanceDTO instance = new InstanceDTO();
    instance.setId(10L);
    instance.setAppId("client-app");
    instance.setClusterName("default");
    instance.setIp("10.0.0.1");

    PageDTO<InstanceDTO> page =
        new PageDTO<>(Collections.singletonList(instance), PageRequest.of(2, 10), 21L);

    OpenInstancePageDTO result = OpenApiModelConverters.fromInstancePageDTO(page);

    assertEquals(2, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(21L, result.getTotal());
    assertEquals(1, result.getInstances().size());
    assertEquals("client-app", result.getInstances().get(0).getAppId());
    assertEquals("10.0.0.1", result.getInstances().get(0).getIp());
  }

  private ClusterDTO createCluster(String name, String appId, String comment) {
    ClusterDTO cluster = new ClusterDTO();
    cluster.setName(name);
    cluster.setAppId(appId);
    cluster.setComment(comment);
    return cluster;
  }
}
