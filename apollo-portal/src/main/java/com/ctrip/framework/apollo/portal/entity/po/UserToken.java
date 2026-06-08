/*
 * Copyright 2025 Apollo Authors
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
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "`UserToken`")
@SQLDelete(
    sql = "Update `UserToken` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@SQLRestriction("`IsDeleted` = false")
public class UserToken extends BaseEntity {

  @Column(name = "`UserId`", nullable = false, length = 64)
  private String userId;

  @Column(name = "`Name`", nullable = false, length = 128)
  private String name;

  @Column(name = "`TokenPrefix`", nullable = false, length = 32)
  private String tokenPrefix;

  @Column(name = "`TokenHash`", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "`Scopes`", length = 4096)
  private String scopes;

  @PositiveOrZero
  @Column(name = "`RateLimit`", nullable = false)
  private Integer rateLimit;

  @Column(name = "`Expires`", nullable = false)
  private Date expires;

  @Column(name = "`LastUsedTime`")
  private Date lastUsedTime;

  @Column(name = "`LastUsedIp`", length = 64)
  private String lastUsedIp;

  @Column(name = "`LastUsedUserAgent`", length = 512)
  private String lastUsedUserAgent;

  @Column(name = "`RevokedAt`")
  private Date revokedAt;

  @Column(name = "`RevokedBy`", length = 64)
  private String revokedBy;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTokenPrefix() {
    return tokenPrefix;
  }

  public void setTokenPrefix(String tokenPrefix) {
    this.tokenPrefix = tokenPrefix;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
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

  public Date getLastUsedTime() {
    return lastUsedTime;
  }

  public void setLastUsedTime(Date lastUsedTime) {
    this.lastUsedTime = lastUsedTime;
  }

  public String getLastUsedIp() {
    return lastUsedIp;
  }

  public void setLastUsedIp(String lastUsedIp) {
    this.lastUsedIp = lastUsedIp;
  }

  public String getLastUsedUserAgent() {
    return lastUsedUserAgent;
  }

  public void setLastUsedUserAgent(String lastUsedUserAgent) {
    this.lastUsedUserAgent = lastUsedUserAgent;
  }

  public Date getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Date revokedAt) {
    this.revokedAt = revokedAt;
  }

  public String getRevokedBy() {
    return revokedBy;
  }

  public void setRevokedBy(String revokedBy) {
    this.revokedBy = revokedBy;
  }

  @Override
  public String toString() {
    return toStringHelper().add("userId", userId).add("name", name).add("tokenPrefix", tokenPrefix)
        .add("rateLimit", rateLimit).add("expires", expires).add("lastUsedTime", lastUsedTime)
        .add("revokedAt", revokedAt).toString();
  }
}
