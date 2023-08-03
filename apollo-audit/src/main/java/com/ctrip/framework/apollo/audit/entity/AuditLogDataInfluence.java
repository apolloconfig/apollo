package com.ctrip.framework.apollo.audit.entity;

import com.ctrip.framework.apollo.common.entity.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "`AuditLogDataInfluence`")
@SQLDelete(sql = "Update AuditLogDataInfluence set IsDeleted = true, DeletedAt = ROUND(UNIX_TIMESTAMP(NOW(4))*1000) where Id = ?")
@Where(clause = "`IsDeleted` = false")
public class AuditLogDataInfluence extends BaseEntity {

  @Column(name = "SpanId", nullable = false)
  private String spanId;

  @Column(name = "InfluenceEntityName", nullable = false)
  private String influenceEntityName;

  @Column(name = "InfluenceEntityId", nullable = false)
  private String influenceEntityId;

  @Column(name = "FieldName", nullable = false)
  private String fieldName;

  @Column(name = "FieldOldValue", nullable = true)
  private String fieldOldValue;

  @Column(name = "FieldNewValue", nullable = true)
  private String fieldNewValue;

  @Override
  public String toString(){
    return toStringHelper().add("InfluenceEntityName", influenceEntityName)
        .add("InfluenceEntityId", influenceEntityId).add("FieldName", fieldName)
        .add("FieldOldValue", fieldOldValue).add("FieldNewValue", fieldNewValue).toString();
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
}
