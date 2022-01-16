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
package com.ctrip.framework.apollo.openapi.dto;

import java.util.List;

public class OpenNamespaceDTO extends BaseDTO {

  private String appId;

  private String clusterName;

  private String namespaceName;

  private String comment;

  private String format;

  private boolean isPublic;

  private List<OpenItemDTO> items;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public void setNamespaceName(String namespaceName) {
    this.namespaceName = namespaceName;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
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

  public List<OpenItemDTO> getItems() {
    return items;
  }

  public void setItems(List<OpenItemDTO> items) {
    this.items = items;
  }

  @Override
  public String toString() {
    return "OpenNamespaceDTO{"
        + "appId='"
        + appId
        + '\''
        + ", clusterName='"
        + clusterName
        + '\''
        + ", namespaceName='"
        + namespaceName
        + '\''
        + ", comment='"
        + comment
        + '\''
        + ", format='"
        + format
        + '\''
        + ", isPublic="
        + isPublic
        + ", items="
        + items
        + ", dataChangeCreatedBy='"
        + dataChangeCreatedBy
        + '\''
        + ", dataChangeLastModifiedBy='"
        + dataChangeLastModifiedBy
        + '\''
        + ", dataChangeCreatedTime="
        + dataChangeCreatedTime
        + ", dataChangeLastModifiedTime="
        + dataChangeLastModifiedTime
        + '}';
  }
}
