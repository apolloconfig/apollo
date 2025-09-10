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

import com.ctrip.framework.apollo.common.entity.BaseEntity;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Entity
@Table(name = "`Permission`")
@SQLDelete(sql = "Update `Permission` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class Permission extends BaseEntity {
  @Column(name = "`PermissionType`", nullable = false)
  private String permissionType;

  @Column(name = "`TargetId`", nullable = false)
  private String targetId;

  public Permission() {
  }

  public Permission(String permissionType, String targetId) {
    this.permissionType = permissionType;
    this.targetId = targetId;
  }

  public String getPermissionType() {
    return permissionType;
  }

  public void setPermissionType(String permissionType) {
    this.permissionType = permissionType;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Permission)) return false;
    Permission that = (Permission) o;
    return Objects.equals(permissionType, that.permissionType) &&
            Objects.equals(targetId, that.targetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionType, targetId);
  }


}
