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
package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;

import java.util.List;

public class NamespaceBO {
  private NamespaceDTO baseInfo;
  private int itemModifiedCnt;
  private List<ItemBO> items;
  private String format;
  private boolean isPublic;
  private String parentAppId;
  private String comment;
  // is the configs hidden to current user?
  private boolean isConfigHidden;

  public NamespaceDTO getBaseInfo() {
    return baseInfo;
  }

  public void setBaseInfo(NamespaceDTO baseInfo) {
    this.baseInfo = baseInfo;
  }

  public int getItemModifiedCnt() {
    return itemModifiedCnt;
  }

  public void setItemModifiedCnt(int itemModifiedCnt) {
    this.itemModifiedCnt = itemModifiedCnt;
  }

  public List<ItemBO> getItems() {
    return items;
  }

  public void setItems(List<ItemBO> items) {
    this.items = items;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean aPublic) {
    isPublic = aPublic;
  }

  public String getParentAppId() {
    return parentAppId;
  }

  public void setParentAppId(String parentAppId) {
    this.parentAppId = parentAppId;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public boolean isConfigHidden() {
    return isConfigHidden;
  }

  public void setConfigHidden(boolean hidden) {
    isConfigHidden = hidden;
  }

  public void hideItems() {
    setConfigHidden(true);
    items.clear();
    setItemModifiedCnt(0);
  }
}
