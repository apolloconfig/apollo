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

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "`App`")
@SQLDelete(sql = "Update `App` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
@ApolloAuditLogDataInfluenceTable(tableName = "App")
public class App extends BaseEntity {

  @NotBlank(message = "Name cannot be blank")
  @Column(name = "`Name`", nullable = false)
  @ApolloAuditLogDataInfluenceTableField(fieldName = "Name")
  private String name;

  @NotBlank(message = "AppId cannot be blank")
  @Pattern(
      regexp = InputValidator.CLUSTER_NAMESPACE_VALIDATOR,
      message = InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE
  )
  @Column(name = "`AppId`", nullable = false)
  @ApolloAuditLogDataInfluenceTableField(fieldName = "AppId")
  private String appId;

  @Column(name = "`OrgId`", nullable = false)
  private String orgId;

  @Column(name = "`OrgName`", nullable = false)
  private String orgName;

  @NotBlank(message = "OwnerName cannot be blank")
  @Column(name = "`OwnerName`", nullable = false)
  private String ownerName;

  @NotBlank(message = "OwnerEmail cannot be blank")
  @Column(name = "`OwnerEmail`", nullable = false)
  private String ownerEmail;

  public String getAppId() {
    return appId;
  }

  public String getName() {
    return name;
  }

  public String getOrgId() {
    return orgId;
  }

  public String getOrgName() {
    return orgName;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  @Override
  public String toString() {
    return toStringHelper().add("name", name).add("appId", appId)
        .add("orgId", orgId)
        .add("orgName", orgName)
        .add("ownerName", ownerName)
        .add("ownerEmail", ownerEmail).toString();
  }

  public static class Builder {

    public Builder() {
    }

    private App app = new App();

    public Builder name(String name) {
      app.setName(name);
      return this;
    }

    public Builder appId(String appId) {
      app.setAppId(appId);
      return this;
    }

    public Builder orgId(String orgId) {
      app.setOrgId(orgId);
      return this;
    }

    public Builder orgName(String orgName) {
      app.setOrgName(orgName);
      return this;
    }

    public Builder ownerName(String ownerName) {
      app.setOwnerName(ownerName);
      return this;
    }

    public Builder ownerEmail(String ownerEmail) {
      app.setOwnerEmail(ownerEmail);
      return this;
    }

    public App build() {
      return app;
    }

  }

  public static Builder builder() {
    return new Builder();
  }


}
