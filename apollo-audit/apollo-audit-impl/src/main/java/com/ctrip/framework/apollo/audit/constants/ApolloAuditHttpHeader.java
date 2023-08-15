package com.ctrip.framework.apollo.audit.constants;

/**
 * http header constants
 * @author luke (lukewei0125@foxmail.com)
 */
public interface ApolloAuditHttpHeader {
  public final String TRACE_ID = "Apollo-Audit-TraceId";
  public final String SPAN_ID = "Apollo-Audit-SpanId";
  public final String OPERATOR = "Apollo-Audit-Operator";
}
