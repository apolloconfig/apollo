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

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceUsage;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.AbstractUnitTest;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.txtresolver.PropertyResolver;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NamespaceServiceTest extends AbstractUnitTest {

  @Mock
  private AdminServiceAPI.NamespaceAPI namespaceAPI;
  @Mock
  private ReleaseService releaseService;
  @Mock
  private ItemService itemService;
  @Mock
  private PropertyResolver resolver;
  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private InstanceService instanceService;
  @Mock
  private NamespaceBranchService branchService;
  @Mock
  private UserInfoHolder userInfoHolder;
  @Mock
  private AdditionalUserInfoEnrichService additionalUserInfoEnrichService;
  @Mock
  private PortalSettings portalSettings;
  @Mock
  private ClusterService clusterService;

  @InjectMocks
  private NamespaceService namespaceService;

  private String testAppId = "6666";
  private String testClusterName = "default";
  private String testNamespaceName = "application";
  private Env testEnv = Env.DEV;

  @Before
  public void setup() {
  }

  @Test
  public void testFindNamespace() {

    AppNamespace applicationAppNamespace = mock(AppNamespace.class);
    AppNamespace hermesAppNamespace = mock(AppNamespace.class);

    NamespaceDTO application = new NamespaceDTO();
    application.setId(1);
    application.setClusterName(testClusterName);
    application.setAppId(testAppId);
    application.setNamespaceName(testNamespaceName);

    NamespaceDTO hermes = new NamespaceDTO();
    hermes.setId(2);
    hermes.setClusterName("default");
    hermes.setAppId(testAppId);
    hermes.setNamespaceName("hermes");
    List<NamespaceDTO> namespaces = Arrays.asList(application, hermes);

    ReleaseDTO someRelease = new ReleaseDTO();
    someRelease.setConfigurations("{\"a\":\"123\",\"b\":\"123\"}");

    ItemDTO i1 = new ItemDTO("a", "123", "", 1);
    ItemDTO i2 = new ItemDTO("b", "1", "", 2);
    ItemDTO i3 = new ItemDTO("", "", "#dddd", 3);
    ItemDTO i4 = new ItemDTO("c", "1", "", 4);
    List<ItemDTO> someItems = Arrays.asList(i1, i2, i3, i4);

    when(applicationAppNamespace.getFormat()).thenReturn(ConfigFileFormat.Properties.getValue());
    when(hermesAppNamespace.getFormat()).thenReturn(ConfigFileFormat.XML.getValue());
    when(appNamespaceService.findByAppIdAndName(testAppId, testNamespaceName))
        .thenReturn(applicationAppNamespace);
    when(appNamespaceService.findPublicAppNamespace("hermes")).thenReturn(hermesAppNamespace);
    when(namespaceAPI.findNamespaceByCluster(testAppId, Env.DEV, testClusterName)).thenReturn(namespaces);
    when(releaseService.loadLatestRelease(testAppId, Env.DEV, testClusterName,
                                          testNamespaceName)).thenReturn(someRelease);
    when(releaseService.loadLatestRelease(testAppId, Env.DEV, testClusterName, "hermes")).thenReturn(someRelease);
    when(itemService.findItems(testAppId, Env.DEV, testClusterName, testNamespaceName)).thenReturn(someItems);

    List<NamespaceBO> namespaceVOs = namespaceService.findNamespaceBOs(testAppId, Env.DEV, testClusterName);
    assertEquals(2, namespaceVOs.size());

    when(namespaceAPI.findNamespaceByCluster(testAppId, Env.DEV, testClusterName)).thenReturn(Lists.list(application));
    namespaceVOs = namespaceService.findNamespaceBOs(testAppId, Env.DEV, testClusterName);
    assertEquals(1, namespaceVOs.size());
    NamespaceBO namespaceVO = namespaceVOs.get(0);
    assertEquals(4, namespaceVO.getItems().size());
    assertEquals("a", namespaceVO.getItems().get(0).getItem().getKey());
    assertEquals(2, namespaceVO.getItemModifiedCnt());
    assertEquals(testAppId, namespaceVO.getBaseInfo().getAppId());
    assertEquals(testClusterName, namespaceVO.getBaseInfo().getClusterName());
    assertEquals(testNamespaceName, namespaceVO.getBaseInfo().getNamespaceName());

    ReleaseDTO errorRelease = new ReleaseDTO();
    errorRelease.setConfigurations("\"a\":\"123\",\"b\":\"123\"");
    when(releaseService.loadLatestRelease(testAppId, Env.DEV, testClusterName, testNamespaceName)).thenReturn(errorRelease);
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(()-> namespaceService.findNamespaceBOs(testAppId, Env.DEV, testClusterName))
        .withMessageStartingWith("Parse namespaces error, expected: 1, but actual: 0, cannot get those namespaces: [application]");

  }

  @Test
  public void testDeletePrivateNamespace() {
    String operator = "user";
    AppNamespace privateNamespace = createAppNamespace(testAppId, testNamespaceName, false);

    when(appNamespaceService.findByAppIdAndName(testAppId, testNamespaceName)).thenReturn(privateNamespace);

    when(userInfoHolder.getUser()).thenReturn(createUser(operator));

    namespaceService.deleteNamespace(testAppId, testEnv, testClusterName, testNamespaceName);

    verify(namespaceAPI, times(1)).deleteNamespace(testEnv, testAppId, testClusterName, testNamespaceName, operator);
  }

  @Test
  public void testGetNamespaceUsage() {
    AppNamespace publicNamespace = createAppNamespace(testAppId, testNamespaceName, true);
    String branchName = "branch";

    NamespaceDTO branch = createNamespace(testAppId, branchName, testNamespaceName);

    when(portalSettings.getActiveEnvs()).thenReturn(Lists.newArrayList(testEnv));
    ClusterDTO cluster = new ClusterDTO();
    cluster.setName(testClusterName);
    cluster.setAppId(testAppId);
    when(clusterService.findClusters(testEnv, testAppId)).thenReturn(Lists.newArrayList(cluster));
    when(appNamespaceService.findByAppIdAndName(testAppId, testNamespaceName)).thenReturn(publicNamespace);
    when(instanceService.getInstanceCountByNamespace(testAppId, testEnv, testClusterName, testNamespaceName))
        .thenReturn(8);
    when(branchService.findBranchBaseInfo(testAppId, testEnv, testClusterName, testNamespaceName)).thenReturn(branch);
    when(instanceService.getInstanceCountByNamespace(testAppId, testEnv, branchName, testNamespaceName)).thenReturn(9);
    when(appNamespaceService.findPublicAppNamespace(testNamespaceName)).thenReturn(publicNamespace);

    when(namespaceAPI.countPublicAppNamespaceAssociatedNamespaces(testEnv, testNamespaceName)).thenReturn(10);

    List<NamespaceUsage> usages = namespaceService.getNamespaceUsageByAppId(testAppId, testNamespaceName);
    assertThat(usages).asList().hasSize(1);
    assertThat(usages.get(0).getInstanceCount()).isEqualTo(8);
    assertThat(usages.get(0).getBranchInstanceCount()).isEqualTo(9);
    assertThat(usages.get(0).getLinkedNamespaceCount()).isEqualTo(10);

    NamespaceUsage usage = namespaceService.getNamespaceUsageByEnv(testAppId, testNamespaceName, testEnv, testClusterName);
    assertThat(usage).isNotNull();
    assertThat(usage.getInstanceCount()).isEqualTo(8);
    assertThat(usage.getBranchInstanceCount()).isEqualTo(9);
    assertThat(usage.getLinkedNamespaceCount()).isEqualTo(0);
  }

  @Test
  public void testDeleteEmptyNamespace() {
    String branchName = "branch";
    String operator = "user";

    AppNamespace publicNamespace = createAppNamespace(testAppId, testNamespaceName, true);
    NamespaceDTO branch = createNamespace(testAppId, branchName, testNamespaceName);

    when(appNamespaceService.findByAppIdAndName(testAppId, testNamespaceName)).thenReturn(publicNamespace);
    when(instanceService.getInstanceCountByNamespace(testAppId, testEnv, testClusterName, testNamespaceName))
        .thenReturn(0);
    when(branchService.findBranchBaseInfo(testAppId, testEnv, testClusterName, testNamespaceName)).thenReturn(branch);
    when(instanceService.getInstanceCountByNamespace(testAppId, testEnv, branchName, testNamespaceName)).thenReturn(0);
    when(appNamespaceService.findPublicAppNamespace(testNamespaceName)).thenReturn(publicNamespace);

    NamespaceDTO namespace = createNamespace(testAppId, testClusterName, testNamespaceName);
    when(namespaceAPI.getPublicAppNamespaceAllNamespaces(testEnv, testNamespaceName, 0, 10)).thenReturn(
        Collections.singletonList(namespace));
    when(userInfoHolder.getUser()).thenReturn(createUser(operator));

    namespaceService.deleteNamespace(testAppId, testEnv, testClusterName, testNamespaceName);

    verify(namespaceAPI, times(1)).deleteNamespace(testEnv, testAppId, testClusterName, testNamespaceName, operator);

  }

  @Test
  public void testLoadNamespaceBO() {
    boolean fillItemDetail = true;
    NamespaceBO namespaceBO = loadNamespaceBO(fillItemDetail);

    List<String> namespaceKey2 = namespaceBO.getItems().stream().map(s -> s.getItem().getKey()).collect(Collectors.toList());
    assertThat(namespaceBO.getItemModifiedCnt()).isEqualTo(2);
    assertThat(namespaceBO.getItems().size()).isEqualTo(2);
    assertThat(namespaceKey2).isEqualTo(Arrays.asList("k1", "k2"));
  }

  @Test
  public void testLoadNamespaceBOWithoutItemDetail() {
    boolean fillItemDetail = false;
    NamespaceBO namespaceBO = loadNamespaceBO(fillItemDetail);

    assertThat(namespaceBO.getItems().size()).isEqualTo(0);
  }

  private NamespaceBO loadNamespaceBO(boolean fillItemDetail) {
    when(namespaceAPI.loadNamespace(any(), any(), any(), any())).thenReturn(createNamespace(testAppId, "branch", testNamespaceName));
    when(releaseService.loadLatestRelease(any(), any(), any(), any())).thenReturn(createReleaseDTO());
    when(itemService.findItems(any(), any(), any(), any())).thenReturn(createItems());
    when(itemService.findDeletedItems(any(), any(), any(), any())).thenReturn(createDeletedItems());

    NamespaceBO namespaceBO1 = namespaceService.loadNamespaceBO(testAppId, testEnv, testClusterName, testNamespaceName);
    List<String> namespaceKey1 = namespaceBO1.getItems().stream().map(s -> s.getItem().getKey()).collect(Collectors.toList());
    assertThat(namespaceBO1.getItemModifiedCnt()).isEqualTo(3);
    assertThat(namespaceBO1.getItems().size()).isEqualTo(3);
    assertThat(namespaceKey1).isEqualTo(Arrays.asList("k1", "k2", "k3"));

    return namespaceService.loadNamespaceBO(testAppId, testEnv, testClusterName, testNamespaceName, fillItemDetail, false);
  }

  @Test
  public void testFindPublicNamespaceForAssociatedNamespace() {
    when(namespaceAPI.findPublicNamespaceForAssociatedNamespace(any(), any(), any(), any())).thenReturn(createNamespace(testAppId, "branch", testNamespaceName));
    when(releaseService.loadLatestRelease(any(), any(), any(), any())).thenReturn(createReleaseDTO());
    when(itemService.findItems(any(), any(), any(), any())).thenReturn(createItems());
    when(itemService.findDeletedItems(any(), any(), any(), any())).thenReturn(createDeletedItems());

    NamespaceBO namespaceBO = namespaceService.findPublicNamespaceForAssociatedNamespace(testEnv, testAppId, testClusterName, testNamespaceName);

    List<String> namespaceKey2 = namespaceBO.getItems().stream().map(s -> s.getItem().getKey()).collect(Collectors.toList());
    assertThat(namespaceBO.getItemModifiedCnt()).isEqualTo(3);
    assertThat(namespaceBO.getItems().size()).isEqualTo(3);
    assertThat(namespaceKey2).isEqualTo(Arrays.asList("k1", "k2", "k3"));
  }

  private ReleaseDTO createReleaseDTO() {
    ReleaseDTO releaseDTO = new ReleaseDTO();
    releaseDTO.setConfigurations("{\"k1\":\"k1\",\"k2\":\"k2\", \"k3\":\"\"}");
    return releaseDTO;
  }

  private List<ItemDTO> createItems() {
    List<ItemDTO> itemDTOList = Lists.newArrayList();
    ItemDTO itemDTO1 = new ItemDTO();
    itemDTO1.setId(1);
    itemDTO1.setNamespaceId(1);
    itemDTO1.setKey("k1");
    itemDTO1.setValue(String.valueOf(1));
    itemDTOList.add(itemDTO1);

    ItemDTO itemDTO2 = new ItemDTO();
    itemDTO2.setId(2);
    itemDTO2.setNamespaceId(2);
    itemDTO2.setKey("k2");
    itemDTO2.setValue(String.valueOf(2));
    itemDTOList.add(itemDTO2);

    return itemDTOList;
  }

  private List<ItemDTO> createDeletedItems() {
    List<ItemDTO> deletedItemDTOList = Lists.newArrayList();
    ItemDTO deletedItemDTO = new ItemDTO();
    deletedItemDTO.setId(3);
    deletedItemDTO.setNamespaceId(3);
    deletedItemDTO.setKey("k3");
    deletedItemDTOList.add(deletedItemDTO);
    return deletedItemDTOList;
  }

  private AppNamespace createAppNamespace(String appId, String name, boolean isPublic) {
    AppNamespace instance = new AppNamespace();

    instance.setAppId(appId);
    instance.setName(name);
    instance.setPublic(isPublic);

    return instance;
  }

  private NamespaceDTO createNamespace(String appId, String clusterName, String namespaceName) {
    NamespaceDTO instance = new NamespaceDTO();

    instance.setAppId(appId);
    instance.setClusterName(clusterName);
    instance.setNamespaceName(namespaceName);

    return instance;
  }

  private UserInfo createUser(String userId) {
    UserInfo instance = new UserInfo();

    instance.setUserId(userId);

    return instance;
  }
}
