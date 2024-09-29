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
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

public class ItemSetServiceTest extends AbstractIntegrationTest {

  @Mock
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

    ReflectionTestUtils.setField(itemSetService, "bizConfig", bizConfig);
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

    ReflectionTestUtils.setField(itemSetService, "bizConfig", bizConfig);
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

    ReflectionTestUtils.setField(itemSetService, "bizConfig", bizConfig);
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
