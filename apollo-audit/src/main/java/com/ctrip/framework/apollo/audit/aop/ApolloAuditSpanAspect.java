package com.ctrip.framework.apollo.audit.aop;

import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


@Aspect
public class ApolloAuditSpanAspect {

  private final ApolloAuditTracer tracer;
  private final ApolloAuditLogService logService;
  private final ApolloAuditSpanService spanService;

  public ApolloAuditSpanAspect(ApolloAuditTracer tracer, ApolloAuditLogService logService,
      ApolloAuditSpanService spanService) {
    this.tracer = tracer;
    this.logService = logService;
    this.spanService = spanService;
  }

  @Pointcut("@annotation(auditEvent)")
  public void setAuditSpan(ApolloAuditLog auditEvent){}

  @Around(value = "setAuditSpan(auditLog)")
  public Object around(ProceedingJoinPoint pjp, ApolloAuditLog auditLog) throws Throwable {

    ApolloAuditSpanContext parentSpanContext = getParentContext();
    ApolloAuditSpan span = generateSpan(parentSpanContext, auditLog);

    Object returnVal = null;
    try (ApolloAuditScope scope = tracer.scopeManager().activate(span.getSpanContext())) {
      span.log();
      logService.logSpan(span);

      returnVal = pjp.proceed();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return returnVal;
  }

  String getFollowSFromId(){
    if(tracer.scopeManager().getScope() == null){
      return null;
    }
    if(tracer.scopeManager().getScope().getLastSpanContext() == null){
      return null;
    }
    return tracer.scopeManager().getScope().getLastSpanContext().getSpanId();
  }

  ApolloAuditSpanContext getParentContext() {
    ApolloAuditSpanContext parentContext = tracer.scopeManager().activeSpanContext();
    if (parentContext == null) {
      parentContext = spanService.tryToGetParentSpanContext();
    }
    return parentContext;
  }

  ApolloAuditSpan generateSpan(ApolloAuditSpanContext parentSpanContext, ApolloAuditLog auditLog) {
    if(parentSpanContext == null){
      return tracer.buildSpan(auditLog.type(), auditLog.name())
          .asRootSpan(spanService.getOperator())
          .description(auditLog.description())
          .followsFrom(getFollowSFromId())
          .build();
    } else {
      return tracer.buildSpan(auditLog.type(), auditLog.name())
          .asChildOf(parentSpanContext)
          .description(auditLog.description())
          .followsFrom(getFollowSFromId())
          .build();
    }
  }

}
