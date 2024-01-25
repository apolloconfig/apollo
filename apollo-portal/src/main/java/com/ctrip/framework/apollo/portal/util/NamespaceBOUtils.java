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
package com.ctrip.framework.apollo.portal.util;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.controller.ConfigsExportController;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;

import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wxq
 */
public class NamespaceBOUtils {

  private static final Gson GSON = new Gson();

  /**
   * namespace must not be {@link ConfigFileFormat#Properties}. the content of namespace in item's value which item's
   * key is {@link ConfigConsts#CONFIG_FILE_CONTENT_KEY}.
   *
   * @param namespaceBO namespace
   * @return content of non-properties's namespace
   */
  static String convertNonProperties2configFileContent(NamespaceBO namespaceBO) {
    List<ItemBO> itemBOS = namespaceBO.getItems();
    for (ItemBO itemBO : itemBOS) {
      String key = itemBO.getItem().getKey();
      // special namespace format(not properties)
      if (ConfigConsts.CONFIG_FILE_CONTENT_KEY.equals(key)) {
        ItemDTO dto = itemBO.getItem();
        dto.setId(0);
        dto.setNamespaceId(0);
        return GSON.toJson(Lists.newArrayList(dto));
      }
    }
    return "";
  }

  /**
   * copy from old {@link ConfigsExportController}. convert {@link NamespaceBO} to a file content.
   *
   * @return content of config file
   * @throws IllegalStateException if convert properties to string fail
   */
  public static String convert2configFileContent(NamespaceBO namespaceBO) {
    // early return if it is not a properties format namespace
    if (!ConfigFileFormat.Properties.equals(ConfigFileFormat.fromString(namespaceBO.getFormat()))) {
      // it is not a properties namespace
      return convertNonProperties2configFileContent(namespaceBO);
    }

    // it must be a properties format namespace
    List<ItemBO> itemBOS = namespaceBO.getItems();
    if (CollectionUtils.isEmpty(itemBOS)) {
      return GSON.toJson(Collections.emptyList());
    }

    List<ItemDTO> itemDTOS = itemBOS.stream().map(itemBO -> {
      ItemDTO dto = itemBO.getItem();
      dto.setId(0);
      dto.setNamespaceId(0);
      return dto;
    }).collect(Collectors.toList());

    return GSON.toJson(itemDTOS);
  }

}
