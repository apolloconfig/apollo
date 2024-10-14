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
package com.ctrip.framework.apollo.biz.service;

import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

public class ItemSetServiceTest extends AbstractIntegrationTest {

  @MockBean
  private BizConfig bizConfig;

  @Autowired
  private ItemService itemService;
  @Autowired
  private NamespaceService namespaceService;

  @Autowired
  private ItemSetService itemSetService;

  @Test
  @Sql(scripts = "/sql/itemset-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testUpdateSetWithoutItemNumLimit() {

    when(bizConfig.itemKeyLengthLimit()).thenReturn(128);
    when(bizConfig.itemValueLengthLimit()).thenReturn(20000);

    when(bizConfig.isItemNumLimitEnabled()).thenReturn(false);
    when(bizConfig.itemNumLimit()).thenReturn(5);

    Namespace namespace = namespaceService.findOne(1L);

    ItemChangeSets changeSets = new ItemChangeSets();
    changeSets.addCreateItem(buildNormalItem(0L, namespace.getId(), "k6", "v6", "test item num limit", 6));
    changeSets.addCreateItem(buildNormalItem(0L, namespace.getId(), "k7", "v7", "test item num limit", 7));

    try {
      itemSetService.updateSet(namespace, changeSets);
    } catch (Exception e) {
      Assert.fail();
    }

    int size = itemService.findNonEmptyItemCount(namespace.getId());
    Assert.assertEquals(7, size);

  }

  @Test
  @Sql(scripts = "/sql/itemset-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testUpdateSetWithItemNumLimit() {

    when(bizConfig.itemKeyLengthLimit()).thenReturn(128);
    when(bizConfig.itemValueLengthLimit()).thenReturn(20000);

    when(bizConfig.isItemNumLimitEnabled()).thenReturn(true);
    when(bizConfig.itemNumLimit()).thenReturn(5);

    Namespace namespace = namespaceService.findOne(1L);
    Item item9901 = itemService.findOne(9901);
    Item item9902 = itemService.findOne(9902);

    ItemChangeSets changeSets = new ItemChangeSets();
    changeSets.addUpdateItem(buildNormalItem(item9901.getId(), item9901.getNamespaceId(), item9901.getKey(), item9901.getValue() + " update", item9901.getComment(), item9901.getLineNum()));
    changeSets.addDeleteItem(buildNormalItem(item9902.getId(), item9902.getNamespaceId(), item9902.getKey(), item9902.getValue() + " update", item9902.getComment(), item9902.getLineNum()));
    changeSets.addCreateItem(buildNormalItem(0L, item9901.getNamespaceId(), "k6", "v6", "test item num limit", 6));
    changeSets.addCreateItem(buildNormalItem(0L, item9901.getNamespaceId(), "k7", "v7", "test item num limit", 7));

    try {
      itemSetService.updateSet(namespace, changeSets);
      Assert.fail();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof BadRequestException);
    }

    int size = itemService.findNonEmptyItemCount(namespace.getId());
    Assert.assertEquals(5, size);

  }

  @Test
  @Sql(scripts = "/sql/itemset-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testUpdateSetWithItemNumLimit2() {

    when(bizConfig.itemKeyLengthLimit()).thenReturn(128);
    when(bizConfig.itemValueLengthLimit()).thenReturn(20000);

    when(bizConfig.isItemNumLimitEnabled()).thenReturn(true);
    when(bizConfig.itemNumLimit()).thenReturn(5);

    Namespace namespace = namespaceService.findOne(1L);
    Item item9901 = itemService.findOne(9901);
    Item item9902 = itemService.findOne(9902);

    ItemChangeSets changeSets = new ItemChangeSets();
    changeSets.addUpdateItem(buildNormalItem(item9901.getId(), item9901.getNamespaceId(), item9901.getKey(), item9901.getValue() + " update", item9901.getComment(), item9901.getLineNum()));
    changeSets.addDeleteItem(buildNormalItem(item9902.getId(), item9902.getNamespaceId(), item9902.getKey(), item9902.getValue() + " update", item9902.getComment(), item9902.getLineNum()));
    changeSets.addCreateItem(buildNormalItem(0L, item9901.getNamespaceId(), "k6", "v6", "test item num limit", 6));

    try {
      itemSetService.updateSet(namespace, changeSets);
    } catch (Exception e) {
      Assert.fail();
    }

    int size = itemService.findNonEmptyItemCount(namespace.getId());
    Assert.assertEquals(5, size);

  }


  private ItemDTO buildNormalItem(Long id, Long namespaceId, String key, String value, String comment, int lineNum) {
    ItemDTO item = new ItemDTO(key, value, comment, lineNum);
    item.setId(id);
    item.setNamespaceId(namespaceId);
    return item;
  }

}
