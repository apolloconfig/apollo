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
 * the LdapMappingProperties description.
 *
 * @author wuzishu
 */
public class LdapMappingProperties {

  /**
   * user ldap objectClass
   */
  private String objectClass;

  /**
   * user login Id
   */
  private String loginId;

  /**
   * user rdn key
   */
  private String rdnKey;

  /**
   * user display name
   */
  private String userDisplayName;

  /**
   * email
   */
  private String email;

  public String getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(String objectClass) {
    this.objectClass = objectClass;
  }

  public String getLoginId() {
    return loginId;
  }

  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

  public String getRdnKey() {
    return rdnKey;
  }

  public void setRdnKey(String rdnKey) {
    this.rdnKey = rdnKey;
  }

  public String getUserDisplayName() {
    return userDisplayName;
  }

  public void setUserDisplayName(String userDisplayName) {
    this.userDisplayName = userDisplayName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
