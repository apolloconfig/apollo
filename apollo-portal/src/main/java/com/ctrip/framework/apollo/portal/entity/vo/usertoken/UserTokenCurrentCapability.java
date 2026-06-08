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

/**
 * Current user token identity and scope visible to OpenAPI callers.
 */
public class UserTokenCurrentCapability {

  private String authType;
  private String userId;
  private long tokenId;
  private String tokenName;
  private String tokenPrefix;
  private Integer rateLimit;
  private Date expires;
  private Date lastUsedTime;
  private Date dataChangeCreatedTime;
  private boolean denyAll;
  private boolean allOperations;
  private Set<String> operations;
  private boolean allApps;
  private Set<String> appIds;
  private boolean allEnvs;
  private Set<String> envs;
  private boolean allNamespaces;
  private List<UserTokenNamespaceScope> namespaces;
  private List<UserTokenOpenApiAction> actions;

  public String getAuthType() {
    return authType;
  }

  public void setAuthType(String authType) {
    this.authType = authType;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public long getTokenId() {
    return tokenId;
  }

  public void setTokenId(long tokenId) {
    this.tokenId = tokenId;
  }

  public String getTokenName() {
    return tokenName;
  }

  public void setTokenName(String tokenName) {
    this.tokenName = tokenName;
  }

  public String getTokenPrefix() {
    return tokenPrefix;
  }

  public void setTokenPrefix(String tokenPrefix) {
    this.tokenPrefix = tokenPrefix;
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

  public Date getDataChangeCreatedTime() {
    return dataChangeCreatedTime;
  }

  public void setDataChangeCreatedTime(Date dataChangeCreatedTime) {
    this.dataChangeCreatedTime = dataChangeCreatedTime;
  }

  public boolean isDenyAll() {
    return denyAll;
  }

  public void setDenyAll(boolean denyAll) {
    this.denyAll = denyAll;
  }

  public boolean isAllOperations() {
    return allOperations;
  }

  public void setAllOperations(boolean allOperations) {
    this.allOperations = allOperations;
  }

  public Set<String> getOperations() {
    return operations;
  }

  public void setOperations(Set<String> operations) {
    this.operations = operations;
  }

  public boolean isAllApps() {
    return allApps;
  }

  public void setAllApps(boolean allApps) {
    this.allApps = allApps;
  }

  public Set<String> getAppIds() {
    return appIds;
  }

  public void setAppIds(Set<String> appIds) {
    this.appIds = appIds;
  }

  public boolean isAllEnvs() {
    return allEnvs;
  }

  public void setAllEnvs(boolean allEnvs) {
    this.allEnvs = allEnvs;
  }

  public Set<String> getEnvs() {
    return envs;
  }

  public void setEnvs(Set<String> envs) {
    this.envs = envs;
  }

  public boolean isAllNamespaces() {
    return allNamespaces;
  }

  public void setAllNamespaces(boolean allNamespaces) {
    this.allNamespaces = allNamespaces;
  }

  public List<UserTokenNamespaceScope> getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(List<UserTokenNamespaceScope> namespaces) {
    this.namespaces = namespaces;
  }

  public List<UserTokenOpenApiAction> getActions() {
    return actions;
  }

  public void setActions(List<UserTokenOpenApiAction> actions) {
    this.actions = actions;
  }
}
