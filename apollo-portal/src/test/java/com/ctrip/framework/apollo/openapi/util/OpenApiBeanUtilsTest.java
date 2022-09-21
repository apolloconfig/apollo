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
package com.ctrip.framework.apollo.openapi.util;

import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author mghio (mghio.dev@gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenApiBeanUtilsTest {

  @Test
  public void testTransformFromItemBOWithUnModifiedItem() {
    ItemBO itemBO = this.mockUnModifiedItemBO();

    OpenItemDTO expectedOpenItemDTO = new OpenItemDTO();
    expectedOpenItemDTO.setKey(itemBO.getItem().getKey());
    expectedOpenItemDTO.setValue(itemBO.getItem().getValue());
    expectedOpenItemDTO.setComment(itemBO.getItem().getComment());

    OpenItemDTO actualOpenItemDTO = OpenApiBeanUtils.transformFromItemBO(itemBO);

    assertEquals(expectedOpenItemDTO.toString(), actualOpenItemDTO.toString());
  }

  @Test
  public void testTransformFromItemBOWithModifiedItem() {
    ItemBO itemBO = this.mockModifiedItemBO();

    OpenItemDTO expectedOpenItemDTO = new OpenItemDTO();
    expectedOpenItemDTO.setKey(itemBO.getItem().getKey());
    expectedOpenItemDTO.setValue(itemBO.getOldValue());
    expectedOpenItemDTO.setComment(itemBO.getItem().getComment());

    OpenItemDTO actualOpenItemDTO = OpenApiBeanUtils.transformFromItemBO(itemBO);

    assertEquals(expectedOpenItemDTO.toString(), actualOpenItemDTO.toString());
  }

  private ItemBO mockUnModifiedItemBO() {
    ItemBO itemBO = new ItemBO();

    ItemDTO itemDTO = new ItemDTO("timeout", "10", "", 2);
    itemDTO.setId(1L);
    itemDTO.setNamespaceId(1L);
    itemBO.setItem(itemDTO);

    itemBO.setModified(false);
    itemBO.setDeleted(false);
    itemBO.setOldValue(null);
    itemBO.setNewValue(null);

    return itemBO;
  }

  private ItemBO mockModifiedItemBO() {
    ItemBO itemBO = new ItemBO();

    ItemDTO itemDTO = new ItemDTO("content", "{\"k1\":newv1,\"key2\":v2}", null, 1);
    itemDTO.setId(1L);
    itemDTO.setNamespaceId(1L);
    itemBO.setItem(itemDTO);

    itemBO.setModified(true);
    itemBO.setDeleted(false);
    itemBO.setOldValue("{\"k1\":v1,\"key2\":v2}");
    itemBO.setNewValue("{\"k1\":newv1,\"key2\":v2}");

    return itemBO;
  }

}
