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

/**
 * @author kl (http://kailing.pub)
 * @since 2022/8/9
 */
public class NamespaceUsage {

  private String namespaceName;
  private String appId;
  private String clusterName;
  private String envName;
  private int instanceCount;
  private int branchInstanceCount;
  private int linkedNamespaceCount;


  public NamespaceUsage() {
  }

  public NamespaceUsage(String namespaceName, String appId, String clusterName,
      String envName) {
    this.namespaceName = namespaceName;
    this.appId = appId;
    this.clusterName = clusterName;
    this.envName = envName;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public void setNamespaceName(String namespaceName) {
    this.namespaceName = namespaceName;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public int getBranchInstanceCount() {
    return branchInstanceCount;
  }

  public void setBranchInstanceCount(int branchInstanceCount) {
    this.branchInstanceCount = branchInstanceCount;
  }

  public int getLinkedNamespaceCount() {
    return linkedNamespaceCount;
  }

  public void setLinkedNamespaceCount(int linkedNamespaceCount) {
    this.linkedNamespaceCount = linkedNamespaceCount;
  }
}
