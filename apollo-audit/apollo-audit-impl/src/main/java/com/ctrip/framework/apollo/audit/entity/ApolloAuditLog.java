/*
 * Copyright 2023 Apollo Authors
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

import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "`AuditLog`")
@SQLDelete(sql = "Update AuditLog set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class ApolloAuditLog extends BaseEntity{

  @Column(name = "TraceId", nullable = false)
  private String traceId;

  @Column(name = "SpanId", nullable = false)
  private String spanId;

  @Column(name = "ParentSpanId", nullable = true)
  private String parentSpanId;

  @Column(name = "FollowsFromSpanId", nullable = true)
  private String followsFromSpanId;

  @Column(name = "Operator", nullable = true)
  private String operator;

  @Column(name = "OpType", nullable = true)
  private String opType;

  @Column(name = "OpName", nullable = true)
  private String opName;

  @Column(name = "Description", nullable = true)
  private String description;

  public static Builder builder() {
    return new Builder();
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  public String getParentSpanId() {
    return parentSpanId;
  }

  public void setParentSpanId(String parentSpanId) {
    this.parentSpanId = parentSpanId;
  }

  public String getFollowsFromSpanId() {
    return followsFromSpanId;
  }

  public void setFollowsFromSpanId(String followsFromSpanId) {
    this.followsFromSpanId = followsFromSpanId;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getOpType() {
    return opType;
  }

  public void setOpType(String opType) {
    this.opType = opType;
  }

  public String getOpName() {
    return opName;
  }

  public void setOpName(String opName) {
    this.opName = opName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public static class Builder {

    ApolloAuditLog auditLog = new ApolloAuditLog();

    public Builder() {
    }

    public Builder traceId(String val) {
      auditLog.setTraceId(val);
      return this;
    }

    public Builder spanId(String val) {
      auditLog.setSpanId(val);
      return this;
    }

    public Builder parentSpanId(String val) {
      auditLog.setParentSpanId(val);
      return this;
    }

    public Builder followsFromSpanId(String val) {
      auditLog.setFollowsFromSpanId(val);
      return this;
    }

    public Builder operator(String val) {
      auditLog.setOperator(val);
      return this;
    }

    public Builder opType(String val) {
      auditLog.setOpType(val);
      return this;
    }

    public Builder opName(String val) {
      auditLog.setOpName(val);
      return this;
    }

    public Builder description(String val) {
      auditLog.setDescription(val);
      return this;
    }

    public ApolloAuditLog build() {
      return auditLog;
    }

  }
}
