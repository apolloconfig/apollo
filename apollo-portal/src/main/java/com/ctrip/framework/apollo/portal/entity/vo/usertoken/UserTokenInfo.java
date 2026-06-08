/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.entity.vo.usertoken;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserTokenInfo {

  private long id;
  private String userId;
  private String name;
  private String tokenPrefix;
  private String tokenValue;
  private String status;
  private Set<String> operations;
  private Set<String> appIds;
  private Set<String> envs;
  private List<UserTokenNamespaceScope> namespaces;
  private Integer rateLimit;
  private Date expires;
  private Date lastUsedTime;
  private String lastUsedIp;
  private String lastUsedUserAgent;
  private Date revokedAt;
  private String revokedBy;
  private Date dataChangeCreatedTime;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTokenPrefix() {
    return tokenPrefix;
  }

  public void setTokenPrefix(String tokenPrefix) {
    this.tokenPrefix = tokenPrefix;
  }

  public String getTokenValue() {
    return tokenValue;
  }

  public void setTokenValue(String tokenValue) {
    this.tokenValue = tokenValue;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Set<String> getOperations() {
    return operations;
  }

  public void setOperations(Set<String> operations) {
    this.operations = operations;
  }

  public Set<String> getAppIds() {
    return appIds;
  }

  public void setAppIds(Set<String> appIds) {
    this.appIds = appIds;
  }

  public Set<String> getEnvs() {
    return envs;
  }

  public void setEnvs(Set<String> envs) {
    this.envs = envs;
  }

  public List<UserTokenNamespaceScope> getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(List<UserTokenNamespaceScope> namespaces) {
    this.namespaces = namespaces;
  }

  public Integer getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(Integer rateLimit) {
    this.rateLimit = rateLimit;
  }

  public Date getExpires() {
    return expires;
  }

  public void setExpires(Date expires) {
    this.expires = expires;
  }

  public Date getLastUsedTime() {
    return lastUsedTime;
  }

  public void setLastUsedTime(Date lastUsedTime) {
    this.lastUsedTime = lastUsedTime;
  }

  public String getLastUsedIp() {
    return lastUsedIp;
  }

  public void setLastUsedIp(String lastUsedIp) {
    this.lastUsedIp = lastUsedIp;
  }

  public String getLastUsedUserAgent() {
    return lastUsedUserAgent;
  }

  public void setLastUsedUserAgent(String lastUsedUserAgent) {
    this.lastUsedUserAgent = lastUsedUserAgent;
  }

  public Date getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Date revokedAt) {
    this.revokedAt = revokedAt;
  }

  public String getRevokedBy() {
    return revokedBy;
  }

  public void setRevokedBy(String revokedBy) {
    this.revokedBy = revokedBy;
  }

  public Date getDataChangeCreatedTime() {
    return dataChangeCreatedTime;
  }

  public void setDataChangeCreatedTime(Date dataChangeCreatedTime) {
    this.dataChangeCreatedTime = dataChangeCreatedTime;
  }
}
