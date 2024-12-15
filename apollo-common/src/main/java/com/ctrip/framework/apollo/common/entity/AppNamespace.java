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
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "`AppNamespace`")
@SQLDelete(sql = "Update `AppNamespace` set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
@ApolloAuditLogDataInfluenceTable(tableName = "AppNamespace")
public class AppNamespace extends BaseEntity {

  @NotBlank(message = "AppNamespace Name cannot be blank")
  @Pattern(
      regexp = InputValidator.CLUSTER_NAMESPACE_VALIDATOR,
      message = "Invalid Namespace format: " + InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE + " & " + InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE
  )
  @Column(name = "`Name`", nullable = false)
  @ApolloAuditLogDataInfluenceTableField(fieldName = "Name")
  private String name;

  @ApolloAuditLogDataInfluenceTableField(fieldName = "AppId")
  @NotBlank(message = "AppId cannot be blank")
  @Column(name = "`AppId`", nullable = false)
  private String appId;

  @ApolloAuditLogDataInfluenceTableField(fieldName = "Format")
  @Column(name = "`Format`", nullable = false)
  private String format;

  @ApolloAuditLogDataInfluenceTableField(fieldName = "IsPublic")
  @Column(name = "`IsPublic`", columnDefinition = "Bit default '0'")
  private boolean isPublic = false;

  @Column(name = "`Comment`")
  private String comment;

  public String getAppId() {
    return appId;
  }

  public String getComment() {
    return comment;
  }

  public String getName() {
    return name;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean aPublic) {
    isPublic = aPublic;
  }

  public ConfigFileFormat formatAsEnum() {
    return ConfigFileFormat.fromString(this.format);
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  @Override
  public String toString() {
    return toStringHelper().add("name", name).add("appId", appId).add("comment", comment)
        .add("format", format).add("isPublic", isPublic).toString();
  }
}
