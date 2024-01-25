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


package com.ctrip.framework.apollo.portal.spi.configuration;

/**
 * the LdapGroupProperties description.
 *
 * @author wuzishu
 */
public class LdapGroupProperties {

  /**
   * group search base
   */
  private String groupBase;

  /**
   * group search filter
   */
  private String groupSearch;

  /**
   * group membership prop
   */
  private String groupMembership;

  public String getGroupBase() {
    return groupBase;
  }

  public void setGroupBase(String groupBase) {
    this.groupBase = groupBase;
  }

  public String getGroupSearch() {
    return groupSearch;
  }

  public void setGroupSearch(String groupSearch) {
    this.groupSearch = groupSearch;
  }

  public String getGroupMembership() {
    return groupMembership;
  }

  public void setGroupMembership(String groupMembership) {
    this.groupMembership = groupMembership;
  }
}
