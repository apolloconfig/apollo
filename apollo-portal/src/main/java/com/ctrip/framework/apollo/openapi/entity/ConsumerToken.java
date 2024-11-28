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
package com.ctrip.framework.apollo.openapi.entity;

import com.ctrip.framework.apollo.common.entity.BaseEntity;

import javax.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Entity
@Table(name = "`ConsumerToken`")
@SQLDelete(sql = "Update `ConsumerToken` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class ConsumerToken extends BaseEntity {
  @Column(name = "`ConsumerId`", nullable = false)
  private long consumerId;

  @Column(name = "`Token`", nullable = false)
  private String token;

  @PositiveOrZero
  @Column(name = "`RateLimit`", nullable = false)
  private Integer rateLimit;

  @Column(name = "`Expires`", nullable = false)
  private Date expires;

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Integer getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(Integer rateLimit) {
    this.rateLimit = rateLimit;
  }

  public Date getExpires() {
    return expires;
  }

  public void setExpires(Date expires) {
    this.expires = expires;
  }

  @Override
  public String toString() {
    return toStringHelper().add("consumerId", consumerId).add("token", token)
        .add("rateLimit", rateLimit)
        .add("expires", expires).toString();
  }
}
