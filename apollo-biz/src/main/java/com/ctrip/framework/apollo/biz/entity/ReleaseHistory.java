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
package com.ctrip.framework.apollo.biz.entity;

import com.ctrip.framework.apollo.common.entity.BaseEntity;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Entity
@Table(name = "`ReleaseHistory`")
@SQLDelete(sql = "Update `ReleaseHistory` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class ReleaseHistory extends BaseEntity {
  @Column(name = "`AppId`", nullable = false)
  private String appId;

  @Column(name = "`ClusterName`", nullable = false)
  private String clusterName;

  @Column(name = "`NamespaceName`", nullable = false)
  private String namespaceName;

  @Column(name = "`BranchName`", nullable = false)
  private String branchName;

  @Column(name = "`ReleaseId`")
  private long releaseId;

  @Column(name = "`PreviousReleaseId`")
  private long previousReleaseId;

  @Column(name = "`Operation`")
  private int operation;

  @Column(name = "`OperationContext`", nullable = false)
  private String operationContext;

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

  public String getNamespaceName() {
    return namespaceName;
  }

  public void setNamespaceName(String namespaceName) {
    this.namespaceName = namespaceName;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public long getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(long releaseId) {
    this.releaseId = releaseId;
  }

  public long getPreviousReleaseId() {
    return previousReleaseId;
  }

  public void setPreviousReleaseId(long previousReleaseId) {
    this.previousReleaseId = previousReleaseId;
  }

  public int getOperation() {
    return operation;
  }

  public void setOperation(int operation) {
    this.operation = operation;
  }

  public String getOperationContext() {
    return operationContext;
  }

  public void setOperationContext(String operationContext) {
    this.operationContext = operationContext;
  }

  @Override
  public String toString() {
    return toStringHelper().add("appId", appId).add("clusterName", clusterName)
        .add("namespaceName", namespaceName).add("branchName", branchName)
        .add("releaseId", releaseId).add("previousReleaseId", previousReleaseId)
        .add("operation", operation).toString();
  }
}
