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

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.repository.CommitRepository;
import com.ctrip.framework.apollo.biz.repository.ItemRepository;
import com.ctrip.framework.apollo.biz.service.ItemService;
import com.ctrip.framework.apollo.common.dto.*;

import java.util.List;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/21
 */
public class ItemControllerTest extends AbstractControllerTest {

  @Autowired
  private CommitRepository commitRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private ItemService itemService;

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreate() {
    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);
    assert app != null;
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");
    assert cluster != null;
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
            NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    String itemKey = "test-key";
    String itemValue = "test-value";
    ItemDTO item = new ItemDTO(itemKey, itemValue, "", 1);
    assert namespace != null;
    item.setNamespaceId(namespace.getId());
    item.setDataChangeLastModifiedBy("apollo");

    ResponseEntity<ItemDTO> response = restTemplate.postForEntity(itemBaseUrl(),
            item, ItemDTO.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assert.assertEquals(itemKey, Objects.requireNonNull(response.getBody()).getKey());

    List<Commit> commitList = commitRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(app.getAppId(), cluster.getName(), namespace.getNamespaceName(),
            Pageable.ofSize(10));
    Assert.assertEquals(1, commitList.size());

    Commit commit = commitList.get(0);
    Assert.assertTrue(commit.getChangeSets().contains(itemKey));
    Assert.assertTrue(commit.getChangeSets().contains(itemValue));
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testUpdate() {
    this.testCreate();

    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);
    assert app != null;
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");
    assert cluster != null;
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
            NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    String itemKey = "test-key";
    String itemValue = "test-value-updated";

    long itemId = itemRepository.findByKey(itemKey, Pageable.ofSize(1))
            .getContent()
            .get(0)
            .getId();
    ItemDTO item = new ItemDTO(itemKey, itemValue, "", 1);
    item.setDataChangeLastModifiedBy("apollo");

    String updateUrl = url(  "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{itemId}");
    assert namespace != null;
    restTemplate.put(updateUrl, item, app.getAppId(), cluster.getName(), namespace.getNamespaceName(), itemId);

    itemRepository.findById(itemId).ifPresent(item1 -> {
      assertThat(item1.getValue()).isEqualTo(itemValue);
      assertThat(item1.getKey()).isEqualTo(itemKey);
    });

    List<Commit> commitList = commitRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(app.getAppId(), cluster.getName(), namespace.getNamespaceName(),
            Pageable.ofSize(10));
    assertThat(commitList).hasSize(2);
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testDelete() {
    this.testCreate();

    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);
    assert app != null;
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");
    assert cluster != null;
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
            NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    String itemKey = "test-key";

    long itemId = itemRepository.findByKey(itemKey, Pageable.ofSize(1))
            .getContent()
            .get(0)
            .getId();

    String deleteUrl = url(  "/items/{itemId}?operator=apollo");
    restTemplate.delete(deleteUrl, itemId);
    assertThat(itemRepository.findById(itemId).isPresent())
            .isFalse();

    assert namespace != null;
    List<Commit> commitList = commitRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(app.getAppId(), cluster.getName(), namespace.getNamespaceName(),
            Pageable.ofSize(10));
    assertThat(commitList).hasSize(2);
  }

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testSearch() {
    this.testCreate();

    String itemKey = "test-key";
    String itemValue = "test-value";
    Page<ItemInfoDTO> itemInfoDTOS = itemService.getItemInfoBySearch(itemKey, itemValue, PageRequest.of(0, 200));
    HttpHeaders headers = new HttpHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<PageDTO<ItemInfoDTO>> response = restTemplate.exchange(
            url("/items-search/key-and-value?key={key}&value={value}&page={page}&size={size}"),
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<PageDTO<ItemInfoDTO>>() {},
            itemKey, itemValue, 0, 200
    );
    assertThat(itemInfoDTOS.getContent().toString()).isEqualTo(response.getBody().getContent().toString());
  }
}
