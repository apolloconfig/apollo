/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.ctrip.framework.apollo.biz.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public void testConvertJsonString() {
    ConfigChangeContentBuilder contentBuilder =
        ConfigChangeContentBuilder.convertJsonString(configString);
    assertNotNull(contentBuilder.getCreateItems());
    assertNotNull(contentBuilder.getUpdateItems().get(0).oldItem);
    assertNotNull(contentBuilder.getUpdateItems().get(0).newItem);
    assertNotNull(contentBuilder.getDeleteItems());
  }

}
