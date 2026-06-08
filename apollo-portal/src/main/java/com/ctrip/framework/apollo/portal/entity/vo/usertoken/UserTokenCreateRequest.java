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

public class UserTokenCreateRequest {

  private String name;
  private Set<String> operations;
  private Set<String> appIds;
  private Set<String> envs;
  private List<UserTokenNamespaceScope> namespaces;
  private Integer rateLimit;
  private Date expires;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
}
