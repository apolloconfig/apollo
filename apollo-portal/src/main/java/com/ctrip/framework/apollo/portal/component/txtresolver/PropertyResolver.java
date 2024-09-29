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
import com.ctrip.framework.apollo.common.utils.BeanUtils;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.base.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * normal property file resolver.
 * update comment and blank item implement by create new item and delete old item.
 * update normal key/value item implement by update.
 */
@Component("propertyResolver")
public class PropertyResolver implements ConfigTextResolver {

  private static final String KV_SEPARATOR = "=";
  private static final String ITEM_SEPARATOR = "\n";

  @Override
  public ItemChangeSets resolve(long namespaceId, String configText, List<ItemDTO> baseItems) {

    Map<String, ItemDTO> oldKeyMapItem = BeanUtils.mapByKey("key", baseItems);
    //remove comment and blank item map.
    oldKeyMapItem.remove("");

    // comment items
    List<ItemDTO> baseCommentItems = new LinkedList<>();
    // blank items
    List<ItemDTO> baseBlankItems = new LinkedList<>();
    if (!CollectionUtils.isEmpty(baseItems)) {

      baseCommentItems = baseItems.stream().filter(itemDTO -> isCommentItem(itemDTO)).sorted(Comparator.comparing(ItemDTO::getLineNum)).collect(Collectors.toCollection(LinkedList::new));

      baseBlankItems = baseItems.stream().filter(itemDTO -> isBlankItem(itemDTO)).sorted(Comparator.comparing(ItemDTO::getLineNum)).collect(Collectors.toCollection(LinkedList::new));
    }

    String[] newItems = configText.split(ITEM_SEPARATOR);
    Set<String> repeatKeys = new HashSet<>();
    if (isHasRepeatKey(newItems, repeatKeys)) {
      throw new BadRequestException("Config text has repeated keys: %s, please check your input.", repeatKeys);
    }

    ItemChangeSets changeSets = new ItemChangeSets();
    Map<Integer, String> newLineNumMapItem = new HashMap<>();//use for delete blank and comment item
    int lineCounter = 1;
    for (String newItem : newItems) {
      newItem = newItem.trim();
      newLineNumMapItem.put(lineCounter, newItem);

      //comment item
      if (isCommentItem(newItem)) {
        ItemDTO oldItemDTO = null;
        if (!CollectionUtils.isEmpty(baseCommentItems)) {
          oldItemDTO = baseCommentItems.remove(0);
        }

        handleCommentLine(namespaceId, oldItemDTO, newItem, lineCounter, changeSets);

        //blank item
      } else if (isBlankItem(newItem)) {

        ItemDTO oldItemDTO = null;
        if (!CollectionUtils.isEmpty(baseBlankItems)) {
          oldItemDTO = baseBlankItems.remove(0);
        }

        handleBlankLine(namespaceId, oldItemDTO, lineCounter, changeSets);

        //normal item
      } else {
        handleNormalLine(namespaceId, oldKeyMapItem, newItem, lineCounter, changeSets);
      }

      lineCounter++;
    }

    deleteCommentAndBlankItem(baseCommentItems, baseBlankItems, changeSets);
    deleteNormalKVItem(oldKeyMapItem, changeSets);

    return changeSets;
  }

  private boolean isHasRepeatKey(String[] newItems, @NotNull Set<String> repeatKeys) {
    Set<String> keys = new HashSet<>();
    int lineCounter = 1;
    for (String item : newItems) {
      if (!isCommentItem(item) && !isBlankItem(item)) {
        String[] kv = parseKeyValueFromItem(item);
        if (kv != null) {
          String key = kv[0].toLowerCase();
          if (!keys.add(key)) {
            repeatKeys.add(key);
          }
        } else {
          throw new BadRequestException("line:" + lineCounter + " key value must separate by '='");
        }
      }
      lineCounter++;
    }
    return !repeatKeys.isEmpty();
  }

  private String[] parseKeyValueFromItem(String item) {
    int kvSeparator = item.indexOf(KV_SEPARATOR);
    if (kvSeparator == -1) {
      return null;
    }

    String[] kv = new String[2];
    kv[0] = item.substring(0, kvSeparator).trim();
    kv[1] = item.substring(kvSeparator + 1).trim();
    return kv;
  }

  private void handleCommentLine(Long namespaceId, ItemDTO oldItemByLine, String newItem, int lineCounter, ItemChangeSets changeSets) {
    if (null == oldItemByLine) {
      changeSets.addCreateItem(buildCommentItem(0L, namespaceId, newItem, lineCounter));
    } else if (!StringUtils.equals(oldItemByLine.getComment(), newItem) || lineCounter != oldItemByLine.getLineNum()) {
      changeSets.addUpdateItem(buildCommentItem(oldItemByLine.getId(), namespaceId, newItem, lineCounter));
    }
  }

  private void handleBlankLine(Long namespaceId, ItemDTO oldItem, int lineCounter, ItemChangeSets changeSets) {
    if (null == oldItem) {
      changeSets.addCreateItem(buildBlankItem(0L, namespaceId, lineCounter));
    } else if (lineCounter != oldItem.getLineNum()) {
      changeSets.addUpdateItem(buildBlankItem(oldItem.getId(), namespaceId, lineCounter));
    }
  }

  private void handleNormalLine(Long namespaceId, Map<String, ItemDTO> keyMapOldItem, String newItem,
                                int lineCounter, ItemChangeSets changeSets) {

    String[] kv = parseKeyValueFromItem(newItem);

    if (kv == null) {
      throw new BadRequestException("line:" + lineCounter + " key value must separate by '='");
    }

    String newKey = kv[0];
    String newValue = kv[1].replace("\\n", "\n"); //handle user input \n

    ItemDTO oldItem = keyMapOldItem.get(newKey);

    //new item
    if (oldItem == null) {
      changeSets.addCreateItem(buildNormalItem(0L, namespaceId, newKey, newValue, "", lineCounter));
      //update item
    } else if (!StringUtils.equals(newValue, oldItem.getValue()) || lineCounter != oldItem.getLineNum()) {
      changeSets.addUpdateItem(buildNormalItem(oldItem.getId(), namespaceId, newKey, newValue, oldItem.getComment(), lineCounter));
    }
    keyMapOldItem.remove(newKey);
  }

  private boolean isCommentItem(ItemDTO item) {
    return item != null && "".equals(item.getKey())
        && (item.getComment().startsWith("#") || item.getComment().startsWith("!"));
  }

  private boolean isCommentItem(String line) {
    return line != null && (line.startsWith("#") || line.startsWith("!"));
  }

  private boolean isBlankItem(ItemDTO item) {
    return item != null && "".equals(item.getKey()) && "".equals(item.getComment());
  }

  private boolean isBlankItem(String line) {
    return Strings.nullToEmpty(line).trim().isEmpty();
  }

  private void deleteNormalKVItem(Map<String, ItemDTO> baseKeyMapItem, ItemChangeSets changeSets) {
    //surplus item is to be deleted
    for (Map.Entry<String, ItemDTO> entry : baseKeyMapItem.entrySet()) {
      changeSets.addDeleteItem(entry.getValue());
    }
  }

  private void deleteCommentAndBlankItem(List<ItemDTO> baseCommentItems,
                                         List<ItemDTO> baseBlankItems,
                                         ItemChangeSets changeSets) {
    baseCommentItems.forEach(oldItemDTO -> changeSets.addDeleteItem(oldItemDTO));
    baseBlankItems.forEach(oldItemDTO -> changeSets.addDeleteItem(oldItemDTO));
  }

  private ItemDTO buildCommentItem(Long id, Long namespaceId, String comment, int lineNum) {
    return buildNormalItem(id, namespaceId, "", "", comment, lineNum);
  }

  private ItemDTO buildBlankItem(Long id, Long namespaceId, int lineNum) {
    return buildNormalItem(id, namespaceId, "", "", "", lineNum);
  }

  private ItemDTO buildNormalItem(Long id, Long namespaceId, String key, String value, String comment, int lineNum) {
    ItemDTO item = new ItemDTO(key, value, comment, lineNum);
    item.setId(id);
    item.setNamespaceId(namespaceId);
    return item;
  }
}
