package com.ctrip.framework.apollo.portal.entity.vo;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import java.util.Set;

public class ClusterRolesAssignedUsers {

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
    return "ClusterRolesAssignedUsers{" +
        "appId='" + appId + '\'' +
        ", env='" + env + '\'' +
        ", cluster='" + cluster + '\'' +
        ", modifyRoleUsers=" + modifyRoleUsers +
        ", releaseRoleUsers=" + releaseRoleUsers +
        '}';
  }
}
