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
package com.ctrip.framework.apollo.biz.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.biz.MockBeanFactory;
import com.ctrip.framework.apollo.biz.entity.Item;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jian.tan
 */

public class ConfigChangeContentBuilderTest {

  private final ConfigChangeContentBuilder configChangeContentBuilder = new ConfigChangeContentBuilder();
  private String configString;

  @Before
  public void initConfig() {

    Item createdItem = MockBeanFactory.mockItem(1, 1, "timeout", "100", 1);
    Item updatedItem = MockBeanFactory.mockItem(1, 1, "timeout", "1001", 1);

    configChangeContentBuilder.createItem(createdItem);
    configChangeContentBuilder.updateItem(createdItem, updatedItem);
    configChangeContentBuilder.deleteItem(updatedItem);

    configString = configChangeContentBuilder.build();
  }

  @Test
  public void testHasContent() {
    assertTrue(configChangeContentBuilder.hasContent());
  }

  @Test
  public void testConvertJsonString() {
    ConfigChangeContentBuilder contentBuilder = ConfigChangeContentBuilder
        .convertJsonString(configString);

    assertNotNull(contentBuilder.getCreateItems());
    assertNotNull(contentBuilder.getUpdateItems().get(0).oldItem);
    assertNotNull(contentBuilder.getUpdateItems().get(0).newItem);
    assertNotNull(contentBuilder.getDeleteItems());
  }

}
