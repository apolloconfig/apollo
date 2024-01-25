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
package com.ctrip.framework.apollo.portal.entity.po;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author lepdou 2017-04-08
 */
@Entity
@Table(name = "`Users`")
public class UserPO {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`Id`")
  private long id;
  @Column(name = "`Username`", nullable = false)
  private String username;
  @Column(name = "`UserDisplayName`", nullable = false)
  private String userDisplayName;
  @Column(name = "`Password`", nullable = false)
  private String password;
  @Column(name = "`Email`", nullable = false)
  private String email;
  @Column(name = "`Enabled`", nullable = false)
  private int enabled;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getEnabled() {
    return enabled;
  }

  public void setEnabled(int enabled) {
    this.enabled = enabled;
  }

  public UserInfo toUserInfo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(this.getUsername());
    userInfo.setName(this.getUserDisplayName());
    userInfo.setEmail(this.getEmail());
    userInfo.setEnabled(this.getEnabled());
    return userInfo;
  }
}
