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
package com.ctrip.framework.apollo.common.dto;

import java.util.LinkedList;
import java.util.List;

/**
 * storage cud result
 */
public class ItemChangeSets extends BaseDTO{

  private List<ItemDTO> createItems = new LinkedList<>();
  private List<ItemDTO> updateItems = new LinkedList<>();
  private List<ItemDTO> deleteItems = new LinkedList<>();

  public void addCreateItem(ItemDTO item) {
    createItems.add(item);
  }

  public void addUpdateItem(ItemDTO item) {
    updateItems.add(item);
  }

  public void addDeleteItem(ItemDTO item) {
    deleteItems.add(item);
  }

  public boolean isEmpty(){
    return createItems.isEmpty() && updateItems.isEmpty() && deleteItems.isEmpty();
  }

  public List<ItemDTO> getCreateItems() {
    return createItems;
  }

  public List<ItemDTO> getUpdateItems() {
    return updateItems;
  }

  public List<ItemDTO> getDeleteItems() {
    return deleteItems;
  }

  public void setCreateItems(List<ItemDTO> createItems) {
    this.createItems = createItems;
  }

  public void setUpdateItems(List<ItemDTO> updateItems) {
    this.updateItems = updateItems;
  }

  public void setDeleteItems(List<ItemDTO> deleteItems) {
    this.deleteItems = deleteItems;
  }

}
