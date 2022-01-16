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

import java.util.Map;

public class OpenReleaseDTO extends BaseDTO {

  private long id;

  private String appId;

  private String clusterName;

  private String namespaceName;

  private String name;

  private Map<String, String> configurations;

  private String comment;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

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

  public Map<String, String> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Map<String, String> configurations) {
    this.configurations = configurations;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return "OpenReleaseDTO{"
        + "id="
        + id
        + ", appId='"
        + appId
        + '\''
        + ", clusterName='"
        + clusterName
        + '\''
        + ", namespaceName='"
        + namespaceName
        + '\''
        + ", name='"
        + name
        + '\''
        + ", configurations="
        + configurations
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
