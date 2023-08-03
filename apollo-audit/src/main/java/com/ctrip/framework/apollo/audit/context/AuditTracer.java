package com.ctrip.framework.apollo.audit.context;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.context.AuditScopeManager;
import com.ctrip.framework.apollo.audit.context.AuditSpan;
import com.ctrip.framework.apollo.audit.context.AuditSpanContext;
import com.ctrip.framework.apollo.audit.util.IdGenerator;
import com.ctrip.framework.apollo.audit.util.OperationUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

public class AuditTracer {

  private final AuditScopeManager manager;

  public AuditTracer(AuditScopeManager manager) {
    this.manager = manager;
  }

  public AuditScopeManager scopeManager(){
    return manager;
  }

  public String extract(){

    return String.format("%s,%s,%s",
        manager.activeSpanContext().getTraceId(),
        manager.activeSpanContext().getSpanId(),
        manager.activeSpanContext().getOperator());
  }

  public AuditSpanContext inject(String val){
    if(val == null || val.equals("")){
      return null;
    }
    String[] arr = val.split(",",3);
    AuditSpanContext context = new AuditSpanContext(arr[0], arr[1]);
    context.setOperator(arr[2]);
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

    public AuditSpanBuilder asChildOf(AuditSpanContext parentContext){
      if(parentContext == null){
        // initialize as a root span
        traceId = IdGenerator.generate();
        parentId = null;
        operator = OperationUtil.getOperatorByFramework();
        return this;
      }
      traceId = parentContext.getTraceId();
      operator = parentContext.getOperator();
      parentId = parentContext.getSpanId();
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

    public AuditSpanBuilder context(AuditSpanContext val){
      this.traceId = val.getTraceId();
      this.operator = val.getOperator();
      return this;
    }

    public AuditSpan build(){
      spanId = IdGenerator.generate();
      AuditSpanContext context = new AuditSpanContext(traceId,spanId);
      context.setOperator(operator);
      AuditSpan span = new AuditSpan();
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
