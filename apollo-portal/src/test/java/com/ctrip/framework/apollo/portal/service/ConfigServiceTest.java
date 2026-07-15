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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.AbstractUnitTest;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.txtresolver.PropertyResolver;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigServiceTest extends AbstractUnitTest {

  @Mock
  private AdminServiceAPI.NamespaceAPI namespaceAPI;
  @Mock
  private AdminServiceAPI.ReleaseAPI releaseAPI;
  @Mock
  private AdminServiceAPI.ItemAPI itemAPI;
  @Mock
  private PropertyResolver resolver;
  @InjectMocks
  private ItemService configService;

  @Before
  public void setup() {
    ReflectionTestUtils.setField(configService, "propertyResolver", resolver);
  }

  @Test
  public void testUpdateConfigByText() {
    String appId = "6666";
    String clusterName = "default";
    String namespaceName = "application";
    long someNamespaceId = 123L;

    NamespaceTextModel model =
        mockNamespaceModel(appId, clusterName, namespaceName, someNamespaceId);
    List<ItemDTO> itemDTOs = mockBaseItemHas3Key();
    ItemChangeSets changeSets = new ItemChangeSets();
    changeSets.addCreateItem(new ItemDTO("d", "c", "", 4));

    NamespaceDTO someNamespaceDto = mock(NamespaceDTO.class);
    when(someNamespaceDto.getId()).thenReturn(someNamespaceId);
    when(namespaceAPI.loadNamespace(appId, model.getEnv(), clusterName, namespaceName))
        .thenReturn(someNamespaceDto);
    when(itemAPI.findItems(appId, Env.DEV, clusterName, namespaceName)).thenReturn(itemDTOs);
    when(resolver.resolve(someNamespaceId, model.getConfigText(), itemDTOs)).thenReturn(changeSets);

    configService.updateConfigItemByText(model, "test");
  }

  private NamespaceTextModel mockNamespaceModel(String appId, String clusterName,
      String namespaceName, long someNamespaceId) {
    NamespaceTextModel model = new NamespaceTextModel();
    model.setEnv("DEV");
    model.setNamespaceName(namespaceName);
    model.setClusterName(clusterName);
    model.setAppId(appId);
    model.setConfigText("a=b\nb=c\nc=d\nd=e");
    model.setFormat(ConfigFileFormat.Properties.getValue());
    model.setNamespaceId(someNamespaceId);
    return model;
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateConfigByTextWithInvalidNamespaceId() {
    String appId = "6666";
    String clusterName = "default";
    String namespaceName = "application";
    long someNamespaceId = 123L;
    long anotherNamespaceId = 321L;

    NamespaceTextModel model =
        mockNamespaceModel(appId, clusterName, namespaceName, anotherNamespaceId);
    List<ItemDTO> itemDTOs = mockBaseItemHas3Key();
    ItemChangeSets changeSets = new ItemChangeSets();
    changeSets.addCreateItem(new ItemDTO("d", "c", "", 4));

    NamespaceDTO someNamespaceDto = mock(NamespaceDTO.class);
    when(someNamespaceDto.getId()).thenReturn(someNamespaceId);
    when(namespaceAPI.loadNamespace(appId, model.getEnv(), clusterName, namespaceName))
        .thenReturn(someNamespaceDto);
    when(itemAPI.findItems(appId, Env.DEV, clusterName, namespaceName)).thenReturn(itemDTOs);
    when(resolver.resolve(someNamespaceId, model.getConfigText(), itemDTOs)).thenReturn(changeSets);

    configService.updateConfigItemByText(model, "test");
  }

  @Test
  public void testRevokeItemShouldPreserveTypeWhenUpdatingPublishedItem() {
    String appId = "6666";
    Env env = Env.DEV;
    String clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    String namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    long namespaceId = 123L;

    NamespaceDTO namespace = generateNamespaceDTO(appId, clusterName, namespaceName);
    namespace.setId(namespaceId);
    ReleaseDTO release = new ReleaseDTO();
    release.setConfigurations("{\"number\":\"42\"}");

    ItemDTO currentItem = new ItemDTO("number", "41", "comment", 1);
    currentItem.setId(1L);
    currentItem.setNamespaceId(namespaceId);
    currentItem.setType(1);

    when(namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName)).thenReturn(namespace);
    when(releaseAPI.loadLatestRelease(appId, env, clusterName, namespaceName)).thenReturn(release);
    when(itemAPI.findItems(appId, env, clusterName, namespaceName))
        .thenReturn(Collections.singletonList(currentItem));
    when(itemAPI.findDeletedItems(appId, env, clusterName, namespaceName))
        .thenReturn(Collections.emptyList());

    configService.revokeItem(appId, env, clusterName, namespaceName, "test");

    ArgumentCaptor<ItemChangeSets> captor = ArgumentCaptor.forClass(ItemChangeSets.class);
    verify(itemAPI).updateItemsByChangeSet(eq(appId), eq(env), eq(clusterName), eq(namespaceName),
        captor.capture());
    ItemChangeSets changeSets = captor.getValue();
    assertEquals(1, changeSets.getUpdateItems().size());
    assertEquals("42", changeSets.getUpdateItems().get(0).getValue());
    assertEquals(1, changeSets.getUpdateItems().get(0).getType());
  }

  @Test
  public void testRevokeItemShouldRestoreTypeWhenRecreatingDeletedPublishedItem() {
    String appId = "6666";
    Env env = Env.DEV;
    String clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    String namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    long namespaceId = 123L;

    NamespaceDTO namespace = generateNamespaceDTO(appId, clusterName, namespaceName);
    namespace.setId(namespaceId);
    ReleaseDTO release = new ReleaseDTO();
    release.setConfigurations("{\"flag\":\"true\"}");

    ItemDTO deletedItem = new ItemDTO("flag", "true", "comment", 1);
    deletedItem.setNamespaceId(namespaceId);
    deletedItem.setType(2);

    when(namespaceAPI.loadNamespace(appId, env, clusterName, namespaceName)).thenReturn(namespace);
    when(releaseAPI.loadLatestRelease(appId, env, clusterName, namespaceName)).thenReturn(release);
    when(itemAPI.findItems(appId, env, clusterName, namespaceName))
        .thenReturn(Collections.emptyList());
    when(itemAPI.findDeletedItems(appId, env, clusterName, namespaceName))
        .thenReturn(Collections.singletonList(deletedItem));

    configService.revokeItem(appId, env, clusterName, namespaceName, "test");

    ArgumentCaptor<ItemChangeSets> captor = ArgumentCaptor.forClass(ItemChangeSets.class);
    verify(itemAPI).updateItemsByChangeSet(eq(appId), eq(env), eq(clusterName), eq(namespaceName),
        captor.capture());
    ItemChangeSets changeSets = captor.getValue();
    assertEquals(1, changeSets.getCreateItems().size());
    assertEquals("true", changeSets.getCreateItems().get(0).getValue());
    assertEquals(2, changeSets.getCreateItems().get(0).getType());
  }

  /**
   * a=b b=c c=d
   */
  private List<ItemDTO> mockBaseItemHas3Key() {
    ItemDTO item1 = new ItemDTO("a", "b", "", 1);
    ItemDTO item2 = new ItemDTO("b", "c", "", 2);
    ItemDTO item3 = new ItemDTO("c", "d", "", 3);
    return Arrays.asList(item1, item2, item3);
  }

  @Test
  public void testCompareTargetNamespaceHasNoItems() {
    ItemDTO sourceItem1 = new ItemDTO("a", "b", "comment", 1);
    List<ItemDTO> sourceItems = Collections.singletonList(sourceItem1);

    String appId = "6666", env = "LOCAL", clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT,
        namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    List<NamespaceIdentifier> namespaceIdentifiers =
        generateNamespaceIdentifier(appId, env, clusterName, namespaceName);
    NamespaceDTO namespaceDTO = generateNamespaceDTO(appId, clusterName, namespaceName);

    when(namespaceAPI.loadNamespace(appId, Env.valueOf(env), clusterName, namespaceName))
        .thenReturn(namespaceDTO);
    when(itemAPI.findItems(appId, Env.valueOf(env), clusterName, namespaceName)).thenReturn(null);

    List<ItemDiffs> itemDiffses = configService.compare(namespaceIdentifiers, sourceItems);

    assertEquals(1, itemDiffses.size());
    ItemDiffs itemDiffs = itemDiffses.get(0);
    ItemChangeSets changeSets = itemDiffs.getDiffs();
    assertEquals(0, changeSets.getUpdateItems().size());
    assertEquals(0, changeSets.getDeleteItems().size());

    List<ItemDTO> createItems = changeSets.getCreateItems();
    ItemDTO createItem = createItems.get(0);
    assertEquals(1, createItem.getLineNum());
    assertEquals("a", createItem.getKey());
    assertEquals("b", createItem.getValue());
    assertEquals("comment", createItem.getComment());
  }

  @Test
  public void testCompare() {
    ItemDTO sourceItem1 = new ItemDTO("a", "b", "comment", 1);// not modified
    ItemDTO sourceItem2 = new ItemDTO("newKey", "c", "comment", 2);// new item
    ItemDTO sourceItem3 = new ItemDTO("c", "newValue", "comment", 3);// update value
    ItemDTO sourceItem4 = new ItemDTO("d", "b", "newComment", 4);// update comment
    List<ItemDTO> sourceItems = Arrays.asList(sourceItem1, sourceItem2, sourceItem3, sourceItem4);

    ItemDTO targetItem1 = new ItemDTO("a", "b", "comment", 1);
    ItemDTO targetItem2 = new ItemDTO("c", "oldValue", "comment", 2);
    ItemDTO targetItem3 = new ItemDTO("d", "b", "oldComment", 3);
    List<ItemDTO> targetItems = Arrays.asList(targetItem1, targetItem2, targetItem3);

    String appId = "6666", env = "LOCAL", clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT,
        namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    List<NamespaceIdentifier> namespaceIdentifiers =
        generateNamespaceIdentifier(appId, env, clusterName, namespaceName);
    NamespaceDTO namespaceDTO = generateNamespaceDTO(appId, clusterName, namespaceName);

    when(namespaceAPI.loadNamespace(appId, Env.valueOf(env), clusterName, namespaceName))
        .thenReturn(namespaceDTO);
    when(itemAPI.findItems(appId, Env.valueOf(env), clusterName, namespaceName))
        .thenReturn(targetItems);

    List<ItemDiffs> itemDiffses = configService.compare(namespaceIdentifiers, sourceItems);
    assertEquals(1, itemDiffses.size());

    ItemDiffs itemDiffs = itemDiffses.get(0);

    ItemChangeSets changeSets = itemDiffs.getDiffs();
    assertEquals(0, changeSets.getDeleteItems().size());
    assertEquals(2, changeSets.getUpdateItems().size());
    assertEquals(1, changeSets.getCreateItems().size());

    NamespaceIdentifier namespaceIdentifier = itemDiffs.getNamespace();
    assertEquals(appId, namespaceIdentifier.getAppId());
    assertEquals(Env.valueOf("LOCAL"), namespaceIdentifier.getEnv());
    assertEquals(clusterName, namespaceIdentifier.getClusterName());
    assertEquals(namespaceName, namespaceIdentifier.getNamespaceName());

    ItemDTO createdItem = changeSets.getCreateItems().get(0);
    assertEquals("newKey", createdItem.getKey());
    assertEquals("c", createdItem.getValue());
    assertEquals("comment", createdItem.getComment());
    assertEquals(4, createdItem.getLineNum());

    List<ItemDTO> updateItems = changeSets.getUpdateItems();
    ItemDTO updateItem1 = updateItems.get(0);
    ItemDTO updateItem2 = updateItems.get(1);
    assertEquals("c", updateItem1.getKey());
    assertEquals("newValue", updateItem1.getValue());
    assertEquals("comment", updateItem1.getComment());
    assertEquals(2, updateItem1.getLineNum());

    assertEquals("d", updateItem2.getKey());
    assertEquals("b", updateItem2.getValue());
    assertEquals("newComment", updateItem2.getComment());
    assertEquals(3, updateItem2.getLineNum());


  }

  private NamespaceDTO generateNamespaceDTO(String appId, String clusterName,
      String namespaceName) {
    NamespaceDTO namespaceDTO = new NamespaceDTO();
    namespaceDTO.setAppId(appId);
    namespaceDTO.setId(1);
    namespaceDTO.setClusterName(clusterName);
    namespaceDTO.setNamespaceName(namespaceName);
    return namespaceDTO;
  }

  private List<NamespaceIdentifier> generateNamespaceIdentifier(String appId, String env,
      String clusterName, String namespaceName) {
    NamespaceIdentifier targetNamespace = new NamespaceIdentifier();
    targetNamespace.setAppId(appId);
    targetNamespace.setEnv(env);
    targetNamespace.setClusterName(clusterName);
    targetNamespace.setNamespaceName(namespaceName);
    return Collections.singletonList(targetNamespace);
  }

}
