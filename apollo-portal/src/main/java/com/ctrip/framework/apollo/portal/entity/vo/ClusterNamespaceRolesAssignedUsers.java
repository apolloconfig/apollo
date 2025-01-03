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
package com.ctrip.framework.apollo.portal.entity.vo;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import java.util.Set;

public class ClusterNamespaceRolesAssignedUsers {

  private String appId;
  private String env;
  private String cluster;
  private Set<UserInfo> modifyRoleUsers;
  private Set<UserInfo> releaseRoleUsers;

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public Set<UserInfo> getModifyRoleUsers() {
    return modifyRoleUsers;
  }

  public void setModifyRoleUsers(Set<UserInfo> modifyRoleUsers) {
    this.modifyRoleUsers = modifyRoleUsers;
  }

  public Set<UserInfo> getReleaseRoleUsers() {
    return releaseRoleUsers;
  }

  public void setReleaseRoleUsers(Set<UserInfo> releaseRoleUsers) {
    this.releaseRoleUsers = releaseRoleUsers;
  }

  @Override
  public String toString() {
    return "ClusterNamespaceRolesAssignedUsers{" +
        "appId='" + appId + '\'' +
        ", env='" + env + '\'' +
        ", cluster='" + cluster + '\'' +
        ", modifyRoleUsers=" + modifyRoleUsers +
        ", releaseRoleUsers=" + releaseRoleUsers +
        '}';
  }
}
