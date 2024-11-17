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
package com.ctrip.framework.apollo.portal.entity.vo.consumer;

/**
 * @see com.ctrip.framework.apollo.openapi.entity.Consumer
 */
public class ConsumerCreateRequestVO {
  private String appId;
  private boolean allowCreateApplication;
  private String name;
  private String orgId;
  private String orgName;
  private String ownerName;
  private boolean rateLimitEnabled;
  private int rateLimit;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public boolean isAllowCreateApplication() {
    return allowCreateApplication;
  }

  public void setAllowCreateApplication(boolean allowCreateApplication) {
    this.allowCreateApplication = allowCreateApplication;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public boolean isRateLimitEnabled() {
    return rateLimitEnabled;
  }

  public void setRateLimitEnabled(boolean rateLimitEnabled) {
    this.rateLimitEnabled = rateLimitEnabled;
  }

  public int getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(int rateLimit) {
    this.rateLimit = rateLimit;
  }

}
