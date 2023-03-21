package com.ctrip.framework.apollo.adminservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.repository.CommitRepository;
import com.ctrip.framework.apollo.biz.repository.ItemRepository;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import java.util.List;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.client.RestTemplate;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/21
 */
public class ItemControllerTest extends AbstractControllerTest {

  @Autowired
  private CommitRepository commitRepository;

  @Autowired
  private ItemRepository itemRepository;

  @Test
  @Sql(scripts = "/controller/test-itemset.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreate(){
    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);
    assert app != null;
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");
    assert cluster != null;
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
        NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    RestTemplate createdTemplate = (new TestRestTemplate()).getRestTemplate();
    createdTemplate.setMessageConverters(restTemplate.getMessageConverters());

    String itemKey = "test-key";
    String itemValue = "test-value";
    ItemDTO item = new ItemDTO(itemKey, itemValue, "", 1);
    assert namespace != null;
    item.setNamespaceId(namespace.getId());
    item.setDataChangeLastModifiedBy("apollo");

    ResponseEntity<ItemDTO> response = createdTemplate.postForEntity(itemBaseUrl(),
        item, ItemDTO.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(itemKey, Objects.requireNonNull(response.getBody()).getKey());

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
  public void testUpdate(){
    this.testCreate();

    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);
    assert app != null;
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");
    assert cluster != null;
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
        NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    RestTemplate updateTemplate = (new TestRestTemplate()).getRestTemplate();
    updateTemplate.setMessageConverters(restTemplate.getMessageConverters());

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
    updateTemplate.put(updateUrl, item, app.getAppId(), cluster.getName(), namespace.getNamespaceName(), itemId);

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
  public void testDelete(){
    this.testCreate();

    String appId = "someAppId";
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);
    assert app != null;
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");
    assert cluster != null;
    NamespaceDTO namespace = restTemplate.getForObject(namespaceBaseUrl(),
        NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    RestTemplate delTemplate = (new TestRestTemplate()).getRestTemplate();
    delTemplate.setMessageConverters(restTemplate.getMessageConverters());

    String itemKey = "test-key";

    long itemId = itemRepository.findByKey(itemKey, Pageable.ofSize(1))
        .getContent()
        .get(0)
        .getId();

    String deleteUrl = url(  "/items/{itemId}?operator=apollo");
    delTemplate.delete(deleteUrl, itemId);
    assertThat(itemRepository.findById(itemId).isPresent())
        .isFalse();

    assert namespace != null;
    List<Commit> commitList = commitRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(app.getAppId(), cluster.getName(), namespace.getNamespaceName(),
        Pageable.ofSize(10));
    assertThat(commitList).hasSize(2);
  }
}
