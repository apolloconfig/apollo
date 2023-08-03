package com.ctrip.framework.apollo.audit.context;

public class AuditSpanContext {

  private String traceId;

  private String spanId;

  private String operator;

  public AuditSpanContext(String traceId, String spanId){
    this.traceId = traceId;
    this.spanId = spanId;
  }

  @Override
  public String toString() {
    return "AuditSpanContext{" +
        "traceId='" + traceId + '\'' +
        ", spanId='" + spanId + '\'' +
        ", operator='" + operator + '\'' +
        '}';
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
