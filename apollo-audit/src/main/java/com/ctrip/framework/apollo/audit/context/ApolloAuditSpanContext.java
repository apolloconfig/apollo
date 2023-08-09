package com.ctrip.framework.apollo.audit.context;

public class ApolloAuditSpanContext {

  private String traceId;

  private String spanId;

  private String operator;

  public ApolloAuditSpanContext(String traceId, String spanId){
    this.traceId = traceId;
    this.spanId = spanId;
  }

  public ApolloAuditSpanContext(String traceId, String spanId, String operator){
    this.traceId = traceId;
    this.spanId = spanId;
    this.operator = operator;
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

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

}
