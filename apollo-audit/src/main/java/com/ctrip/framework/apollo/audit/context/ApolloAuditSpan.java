package com.ctrip.framework.apollo.audit.context;

import com.ctrip.framework.apollo.audit.annotation.OpType;

public class ApolloAuditSpan {

  private String parentId;
  private String followsFromId;

  private OpType opType;
  private String opName;
  private String description;

  private ApolloAuditSpanContext spanContext;

  public ApolloAuditSpanContext context(){
    return this.spanContext;
  }

  //just do nothing
  public void finish(){}

  public void log(){
    //TODO
    System.out.println("LOG!!!\n"+this+"\n");
  }

  @Override
  public String toString() {
    return "AuditSpan{" +
        "parentId='" + parentId + '\'' +
        ", followsFromId='" + followsFromId + '\'' +
        ", opType=" + opType +
        ", opName='" + opName + '\'' +
        ", description='" + description + '\'' +
        ", spanContext=" + spanContext +
        '}';
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getFollowsFromId() {
    return followsFromId;
  }

  public void setFollowsFromId(String followsFromId) {
    this.followsFromId = followsFromId;
  }

  public OpType getOpType() {
    return opType;
  }

  public void setOpType(OpType opType) {
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

  public ApolloAuditSpanContext getSpanContext() {
    return spanContext;
  }

  public void setSpanContext(ApolloAuditSpanContext spanContext) {
    this.spanContext = spanContext;
  }
}