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

public class OpenAppNamespaceDTO extends BaseDTO {
  private String name;

  private String appId;

  private String format;

  private boolean isPublic;

  // whether to append namespace prefix for public namespace name
  private boolean appendNamespacePrefix = true;

  private String comment;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
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

  public boolean isAppendNamespacePrefix() {
    return appendNamespacePrefix;
  }

  public void setAppendNamespacePrefix(boolean appendNamespacePrefix) {
    this.appendNamespacePrefix = appendNamespacePrefix;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return "OpenAppNamespaceDTO{"
        + "name='"
        + name
        + '\''
        + ", appId='"
        + appId
        + '\''
        + ", format='"
        + format
        + '\''
        + ", isPublic="
        + isPublic
        + ", appendNamespacePrefix="
        + appendNamespacePrefix
        + ", comment='"
        + comment
        + '\''
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
