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

import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.repository.ItemRepository;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

import java.util.List;
import org.springframework.web.client.HttpClientErrorException;

public class ItemSetControllerTest extends AbstractControllerTest {

  @Autowired
  ItemRepository itemRepository;

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetCreated() {
    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);

    Assert.assertNotNull(app);
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");

    Assert.assertNotNull(cluster);
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
            NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    Assert.assertNotNull(namespace);
    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    int createdSize = 3;
    ItemChangeSets itemSet = mockCreateItemChangeSets(namespace, createdSize);

    ResponseEntity<Void> response = restTemplate.postForEntity(itemSetBaseUrl(),
            itemSet, Void.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    List<Item> items = itemRepository.findByNamespaceIdOrderByLineNumAsc(namespace.getId());
    Assert.assertEquals(createdSize, items.size());
    Item item0 = items.get(0);
    Assert.assertEquals("key_0", item0.getKey());
    Assert.assertEquals("created_value_0", item0.getValue());
    Assert.assertEquals("created", item0.getDataChangeCreatedBy());
    Assert.assertNotNull(item0.getDataChangeCreatedTime());
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetCreatedWithInvalidNamespaceId() {
    String appId = "someAppId";
    String clusterName = "default";
    String namespaceName = "application";
    String someNamespaceName = "someNamespace";

    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, appId, clusterName, namespaceName);

    NamespaceDTO someNamespace =
        restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, appId, clusterName, someNamespaceName);

    Assert.assertNotNull(someNamespace);
    long someNamespaceId = someNamespace.getId();

    int createdSize = 3;
    ItemChangeSets itemSet = mockCreateItemChangeSets(namespace, createdSize);
    itemSet.getCreateItems().get(createdSize - 1).setNamespaceId(someNamespaceId);

    try {
          restTemplate.postForEntity(itemSetBaseUrl(), itemSet, Void.class, appId, clusterName, namespaceName);
    } catch (HttpClientErrorException e) {
      Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Assert.assertTrue(
          Objects.requireNonNull(e.getMessage()).contains(BadRequestException.namespaceNotMatch().getMessage()));
      Assert.assertTrue(e.getMessage().contains(BadRequestException.class.getName()));
    }
    List<Item> items = itemRepository.findByNamespaceIdOrderByLineNumAsc(someNamespaceId);
    Assert.assertEquals(0, items.size());
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetUpdated() {
    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);

    Assert.assertNotNull(app);
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");

    Assert.assertNotNull(cluster);
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
        NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    Assert.assertNotNull(namespace);
    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    int createdSize = 3;
    ItemChangeSets createChangeSet = mockCreateItemChangeSets(namespace, createdSize);

    ResponseEntity<Void> response = restTemplate.postForEntity(itemSetBaseUrl(),
        createChangeSet, Void.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

    ItemDTO[] items = restTemplate.getForObject(itemBaseUrl(),
            ItemDTO[].class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());

    ItemChangeSets updateChangeSet = new ItemChangeSets();
    updateChangeSet.setDataChangeLastModifiedBy("updated");

    int updatedSize = 2;
    for (int i = 0; i < updatedSize; i++) {
      items[i].setValue("updated_value_" + i);
      updateChangeSet.addUpdateItem(items[i]);
    }

    response = restTemplate.postForEntity(itemSetBaseUrl(),
        updateChangeSet, Void.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    List<Item> savedItems = itemRepository.findByNamespaceIdOrderByLineNumAsc(namespace.getId());
    Assert.assertEquals(createdSize, savedItems.size());
    Item item0 = savedItems.get(0);
    Assert.assertEquals("key_0", item0.getKey());
    Assert.assertEquals("updated_value_0", item0.getValue());
    Assert.assertEquals("created", item0.getDataChangeCreatedBy());
    Assert.assertEquals("updated", item0.getDataChangeLastModifiedBy());
    Assert.assertNotNull(item0.getDataChangeCreatedTime());
    Assert.assertNotNull(item0.getDataChangeLastModifiedTime());
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetUpdatedWithInvalidNamespaceId() {
    String appId = "someAppId";
    String clusterName = "default";
    String namespaceName = "application";
    String someNamespaceName = "someNamespace";

    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, appId, clusterName, namespaceName);
    NamespaceDTO someNamespace = restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, appId, clusterName, someNamespaceName);

    int createdSize = 3;
    ItemChangeSets createChangeSet = mockCreateItemChangeSets(namespace, createdSize);

    Assert.assertNotNull(namespace);
    ResponseEntity<Void> response = restTemplate.postForEntity(itemSetBaseUrl(),
        createChangeSet, Void.class, appId, clusterName, namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

    ItemDTO[] items =
        restTemplate.getForObject(itemBaseUrl(),
            ItemDTO[].class, appId, clusterName, namespace.getNamespaceName());

    ItemChangeSets updateChangeSet = new ItemChangeSets();
    updateChangeSet.setDataChangeLastModifiedBy("updated");

    int updatedSize = 2;
    for (int i = 0; i < updatedSize; i++) {
      items[i].setValue("updated_value_" + i);
      updateChangeSet.addUpdateItem(items[i]);
    }

    try {
       restTemplate.postForEntity(itemSetBaseUrl(), updateChangeSet, Void.class, appId, clusterName, someNamespaceName);
    } catch (HttpClientErrorException e) {
      Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Assert.assertTrue(
          Objects.requireNonNull(e.getMessage()).contains(BadRequestException.namespaceNotMatch().getMessage()));
      Assert.assertTrue(e.getMessage().contains(BadRequestException.class.getName()));
    }

    List<Item> savedItems = itemRepository.findByNamespaceIdOrderByLineNumAsc(someNamespace.getId());
    Assert.assertEquals(0, savedItems.size());
  }

  private ItemChangeSets mockCreateItemChangeSets(NamespaceDTO namespace, int createdSize) {
    ItemChangeSets createChangeSet = new ItemChangeSets();
    createChangeSet.setDataChangeLastModifiedBy("created");

    for (int i = 0; i < createdSize; i++) {
      ItemDTO item = new ItemDTO();
      item.setNamespaceId(namespace.getId());
      item.setKey("key_" + i);
      item.setValue("created_value_" + i);
      createChangeSet.addCreateItem(item);
    }
    return createChangeSet;
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetDeleted() {
    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);

    Assert.assertNotNull(app);
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");

    Assert.assertNotNull(cluster);
    NamespaceDTO namespace =
        restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    Assert.assertNotNull(namespace);
    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    int createdSize = 3;
    ItemChangeSets createChangeSet = mockCreateItemChangeSets(namespace, createdSize);

    ResponseEntity<Void> response = restTemplate.postForEntity(itemSetBaseUrl(),
        createChangeSet, Void.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

    ItemDTO[] items = restTemplate.getForObject(itemBaseUrl(),
            ItemDTO[].class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());

    ItemChangeSets deleteChangeSet = new ItemChangeSets();
    deleteChangeSet.setDataChangeLastModifiedBy("deleted");

    int deletedSize = 1;
    for (int i = 0; i < deletedSize; i++) {
      items[i].setValue("deleted_value_" + i);
      deleteChangeSet.addDeleteItem(items[i]);
    }

    response = restTemplate.postForEntity(itemSetBaseUrl(),
        deleteChangeSet, Void.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    List<Item> savedItems = itemRepository.findByNamespaceIdOrderByLineNumAsc(namespace.getId());
    Assert.assertEquals(createdSize - deletedSize, savedItems.size());
    Item item0 = savedItems.get(0);
    Assert.assertEquals("key_1", item0.getKey());
    Assert.assertEquals("created_value_1", item0.getValue());
    Assert.assertEquals("created", item0.getDataChangeCreatedBy());
    Assert.assertNotNull(item0.getDataChangeCreatedTime());
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetDeletedWithInvalidNamespaceId() {
    String appId = "someAppId";
    String clusterName = "default";
    String namespaceName = "application";
    String someNamespaceName = "someNamespace";

    NamespaceDTO namespace =
        restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, appId, clusterName, namespaceName);

    int createdSize = 3;
    ItemChangeSets createChangeSet = mockCreateItemChangeSets(namespace, createdSize);

    Assert.assertNotNull(namespace);
    ResponseEntity<Void> response = restTemplate.postForEntity(itemSetBaseUrl(),
        createChangeSet, Void.class, appId, clusterName, namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

    ItemDTO[] items = restTemplate.getForObject(itemBaseUrl(),
            ItemDTO[].class, appId, clusterName, namespace.getNamespaceName());

    ItemChangeSets deleteChangeSet = new ItemChangeSets();
    deleteChangeSet.setDataChangeLastModifiedBy("deleted");

    int deletedSize = 1;
    for (int i = 0; i < deletedSize; i++) {
      items[i].setValue("deleted_value_" + i);
      deleteChangeSet.addDeleteItem(items[i]);
    }

    try {
      restTemplate.postForEntity(itemSetBaseUrl(), deleteChangeSet, Void.class, appId, clusterName, someNamespaceName);
    } catch (HttpClientErrorException e) {
      Assert.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      Assert.assertTrue(
          Objects.requireNonNull(e.getMessage()).contains(BadRequestException.namespaceNotMatch().getMessage()));
      Assert.assertTrue(e.getMessage().contains(BadRequestException.class.getName()));
    }

    List<Item> savedItems = itemRepository.findByNamespaceIdOrderByLineNumAsc(namespace.getId());
    Assert.assertEquals(createdSize, savedItems.size());
  }

  private String itemSetBaseUrl() {
    return url("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/itemset");
  }
}
