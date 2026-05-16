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
package com.ctrip.framework.apollo.openapi.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.model.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemDiffDTO;
import com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceIdentifier;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceSyncDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.entity.vo.ItemDiffs;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for ServerItemOpenApiService item conversion and delegation behavior.
 */
@ExtendWith(MockitoExtension.class)
class ServerItemOpenApiServiceTest {

  private static final String APP_ID = "app-1";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";

  @Mock
  private ItemService itemService;

  private ServerItemOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerItemOpenApiService(itemService);
  }

  @Test
  void findItemsByNamespaceShouldConvertLegacyOpenItemPage() {
    com.ctrip.framework.apollo.openapi.dto.OpenItemDTO legacyItem =
        new com.ctrip.framework.apollo.openapi.dto.OpenItemDTO();
    legacyItem.setKey("timeout");
    legacyItem.setValue("100");
    legacyItem.setComment("comment");
    legacyItem.setDataChangeCreatedBy("apollo");
    PageDTO<com.ctrip.framework.apollo.openapi.dto.OpenItemDTO> page =
        new PageDTO<>(Collections.singletonList(legacyItem), PageRequest.of(1, 20), 23);
    when(itemService.findItemsByNamespace(APP_ID, Env.valueOf(ENV), CLUSTER, NAMESPACE, 1, 20))
        .thenReturn(page);

    OpenItemPageDTO result = service.findItemsByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, 1, 20);

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getTotal()).isEqualTo(23);
    assertThat(result.getContent()).extracting(OpenItemDTO::getKey).containsExactly("timeout");
  }

  @Test
  void batchUpdateItemsByTextShouldStampPathFieldsAndDelegate() {
    OpenNamespaceTextModel model = new OpenNamespaceTextModel();
    model.setNamespaceId(10L);
    model.setFormat("properties");
    model.setConfigText("timeout=100");

    service.batchUpdateItemsByText(APP_ID, ENV, CLUSTER, NAMESPACE, model, "operator");

    ArgumentCaptor<NamespaceTextModel> captor = ArgumentCaptor.forClass(NamespaceTextModel.class);
    verify(itemService).updateConfigItemByText(captor.capture(), eq("operator"));
    NamespaceTextModel delegated = captor.getValue();
    assertThat(delegated.getAppId()).isEqualTo(APP_ID);
    assertThat(delegated.getEnv()).isEqualTo(Env.valueOf(ENV));
    assertThat(delegated.getClusterName()).isEqualTo(CLUSTER);
    assertThat(delegated.getNamespaceName()).isEqualTo(NAMESPACE);
    assertThat(delegated.getNamespaceId()).isEqualTo(10L);
  }

  @Test
  void compareItemsShouldConvertLegacyDiffsToGeneratedFlatDto() {
    NamespaceIdentifier namespace = namespaceIdentifier("FAT", "cluster-a", NAMESPACE);
    ItemChangeSets changeSets = new ItemChangeSets();
    changeSets.addCreateItem(item("created", "1"));
    changeSets.addUpdateItem(item("updated", "2"));
    changeSets.addDeleteItem(item("deleted", "3"));
    ItemDiffs diff = new ItemDiffs(namespace);
    diff.setDiffs(changeSets);
    diff.setExtInfo("hidden");
    when(itemService.compare(any(), any())).thenReturn(Collections.singletonList(diff));

    OpenNamespaceSyncDTO request = new OpenNamespaceSyncDTO();
    request.setSyncToNamespaces(
        Collections.singletonList(openNamespaceIdentifier("FAT", "cluster-a", NAMESPACE)));
    request.setSyncItems(Collections.singletonList(openItem("source", "0")));

    List<OpenItemDiffDTO> result = service.compareItems(APP_ID, ENV, CLUSTER, NAMESPACE, request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getNamespace().getEnv()).isEqualTo("FAT");
    assertThat(result.get(0).getCreateItems()).extracting(OpenItemDTO::getKey)
        .containsExactly("created");
    assertThat(result.get(0).getUpdateItems()).extracting(OpenItemDTO::getKey)
        .containsExactly("updated");
    assertThat(result.get(0).getDeleteItems()).extracting(OpenItemDTO::getKey)
        .containsExactly("deleted");
    assertThat(result.get(0).getMessage()).isEqualTo("hidden");
  }

  @Test
  void syncItemsShouldConvertGeneratedPayloadAndDelegateWithOperator() {
    OpenNamespaceSyncDTO request = new OpenNamespaceSyncDTO();
    request.setSyncToNamespaces(
        Collections.singletonList(openNamespaceIdentifier("FAT", "cluster-a", NAMESPACE)));
    request.setSyncItems(Collections.singletonList(openItem("timeout", "100")));

    service.syncItems(APP_ID, ENV, CLUSTER, NAMESPACE, request, "operator");

    ArgumentCaptor<List<NamespaceIdentifier>> namespaceCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<ItemDTO>> itemCaptor = ArgumentCaptor.forClass(List.class);
    verify(itemService).syncItems(namespaceCaptor.capture(), itemCaptor.capture(), eq("operator"));
    assertThat(namespaceCaptor.getValue()).extracting(NamespaceIdentifier::getClusterName)
        .containsExactly("cluster-a");
    assertThat(itemCaptor.getValue()).extracting(ItemDTO::getKey).containsExactly("timeout");
  }

  @Test
  void revertItemsShouldDelegateToItemService() {
    service.revertItems(APP_ID, ENV, CLUSTER, NAMESPACE, "operator");

    verify(itemService).revokeItem(APP_ID, Env.valueOf(ENV), CLUSTER, NAMESPACE, "operator");
  }

  @Test
  void updateItemShouldLoadExistingItemAndPreserveIdentityFields() {
    ItemDTO existing = item("timeout", "100");
    existing.setId(99);
    existing.setNamespaceId(10);
    existing.setLineNum(7);
    when(itemService.loadItem(Env.valueOf(ENV), APP_ID, CLUSTER, NAMESPACE, "timeout"))
        .thenReturn(existing);

    OpenItemDTO request = openItem("timeout", "200");
    request.setComment("new comment");

    service.updateItem(APP_ID, ENV, CLUSTER, NAMESPACE, request, "operator");

    ArgumentCaptor<ItemDTO> captor = ArgumentCaptor.forClass(ItemDTO.class);
    verify(itemService).updateItem(eq(APP_ID), eq(Env.valueOf(ENV)), eq(CLUSTER), eq(NAMESPACE),
        captor.capture());
    ItemDTO delegated = captor.getValue();
    assertThat(delegated.getId()).isEqualTo(99);
    assertThat(delegated.getNamespaceId()).isEqualTo(10);
    assertThat(delegated.getLineNum()).isEqualTo(7);
    assertThat(delegated.getValue()).isEqualTo("200");
    assertThat(delegated.getComment()).isEqualTo("new comment");
    assertThat(delegated.getDataChangeLastModifiedBy()).isEqualTo("operator");
  }

  @Test
  void createOrUpdateItemShouldCreateWhenExistingItemIsMissing() {
    when(itemService.loadItem(Env.valueOf(ENV), APP_ID, CLUSTER, NAMESPACE, "timeout"))
        .thenReturn(null);
    OpenItemDTO request = openItem("timeout", "100");

    service.createOrUpdateItem(APP_ID, ENV, CLUSTER, NAMESPACE, request, "operator");

    verify(itemService).createItem(eq(APP_ID), eq(Env.valueOf(ENV)), eq(CLUSTER), eq(NAMESPACE),
        any(ItemDTO.class));
  }

  @Test
  void createOrUpdateItemShouldUpdateExistingItemAndPreserveIdentityFields() {
    ItemDTO existing = item("timeout", "100");
    existing.setId(99);
    existing.setNamespaceId(10);
    when(itemService.loadItem(Env.valueOf(ENV), APP_ID, CLUSTER, NAMESPACE, "timeout"))
        .thenReturn(existing);
    OpenItemDTO request = openItem("timeout", "200");

    service.createOrUpdateItem(APP_ID, ENV, CLUSTER, NAMESPACE, request, "operator");

    verify(itemService, never()).createItem(eq(APP_ID), eq(Env.valueOf(ENV)), eq(CLUSTER),
        eq(NAMESPACE), any(ItemDTO.class));
    ArgumentCaptor<ItemDTO> captor = ArgumentCaptor.forClass(ItemDTO.class);
    verify(itemService).updateItem(eq(APP_ID), eq(Env.valueOf(ENV)), eq(CLUSTER), eq(NAMESPACE),
        captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(99);
    assertThat(captor.getValue().getNamespaceId()).isEqualTo(10);
    assertThat(captor.getValue().getValue()).isEqualTo("200");
  }

  @Test
  void createOrUpdateItemShouldFallbackToUpdateWhenCreateReportsDuplicate() {
    ItemDTO existing = item("timeout", "100");
    existing.setId(99);
    existing.setNamespaceId(10);
    when(itemService.loadItem(Env.valueOf(ENV), APP_ID, CLUSTER, NAMESPACE, "timeout"))
        .thenReturn(null, existing);
    when(itemService.createItem(eq(APP_ID), eq(Env.valueOf(ENV)), eq(CLUSTER), eq(NAMESPACE),
        any(ItemDTO.class))).thenThrow(BadRequestException.itemAlreadyExists("timeout"));
    OpenItemDTO request = openItem("timeout", "200");

    service.createOrUpdateItem(APP_ID, ENV, CLUSTER, NAMESPACE, request, "operator");

    ArgumentCaptor<ItemDTO> captor = ArgumentCaptor.forClass(ItemDTO.class);
    verify(itemService).updateItem(eq(APP_ID), eq(Env.valueOf(ENV)), eq(CLUSTER), eq(NAMESPACE),
        captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(99);
    assertThat(captor.getValue().getNamespaceId()).isEqualTo(10);
    assertThat(captor.getValue().getValue()).isEqualTo("200");
  }

  private static ItemDTO item(String key, String value) {
    ItemDTO item = new ItemDTO();
    item.setKey(key);
    item.setValue(value);
    item.setComment("comment");
    item.setType(0);
    return item;
  }

  private static OpenItemDTO openItem(String key, String value) {
    OpenItemDTO item = new OpenItemDTO();
    item.setKey(key);
    item.setValue(value);
    item.setComment("comment");
    item.setType(0);
    return item;
  }

  private static NamespaceIdentifier namespaceIdentifier(String env, String clusterName,
      String namespaceName) {
    NamespaceIdentifier namespaceIdentifier = new NamespaceIdentifier();
    namespaceIdentifier.setAppId(APP_ID);
    namespaceIdentifier.setEnv(env);
    namespaceIdentifier.setClusterName(clusterName);
    namespaceIdentifier.setNamespaceName(namespaceName);
    return namespaceIdentifier;
  }

  private static OpenNamespaceIdentifier openNamespaceIdentifier(String env, String clusterName,
      String namespaceName) {
    OpenNamespaceIdentifier namespaceIdentifier = new OpenNamespaceIdentifier();
    namespaceIdentifier.setAppId(APP_ID);
    namespaceIdentifier.setEnv(env);
    namespaceIdentifier.setClusterName(clusterName);
    namespaceIdentifier.setNamespaceName(namespaceName);
    return namespaceIdentifier;
  }
}
