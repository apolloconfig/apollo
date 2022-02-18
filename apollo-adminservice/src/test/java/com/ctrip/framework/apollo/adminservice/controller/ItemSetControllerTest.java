/*
 * Copyright 2022 Apollo Authors
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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class ItemSetControllerTest extends AbstractControllerTest {

  @Autowired
  ItemRepository itemRepository;

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testItemSetCreated() {
    String appId = "someAppId";
    AppDTO app =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + appId, AppDTO.class);

    ClusterDTO cluster = restTemplate.getForObject(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/default",
        ClusterDTO.class);

    NamespaceDTO namespace =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + app.getAppId()
            + "/clusters/" + cluster.getName() + "/namespaces/application", NamespaceDTO.class);

    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    ItemChangeSets itemSet = new ItemChangeSets();
    itemSet.setDataChangeLastModifiedBy("created");
    RestTemplate createdTemplate = (new TestRestTemplate()).getRestTemplate();
    createdTemplate.setMessageConverters(restTemplate.getMessageConverters());
    
    int createdSize = 3;
    for (int i = 0; i < createdSize; i++) {
      ItemDTO item = new ItemDTO();
      item.setNamespaceId(namespace.getId());
      item.setKey("key_" + i);
      item.setValue("created_value_" + i);
      itemSet.addCreateItem(item);
    }

    ResponseEntity<Void> response =
        createdTemplate.postForEntity(
            "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/"
                + cluster.getName() + "/namespaces/" + namespace.getNamespaceName() + "/itemset",
            itemSet, Void.class);
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
  public void testItemSetUpdated() {
    String appId = "someAppId";
    AppDTO app =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + appId, AppDTO.class);

    ClusterDTO cluster = restTemplate.getForObject(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/default",
        ClusterDTO.class);

    NamespaceDTO namespace =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + app.getAppId()
            + "/clusters/" + cluster.getName() + "/namespaces/application", NamespaceDTO.class);

    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    ItemChangeSets createChangeSet = new ItemChangeSets();
    createChangeSet.setDataChangeLastModifiedBy("created");
    RestTemplate createdRestTemplate = (new TestRestTemplate()).getRestTemplate();
    createdRestTemplate.setMessageConverters(restTemplate.getMessageConverters());
    
    int createdSize = 3;
    for (int i = 0; i < createdSize; i++) {
      ItemDTO item = new ItemDTO();
      item.setNamespaceId(namespace.getId());
      item.setKey("key_" + i);
      item.setValue("created_value_" + i);
      createChangeSet.addCreateItem(item);
    }

    ResponseEntity<Void> response = createdRestTemplate.postForEntity(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/" + cluster.getName()
            + "/namespaces/" + namespace.getNamespaceName() + "/itemset",
        createChangeSet, Void.class);
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

    ItemDTO[] items =
        createdRestTemplate.getForObject(
            "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/"
                + cluster.getName() + "/namespaces/" + namespace.getNamespaceName() + "/items",
            ItemDTO[].class);

    ItemChangeSets updateChangeSet = new ItemChangeSets();
    updateChangeSet.setDataChangeLastModifiedBy("updated");

    RestTemplate updatedRestTemplate = (new TestRestTemplate()).getRestTemplate();
    updatedRestTemplate.setMessageConverters(restTemplate.getMessageConverters());
    
    int updatedSize = 2;
    for (int i = 0; i < updatedSize; i++) {
      items[i].setValue("updated_value_" + i);
      updateChangeSet.addUpdateItem(items[i]);
    }

    response = updatedRestTemplate.postForEntity(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/" + cluster.getName()
            + "/namespaces/" + namespace.getNamespaceName() + "/itemset",
        updateChangeSet, Void.class);
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
  public void testItemSetDeleted() {
    String appId = "someAppId";
    AppDTO app =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + appId, AppDTO.class);

    ClusterDTO cluster = restTemplate.getForObject(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/default",
        ClusterDTO.class);

    NamespaceDTO namespace =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + app.getAppId()
            + "/clusters/" + cluster.getName() + "/namespaces/application", NamespaceDTO.class);

    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    ItemChangeSets createChangeSet = new ItemChangeSets();
    createChangeSet.setDataChangeLastModifiedBy("created");
    RestTemplate createdTemplate = (new TestRestTemplate()).getRestTemplate();
    createdTemplate.setMessageConverters(restTemplate.getMessageConverters());
    
    int createdSize = 3;
    for (int i = 0; i < createdSize; i++) {
      ItemDTO item = new ItemDTO();
      item.setNamespaceId(namespace.getId());
      item.setKey("key_" + i);
      item.setValue("created_value_" + i);
      createChangeSet.addCreateItem(item);
    }

    ResponseEntity<Void> response = createdTemplate.postForEntity(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/" + cluster.getName()
            + "/namespaces/" + namespace.getNamespaceName() + "/itemset",
        createChangeSet, Void.class);
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());

    ItemDTO[] items =
        restTemplate.getForObject(
            "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/"
                + cluster.getName() + "/namespaces/" + namespace.getNamespaceName() + "/items",
            ItemDTO[].class);

    ItemChangeSets deleteChangeSet = new ItemChangeSets();
    deleteChangeSet.setDataChangeLastModifiedBy("deleted");
    RestTemplate deletedTemplate = (new TestRestTemplate()).getRestTemplate();
    deletedTemplate.setMessageConverters(restTemplate.getMessageConverters());
    
    int deletedSize = 1;
    for (int i = 0; i < deletedSize; i++) {
      items[i].setValue("deleted_value_" + i);
      deleteChangeSet.addDeleteItem(items[i]);
    }

    response = deletedTemplate.postForEntity(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/" + cluster.getName()
            + "/namespaces/" + namespace.getNamespaceName() + "/itemset",
        deleteChangeSet, Void.class);
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    List<Item> savedItems = itemRepository.findByNamespaceIdOrderByLineNumAsc(namespace.getId());
    Assert.assertEquals(createdSize - deletedSize, savedItems.size());
    Item item0 = savedItems.get(0);
    Assert.assertEquals("key_1", item0.getKey());
    Assert.assertEquals("created_value_1", item0.getValue());
    Assert.assertEquals("created", item0.getDataChangeCreatedBy());
    Assert.assertNotNull(item0.getDataChangeCreatedTime());
  }
}
