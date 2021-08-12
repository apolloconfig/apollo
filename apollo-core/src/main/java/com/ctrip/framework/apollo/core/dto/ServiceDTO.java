/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.core.dto;

public class ServiceDTO {

  private String appName;

  private String instanceId;

  private String homepageUrl;

  public String getAppName() {
    return appName;
  }

  public String getHomepageUrl() {
    return homepageUrl;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setHomepageUrl(String homepageUrl) {
    this.homepageUrl = homepageUrl;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ServiceDTO{");
    sb.append("appName='").append(appName).append('\'');
    sb.append(", instanceId='").append(instanceId).append('\'');
    sb.append(", homepageUrl='").append(homepageUrl).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
