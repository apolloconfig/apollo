package com.ctrip.framework.apollo.audit.api;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ApolloAuditEntityWrapper {

  private String entityName;

  private List<Field> dataInfluenceFields;

  private Field entityIdField;

  public ApolloAuditEntityWrapper() {
    this.dataInfluenceFields = new ArrayList<>();
  }

  public ApolloAuditEntityWrapper entityName(String val) {
    this.entityName = val;
    return this;
  }

  public ApolloAuditEntityWrapper entityIdField(Field f) {
    this.entityIdField = f;
    return this;
  }

  public ApolloAuditEntityWrapper dataInfluenceFields(List<Field> val) {
    this.dataInfluenceFields = val;
    return this;
  }

  public ApolloAuditEntityWrapper addDataInfluenceFields(Field f) {
    this.dataInfluenceFields.add(f);
    return this;
  }

  public String getEntityName() {
    return entityName;
  }

  public Field getEntityIdField() {
    return entityIdField;
  }

  public List<Field> getDataInfluenceFields() {
    return dataInfluenceFields;
  }
}
