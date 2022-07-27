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
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
  }

  @Test
  public void testAddCommentAndBlankItem() {
    ItemChangeSets changeSets = resolver.resolve(1, "#ddd\na=b\n\nb=c\nc=d", mockBaseItemHas3Key());
    Assert.assertEquals(2, changeSets.getCreateItems().size());
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
  }

  @Test
  public void testChangeItemNumLine() {
    ItemChangeSets changeSets = resolver.resolve(1, "b=c\nc=d\na=b", mockBaseItemHas3Key());
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getCreateItems().size());
    Assert.assertEquals(0, changeSets.getDeleteItems().size());
  }

  @Test
  public void testDeleteItem() {

    ItemChangeSets changeSets = resolver.resolve(1, "a=b", mockBaseItemHas3Key());
    Assert.assertEquals(2, changeSets.getDeleteItems().size());
  }

  @Test
  public void testDeleteCommentItem() {
    ItemChangeSets changeSets = resolver.resolve(1, "a=b\n\nb=c", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(2, changeSets.getDeleteItems().size());
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
    Assert.assertEquals(1, changeSets.getCreateItems().size());
  }

  @Test
  public void testDeleteBlankItem(){
    ItemChangeSets changeSets = resolver.resolve(1, "#qqqq\na=b\nb=c", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(1, changeSets.getDeleteItems().size());
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
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
    Assert.assertEquals(1, changeSets.getDeleteItems().size());
    Assert.assertEquals(0, changeSets.getUpdateItems().size());
    Assert.assertEquals(1, changeSets.getCreateItems().size());
  }

  @Test
  public void testDeleteNonBottomItemThenOtherItemLastModifiedTimeNotUpdate() {
    String configText = "#timeout(seconds)\n"
        + "timeout=100\n"
        + "jdbc.username=mghio\n"
        + "jdbc.password=123456";

    String toDeleteItemKey = "jdbc.url";
    String toDeleteItemValue = "jdbc:mysql://localhost:3306/testdb?characterEncoding=utf8";
    List<ItemDTO> baseItems = mockBaseItem4TestDeleteNonBottomItemThenOtherItemLastModifiedTimeNotUpdate();

    ItemChangeSets changeSets = resolver.resolve(1, configText, baseItems);

    Assert.assertEquals(1, changeSets.getDeleteItems().size());
    ItemDTO toDeleteItem = changeSets.getDeleteItems().get(0);
    Assert.assertEquals(toDeleteItemKey, toDeleteItem.getKey());
    Assert.assertEquals(toDeleteItemValue, toDeleteItem.getValue());

    Assert.assertEquals(0, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getCreateItems().size());
  }

  @Test
  public void testAddNonBottomItemThenOtherItemLastModifiedTimeNotUpdate() {
    String configText = "#timeout(seconds)\n"
        + "jdbc.url=jdbc:mysql://localhost:3306/testdb?characterEncoding=utf8\n"
        + "timeout=100\n"
        + "jdbc.username=mghio\n"
        + "jdbc.password=123456";

    String toAddItemKey = "jdbc.url";
    String toAddItemValue = "jdbc:mysql://localhost:3306/testdb?characterEncoding=utf8";
    List<ItemDTO> baseItems = mockBaseItem4TestAddNonBottomItemThenOtherItemLastModifiedTimeNotUpdate();

    ItemChangeSets changeSets = resolver.resolve(1, configText, baseItems);

    Assert.assertEquals(1, changeSets.getCreateItems().size());
    ItemDTO toAddItem = changeSets.getCreateItems().get(0);
    Assert.assertEquals(toAddItemKey, toAddItem.getKey());
    Assert.assertEquals(toAddItemValue, toAddItem.getValue());

    Assert.assertEquals(0, changeSets.getUpdateItems().size());
    Assert.assertEquals(0, changeSets.getDeleteItems().size());
  }

  @Test
  public void testAllSituation(){
    ItemChangeSets changeSets = resolver.resolve(1, "#ww\nd=e\nb=d\na=b\n\nq=w\n#eee", mockBaseItemWith2Key1Comment1Blank());
    Assert.assertEquals(2, changeSets.getDeleteItems().size());
    Assert.assertEquals(1, changeSets.getUpdateItems().size());
    Assert.assertEquals(5, changeSets.getCreateItems().size());
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

  /**
   * #timeout(seconds)
   * timeout=100
   * jdbc.url=jdbc:mysql://localhost:3306/testdb?characterEncoding=utf8
   * jdbc.username=mghio
   * jdbc.password=123456
   */
  private List<ItemDTO> mockBaseItem4TestDeleteNonBottomItemThenOtherItemLastModifiedTimeNotUpdate() {
    ItemDTO item1 = new ItemDTO("", "", "#timeout(seconds)", 1);
    ItemDTO item2 = new ItemDTO("timeout", "100", "", 2);
    ItemDTO item3 = new ItemDTO("jdbc.url", "jdbc:mysql://localhost:3306/testdb?characterEncoding=utf8", "", 3);
    ItemDTO item4 = new ItemDTO("jdbc.username", "mghio", "", 4);
    ItemDTO item5 = new ItemDTO("jdbc.password", "123456", "", 5);
    return Arrays.asList(item1, item2, item3, item4, item5);
  }

  /**
   * #timeout(seconds)
   * timeout=100
   * jdbc.username=mghio
   * jdbc.password=123456
   */
  private List<ItemDTO> mockBaseItem4TestAddNonBottomItemThenOtherItemLastModifiedTimeNotUpdate() {
    ItemDTO item1 = new ItemDTO("", "", "#timeout(seconds)", 1);
    ItemDTO item2 = new ItemDTO("timeout", "100", "", 2);
    ItemDTO item3 = new ItemDTO("jdbc.username", "mghio", "", 3);
    ItemDTO item4 = new ItemDTO("jdbc.password", "123456", "", 4);
    return Arrays.asList(item1, item2, item3, item4);
  }

}
