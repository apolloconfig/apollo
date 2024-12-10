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
package com.ctrip.framework.apollo.common.entity;

import com.ctrip.framework.apollo.audit.event.ApolloAuditLogDataInfluenceEvent;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import org.springframework.data.domain.DomainEvents;

@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`Id`")
  private long id;

  @Column(name = "`IsDeleted`", columnDefinition = "Bit default '0'")
  protected boolean isDeleted = false;

  @Column(name = "`DeletedAt`", columnDefinition = "Bigint default '0'")
  protected long deletedAt;

  @Column(name = "`DataChange_CreatedBy`", nullable = false)
  private String dataChangeCreatedBy;

  @Column(name = "`DataChange_CreatedTime`", nullable = false)
  private Date dataChangeCreatedTime;

  @Column(name = "`DataChange_LastModifiedBy`")
  private String dataChangeLastModifiedBy;

  @Column(name = "`DataChange_LastTime`")
  private Date dataChangeLastModifiedTime;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
    if (deleted && this.deletedAt == 0) {
      // also set deletedAt value as epoch millisecond
      this.deletedAt = System.currentTimeMillis();
    } else if (!deleted) {
      this.deletedAt = 0L;
    }
  }

  public long getDeletedAt() {
    return deletedAt;
  }

  public String getDataChangeCreatedBy() {
    return dataChangeCreatedBy;
  }

  public void setDataChangeCreatedBy(String dataChangeCreatedBy) {
    this.dataChangeCreatedBy = dataChangeCreatedBy;
  }

  public Date getDataChangeCreatedTime() {
    return dataChangeCreatedTime;
  }

  public void setDataChangeCreatedTime(Date dataChangeCreatedTime) {
    this.dataChangeCreatedTime = dataChangeCreatedTime;
  }

  public String getDataChangeLastModifiedBy() {
    return dataChangeLastModifiedBy;
  }

  public void setDataChangeLastModifiedBy(String dataChangeLastModifiedBy) {
    this.dataChangeLastModifiedBy = dataChangeLastModifiedBy;
  }

  public Date getDataChangeLastModifiedTime() {
    return dataChangeLastModifiedTime;
  }

  public void setDataChangeLastModifiedTime(Date dataChangeLastModifiedTime) {
    this.dataChangeLastModifiedTime = dataChangeLastModifiedTime;
  }

  @PrePersist
  protected void prePersist() {
    if (this.dataChangeCreatedTime == null) {
        dataChangeCreatedTime = new Date();
    }
    if (this.dataChangeLastModifiedTime == null) {
        dataChangeLastModifiedTime = new Date();
    }
  }

  @PreUpdate
  protected void preUpdate() {
    this.dataChangeLastModifiedTime = new Date();
  }

  @PreRemove
  protected void preRemove() {
    this.dataChangeLastModifiedTime = new Date();
  }

  protected ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this).omitNullValues().add("id", id)
        .add("dataChangeCreatedBy", dataChangeCreatedBy)
        .add("dataChangeCreatedTime", dataChangeCreatedTime)
        .add("dataChangeLastModifiedBy", dataChangeLastModifiedBy)
        .add("dataChangeLastModifiedTime", dataChangeLastModifiedTime);
  }

  @Override
  public String toString(){
    return toStringHelper().toString();
  }

  @DomainEvents
  public Collection<ApolloAuditLogDataInfluenceEvent> domainEvents() {
    return Collections.singletonList(new ApolloAuditLogDataInfluenceEvent(this.getClass(), this));
  }
}
