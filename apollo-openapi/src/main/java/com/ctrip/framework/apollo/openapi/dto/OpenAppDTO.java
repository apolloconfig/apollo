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

public class OpenAppDTO extends BaseDTO {

  private String name;

  private String appId;

  private String orgId;

  private String orgName;

  private String ownerName;

  private String ownerEmail;

  public String getAppId() {
    return appId;
  }

  public String getName() {
    return name;
  }

  public String getOrgId() {
    return orgId;
  }

  public String getOrgName() {
    return orgName;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("OpenAppDTO{");
    sb.append("name='").append(name).append('\'');
    sb.append(", appId='").append(appId).append('\'');
    sb.append(", orgId='").append(orgId).append('\'');
    sb.append(", orgName='").append(orgName).append('\'');
    sb.append(", ownerName='").append(ownerName).append('\'');
    sb.append(", ownerEmail='").append(ownerEmail).append('\'');
    sb.append(", dataChangeCreatedBy='").append(dataChangeCreatedBy).append('\'');
    sb.append(", dataChangeLastModifiedBy='").append(dataChangeLastModifiedBy).append('\'');
    sb.append(", dataChangeCreatedTime=").append(dataChangeCreatedTime);
    sb.append(", dataChangeLastModifiedTime=").append(dataChangeLastModifiedTime);
    sb.append('}');
    return sb.toString();
  }
}
