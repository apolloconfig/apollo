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
package com.ctrip.framework.apollo.audit.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "`AuditLogDataInfluence`")
public class ApolloAuditLogDataInfluence extends BaseEntity {

  @Column(name = "SpanId", nullable = false)
  private String spanId;

  @Column(name = "InfluenceEntityName", nullable = false)
  private String influenceEntityName;

  @Column(name = "InfluenceEntityId", nullable = false)
  private String influenceEntityId;

  @Column(name = "FieldName")
  private String fieldName;

  @Column(name = "FieldOldValue")
  private String fieldOldValue;

  @Column(name = "FieldNewValue")
  private String fieldNewValue;

  public ApolloAuditLogDataInfluence() {
  }

  public ApolloAuditLogDataInfluence(String spanId, String entityName, String entityId,
      String fieldName, String oldVal, String newVal) {
    this.spanId = spanId;
    this.influenceEntityName = entityName;
    this.influenceEntityId = entityId;
    this.fieldName = fieldName;
    this.fieldOldValue = oldVal;
    this.fieldNewValue = newVal;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getSpanId() {
    return spanId;
  }

  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  public String getInfluenceEntityName() {
    return influenceEntityName;
  }

  public void setInfluenceEntityName(String influenceEntityName) {
    this.influenceEntityName = influenceEntityName;
  }

  public String getInfluenceEntityId() {
    return influenceEntityId;
  }

  public void setInfluenceEntityId(String influenceEntityId) {
    this.influenceEntityId = influenceEntityId;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldOldValue() {
    return fieldOldValue;
  }

  public void setFieldOldValue(String fieldOldValue) {
    this.fieldOldValue = fieldOldValue;
  }

  public String getFieldNewValue() {
    return fieldNewValue;
  }

  public void setFieldNewValue(String fieldNewValue) {
    this.fieldNewValue = fieldNewValue;
  }

  public static class Builder {

    ApolloAuditLogDataInfluence influence = new ApolloAuditLogDataInfluence();

    public Builder() {
    }

    public Builder spanId(String val) {
      influence.setSpanId(val);
      return this;
    }

    public Builder entityId(String val) {
      influence.setInfluenceEntityId(val);
      return this;
    }

    public Builder entityName(String val) {
      influence.setInfluenceEntityName(val);
      return this;
    }

    public Builder fieldName(String val) {
      influence.setFieldName(val);
      return this;
    }

    public Builder oldVal(String val) {
      influence.setFieldOldValue(val);
      return this;
    }

    public Builder newVal(String val) {
      influence.setFieldNewValue(val);
      return this;
    }

    public ApolloAuditLogDataInfluence build() {
      return influence;
    }
  }
}
