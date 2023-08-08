package com.ctrip.framework.apollo.audit.context;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditHttpHeader;
import com.ctrip.framework.apollo.audit.util.IdGenerator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

public class ApolloAuditTracer {

  private final ApolloAuditScopeManager manager;

  public ApolloAuditTracer(ApolloAuditScopeManager manager) {
    this.manager = manager;
  }

  public ApolloAuditScopeManager scopeManager(){
    return manager;
  }

  public Map<String, List<String>> extract(){
    Map<String, List<String>> map = new HashMap<>();
    map.put(ApolloAuditHttpHeader.TRACE_ID,
        Collections.singletonList(manager.activeSpanContext().getTraceId()));
    map.put(ApolloAuditHttpHeader.SPAN_ID,
        Collections.singletonList(manager.activeSpanContext().getSpanId()));
    map.put(ApolloAuditHttpHeader.OPERATOR,
        Collections.singletonList(manager.activeSpanContext().getOperator()));
    return map;
  }

  public ApolloAuditSpanContext inject(Map<String, String> map){
    if(map.isEmpty()){
      return null;
    }
    ApolloAuditSpanContext context = new ApolloAuditSpanContext(map.get(ApolloAuditHttpHeader.TRACE_ID),
        ApolloAuditHttpHeader.SPAN_ID);
    context.setOperator(ApolloAuditHttpHeader.OPERATOR);
    return context;
  }

  public AuditSpanBuilder buildSpan(OpType type, String name){
    return new AuditSpanBuilder(type, name);
  }

  public static class AuditSpanBuilder{
    private String traceId;
    private String spanId;
    private String operator;

    private String parentId;
    private String followsFromId;
    private OpType opType;
    private String opName;
    private String description;

    public AuditSpanBuilder(OpType type, String name){
      opType = type;
      opName = name;
    }

    public AuditSpanBuilder asChildOf(ApolloAuditSpanContext parentContext){
      traceId = parentContext.getTraceId();
      operator = parentContext.getOperator();
      parentId = parentContext.getSpanId();
      return this;
    }

    public AuditSpanBuilder asRootSpan(String operator) {
      traceId = IdGenerator.generate();
      this.operator = operator;
      return this;
    }

    public AuditSpanBuilder followsFrom(String id){
      this.followsFromId = id;
      return this;
    }

    public AuditSpanBuilder description(String val){
      this.description = val;
      return this;
    }

    public AuditSpanBuilder context(ApolloAuditSpanContext val){
      this.traceId = val.getTraceId();
      this.operator = val.getOperator();
      return this;
    }

    public ApolloAuditSpan build(){
      spanId = IdGenerator.generate();
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId,spanId);
      context.setOperator(operator);

      ApolloAuditSpan span = new ApolloAuditSpan();
      span.setSpanContext(context);
      span.setDescription(description);
      span.setFollowsFromId(followsFromId);
      span.setParentId(parentId);
      span.setOpName(opName);
      span.setOpType(opType);
      return span;
    }
  }
}
