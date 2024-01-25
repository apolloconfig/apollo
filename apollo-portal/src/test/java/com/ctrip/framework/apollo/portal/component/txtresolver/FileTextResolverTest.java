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
package com.ctrip.framework.apollo.portal.component.txtresolver;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.portal.AbstractUnitTest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.util.Collections;

public class FileTextResolverTest extends AbstractUnitTest {

  @InjectMocks
  private FileTextResolver resolver;

  private final String CONFIG_TEXT = "config_text";
  private final long NAMESPACE = 1000;

  @Test
  public void testCreateItem(){
    ItemChangeSets changeSets = resolver.resolve(NAMESPACE, CONFIG_TEXT, Collections.emptyList());

    Assert.assertEquals(1, changeSets.getCreateItems().size());
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getDeleteItems().size());

    ItemDTO createdItem = changeSets.getCreateItems().get(0);
    Assert.assertEquals(CONFIG_TEXT, createdItem.getValue());

  }

  @Test
  public void testUpdateItem(){
    ItemDTO existedItem = new ItemDTO();
    existedItem.setId(1000);
    existedItem.setKey(ConfigConsts.CONFIG_FILE_CONTENT_KEY);
    existedItem.setValue("before");

    ItemChangeSets changeSets = resolver.resolve(NAMESPACE, CONFIG_TEXT,
        Collections.singletonList(existedItem));

    Assert.assertEquals(0, changeSets.getCreateItems().size());
    Assert.assertEquals(1, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getDeleteItems().size());

    ItemDTO updatedItem = changeSets.getUpdateItems().get(0);
    Assert.assertEquals(CONFIG_TEXT, updatedItem.getValue());

  }

}
