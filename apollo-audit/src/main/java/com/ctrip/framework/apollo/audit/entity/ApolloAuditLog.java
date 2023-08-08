package com.ctrip.framework.apollo.audit.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "`AuditLog`")
@SQLDelete(sql = "Update AuditLog set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class ApolloAuditLog extends BaseEntity {

  @Column(name = "TraceId", nullable = false)
  private String traceId;

  @Column(name = "SpanId", nullable = false)
  private String spanId;

  @Column(name = "ParentSpanId", nullable = false)
  private String parentSpanId;

  @Column(name = "FollowsFromSpanId", nullable = false)
  private String followsFromSpanId;

  @Column(name = "Operator", nullable = false)
  private String operator;

  @Column(name = "OpType", nullable = false)
  private String opType;

  @Column(name = "OpName", nullable = false)
  private String opName;

  @Column(name = "Description", nullable = true)
  private String description;

  @Override
  public String toString(){
    return toStringHelper().add("TraceId", traceId).add("SpanId", spanId)
        .add("ParentSpan", parentSpanId).add("FollowsFromSpan", followsFromSpanId)
        .add("OpType", opType).add("OpName", opName).add("Operator", operator).toString();
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

}
