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
@Table(name = "`Cluster`")
@SQLDelete(sql = "Update `Cluster` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class Cluster extends BaseEntity implements Comparable<Cluster> {

  @Column(name = "`Name`", nullable = false)
  private String name;

  @Column(name = "`AppId`", nullable = false)
  private String appId;

  @Column(name = "`ParentClusterId`", nullable = false)
  private long parentClusterId;

  @Column(name = "`Comment`")
  private String comment;

  public String getAppId() {
    return appId;
  }

  public String getName() {
    return name;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getParentClusterId() {
    return parentClusterId;
  }

  public void setParentClusterId(long parentClusterId) {
    this.parentClusterId = parentClusterId;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return toStringHelper().add("name", name).add("appId", appId)
        .add("parentClusterId", parentClusterId).add("comment", comment).toString();
  }

  @Override
  public int compareTo(Cluster o) {
    if (o == null || getId() > o.getId()) {
      return 1;
    }

    if (getId() == o.getId()) {
      return 0;
    }

    return -1;
  }
}
