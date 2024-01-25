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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.utils.StringUtils;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ItemsComparator {


  public ItemChangeSets compareIgnoreBlankAndCommentItem(long baseNamespaceId, List<ItemDTO> baseItems, List<ItemDTO> targetItems){
    List<ItemDTO> filteredSourceItems = filterBlankAndCommentItem(baseItems);
    List<ItemDTO> filteredTargetItems = filterBlankAndCommentItem(targetItems);

    Map<String, ItemDTO> sourceItemMap = BeanUtils.mapByKey("key", filteredSourceItems);
    Map<String, ItemDTO> targetItemMap = BeanUtils.mapByKey("key", filteredTargetItems);

    ItemChangeSets changeSets = new ItemChangeSets();

    for (ItemDTO item: targetItems){
      String key = item.getKey();

      ItemDTO sourceItem = sourceItemMap.get(key);
      if (sourceItem == null){//add
        ItemDTO copiedItem = copyItem(item);
        copiedItem.setNamespaceId(baseNamespaceId);
        changeSets.addCreateItem(copiedItem);
      }else if (!Objects.equals(sourceItem.getValue(), item.getValue())){//update
        //only type & value & comment can be update
        sourceItem.setType(item.getType());
        sourceItem.setValue(item.getValue());
        sourceItem.setComment(item.getComment());
        changeSets.addUpdateItem(sourceItem);
      }
    }

    for (ItemDTO item: baseItems){
      String key = item.getKey();

      ItemDTO targetItem = targetItemMap.get(key);
      if(targetItem == null){//delete
        changeSets.addDeleteItem(item);
      }
    }

    return changeSets;
  }

  private List<ItemDTO> filterBlankAndCommentItem(List<ItemDTO> items){

    List<ItemDTO> result = new LinkedList<>();

    if (CollectionUtils.isEmpty(items)){
      return result;
    }

    for (ItemDTO item: items){
      if (!StringUtils.isEmpty(item.getKey())){
        result.add(item);
      }
    }

    return result;
  }

  private ItemDTO copyItem(ItemDTO sourceItem){
    ItemDTO copiedItem = new ItemDTO();
    copiedItem.setKey(sourceItem.getKey());
    copiedItem.setType(sourceItem.getType());
    copiedItem.setValue(sourceItem.getValue());
    copiedItem.setComment(sourceItem.getComment());
    return copiedItem;

  }

}
