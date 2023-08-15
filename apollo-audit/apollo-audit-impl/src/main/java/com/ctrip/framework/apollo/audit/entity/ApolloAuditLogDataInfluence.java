package com.ctrip.framework.apollo.audit.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "`AuditLogDataInfluence`")
public class ApolloAuditLogDataInfluence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "`Id`")
  private long id;

  @Column(name = "SpanId", nullable = false)
  private String spanId;

  @Column(name = "InfluenceEntityName", nullable = false)
  private String influenceEntityName;

  @Column(name = "InfluenceEntityId", nullable = false)
  private long influenceEntityId;

  @Column(name = "FieldName", nullable = true)
  private String fieldName;

  @Column(name = "FieldOldValue", nullable = true)
  private String fieldOldValue;

  @Column(name = "FieldNewValue", nullable = true)
  private String fieldNewValue;

  public ApolloAuditLogDataInfluence() {
  }

  public ApolloAuditLogDataInfluence(String spanId, String entityName, long entityId,
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

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
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

  public long getInfluenceEntityId() {
    return influenceEntityId;
  }

  public void setInfluenceEntityId(long influenceEntityId) {
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

    public Builder entityId(long val) {
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
