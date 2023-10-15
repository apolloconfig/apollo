package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import javax.persistence.Id;

@ApolloAuditLogDataInfluenceTable(tableName = "MockTableName")
public class MockDataInfluenceEntity {

  @Id
  private long id;

  @ApolloAuditLogDataInfluenceTableField(fieldName = "MarkedAttribute")
  private String markedAttribute;
  private String unMarkedAttribute;
  private boolean isDeleted;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getMarkedAttribute() {
    return markedAttribute;
  }

  public void setMarkedAttribute(String markedAttribute) {
    this.markedAttribute = markedAttribute;
  }

  public String getUnMarkedAttribute() {
    return unMarkedAttribute;
  }

  public void setUnMarkedAttribute(String unMarkedAttribute) {
    this.unMarkedAttribute = unMarkedAttribute;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }
}
