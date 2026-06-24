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
package com.ctrip.framework.apollo.biz.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.ctrip.framework.apollo.biz.MockBeanFactory;
import com.ctrip.framework.apollo.biz.entity.Item;

/**
 * @author jian.tan
 */

public class ConfigChangeContentBuilderTest {

  private ConfigChangeContentBuilder configChangeContentBuilder;
  private String configString;
  private Item createdItem;
  private Item updatedItem;
  private Item updatedItemFalseCheck;
  private Item createdItemFalseCheck;

  @Before
  public void initConfig() {
    configChangeContentBuilder = new ConfigChangeContentBuilder();
    createdItem = MockBeanFactory.mockItem(1, 1, "timeout", "100", 1);
    updatedItem = MockBeanFactory.mockItem(1, 1, "timeout", "1001", 1);
    updatedItemFalseCheck = MockBeanFactory.mockItem(1, 1, "timeout", "100", 1);
    createdItemFalseCheck = MockBeanFactory.mockItem(1, 1, "", "100", 1);
    configChangeContentBuilder.createItem(createdItem);
    configChangeContentBuilder.createItem(createdItemFalseCheck);
    configChangeContentBuilder.updateItem(createdItem, updatedItem);
    configChangeContentBuilder.updateItem(createdItem, updatedItemFalseCheck);
    configChangeContentBuilder.deleteItem(updatedItem);
    configChangeContentBuilder.deleteItem(createdItemFalseCheck);
    configString = configChangeContentBuilder.build();
  }

  @Test
  public void testHasContent() {
    assertTrue(configChangeContentBuilder.hasContent());
    configChangeContentBuilder.getCreateItems().clear();
    assertTrue(configChangeContentBuilder.hasContent());
    configChangeContentBuilder.getUpdateItems().clear();
    assertTrue(configChangeContentBuilder.hasContent());
  }

  @Test
  public void testHasContentFalseCheck() {
    configChangeContentBuilder.getCreateItems().clear();
    configChangeContentBuilder.getUpdateItems().clear();
    configChangeContentBuilder.getDeleteItems().clear();
    assertFalse(configChangeContentBuilder.hasContent());
  }

  @Test
  public void testUpdateItemWithNullOldValue() {
    ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();

    Item oldItem = new Item();
    oldItem.setKey("someKey");
    oldItem.setValue(null);

    Item newItem = new Item();
    newItem.setKey("someKey");
    newItem.setValue("newValue");

    // Should NOT throw NPE; should detect change from null to non-null
    builder.updateItem(oldItem, newItem);

    assertTrue("should detect change from null to non-null", builder.hasContent());
    assertEquals("should have one update item", 1, builder.getUpdateItems().size());
  }

  @Test
  public void testUpdateItemWithBothNullValues() {
    ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();

    Item oldItem = new Item();
    oldItem.setKey("someKey");
    oldItem.setValue(null);

    Item newItem = new Item();
    newItem.setKey("someKey");
    newItem.setValue(null);

    // Both values are null so no change should be recorded
    builder.updateItem(oldItem, newItem);

    assertFalse("no change when both values are null", builder.hasContent());
  }

  @Test
  public void testConvertJsonString() {
    ConfigChangeContentBuilder contentBuilder =
        ConfigChangeContentBuilder.convertJsonString(configString);
    assertNotNull(contentBuilder.getCreateItems());
    assertNotNull(contentBuilder.getUpdateItems().get(0).oldItem);
    assertNotNull(contentBuilder.getUpdateItems().get(0).newItem);
    assertNotNull(contentBuilder.getDeleteItems());
  }

}
