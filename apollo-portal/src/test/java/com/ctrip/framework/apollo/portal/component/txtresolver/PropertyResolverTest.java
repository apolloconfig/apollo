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
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.AbstractUnitTest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PropertyResolverTest extends AbstractUnitTest {

  @InjectMocks
  private PropertyResolver resolver;

  @Test
  public void testEmptyText() {
    try {
      resolver.resolve(0, "", null);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof BadRequestException);
    }
  }

  @Test
  public void testRepeatKey() {
    try {
      resolver.resolve(1, "a=b\nb=c\nA=d\nB=e", Collections.emptyList());
    } catch (Exception e) {
      Assert.assertTrue(e instanceof BadRequestException);
    }
  }

  @Test
  public void testAddItemBeforeNoItem() {
    ItemChangeSets changeSets = resolver.resolve(1, "a=b\nb=c", Collections.emptyList());
    Assert.assertEquals(2, changeSets.getCreateItems().size());
  }

  @Test
  public void testAddItemBeforeHasItem() {

    ItemChangeSets changeSets = resolver.resolve(1, "x=y\na=b\nb=c\nc=d", mockBaseItemHas3Key());
    Assert.assertEquals("x", changeSets.getCreateItems().get(0).getKey());
    Assert.assertEquals(1, changeSets.getCreateItems().size());
    Assert.assertEquals(3, changeSets.getUpdateItems().size());
  }

  @Test
  public void testAddCommentAndBlankItem() {
    ItemChangeSets changeSets = resolver.resolve(1, "#ddd\na=b\n\nb=c\nc=d", mockBaseItemHas3Key());
    Assert.assertEquals(2, changeSets.getCreateItems().size());
    Assert.assertEquals(3, changeSets.getUpdateItems().size());
  }

  @Test
  public void testChangeItemNumLine() {
    ItemChangeSets changeSets = resolver.resolve(1, "b=c\nc=d\na=b", mockBaseItemHas3Key());
    Assert.assertEquals(3, changeSets.getUpdateItems().size());
  }

  @Test
  public void testDeleteItem() {

    ItemChangeSets changeSets = resolver.resolve(1, "a=b", mockBaseItemHas3Key());
    Assert.assertEquals(2, changeSets.getDeleteItems().size());
  }

  @Test
  public void testDeleteCommentItem() {
    ItemChangeSets changeSets = resolver.resolve(1, "a=b\n\nb=c", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(1, changeSets.getDeleteItems().size());
    Assert.assertEquals(3, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getCreateItems().size());
  }

  @Test
  public void testDeleteBlankItem(){
    ItemChangeSets changeSets = resolver.resolve(1, "#qqqq\na=b\nb=c", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(1, changeSets.getDeleteItems().size());
    Assert.assertEquals(1, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getCreateItems().size());
  }

  @Test
  public void testUpdateItem() {

    ItemChangeSets changeSets = resolver.resolve(1, "a=d", mockBaseItemHas3Key());
    List<ItemDTO> updateItems = changeSets.getUpdateItems();
    Assert.assertEquals(1, updateItems.size());
    Assert.assertEquals("d", updateItems.get(0).getValue());
  }

  @Test
  public void testUpdateCommentItem() {

    ItemChangeSets changeSets = resolver.resolve(1, "#ww\n"
                                                    + "a=b\n"
                                                    +"\n"
                                                    + "b=c", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(0, changeSets.getDeleteItems().size());
    Assert.assertEquals(1, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getCreateItems().size());
  }

  @Test
  public void testAllSituation(){
    ItemChangeSets changeSets = resolver.resolve(1, "#ww\nd=e\nb=c\na=b\n\nq=w\n#eee", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(0, changeSets.getDeleteItems().size());
    Assert.assertEquals(4, changeSets.getUpdateItems().size());
    Assert.assertEquals(3, changeSets.getCreateItems().size());
  }

  /**
   * a=b b=c c=d
   */
  private List<ItemDTO> mockBaseItemHas3Key() {
    ItemDTO item1 = new ItemDTO("a", "b", "", 1);
    ItemDTO item2 = new ItemDTO("b", "c", "", 2);
    ItemDTO item3 = new ItemDTO("c", "d", "", 3);
    return Arrays.asList(item1, item2, item3);
  }

  /**
   * #qqqq
   * a=b
   *
   * b=c
   */
  private List<ItemDTO> mockBaseItemWith2Key1Comment1Blank() {
    ItemDTO i1 = new ItemDTO("", "", "#qqqq", 1);
    ItemDTO i2 = new ItemDTO("a", "b", "", 2);
    ItemDTO i3 = new ItemDTO("", "", "", 3);
    ItemDTO i4 = new ItemDTO("b", "c", "", 4);
    i4.setLineNum(4);
    return Arrays.asList(i1, i2, i3, i4);
  }

}
